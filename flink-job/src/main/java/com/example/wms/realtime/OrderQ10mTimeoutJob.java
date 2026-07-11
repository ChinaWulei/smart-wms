package com.example.wms.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class OrderQ10mTimeoutJob {
    private static final long TEN_MINUTES_MS = 10 * 60 * 1000L;
    private static final String METRIC_CODE = "CONTINUOUS_Q_ORDER_10M";
    private static final String METRIC_NAME = "\u8fde\u7eed10\u5206\u949f\u72b6\u6001\u4e3aQ\u7684\u8ba2\u5355\u6570\u91cf";

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(env("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"))
                .setTopics(env("DWD_TOPIC", "dwd_order_status_change"))
                .setGroupId(env("FLINK_CONSUMER_GROUP", "wms-dws-q-10m-timeout"))
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<OrderStatusChange> changes = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "dwd-order-status-change")
                .map(OrderStatusChange::fromJson)
                .filter(change -> change.orderId != null && change.orderNo != null && change.afterStatus != null);

        changes.keyBy(change -> change.orderId)
                .process(new QTimeoutProcessFunction())
                .name("dws-ads-q-10m-timeout");

        env.execute("wms-order-q-10m-timeout");
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static class QTimeoutProcessFunction extends KeyedProcessFunction<Long, OrderStatusChange, Void> {
        private transient ValueState<OrderRuntimeState> state;
        private transient ClickHouseWriter writer;

        @Override
        public void open(Configuration parameters) throws Exception {
            state = getRuntimeContext().getState(new ValueStateDescriptor<>("order-q-runtime", OrderRuntimeState.class));
            writer = new ClickHouseWriter();
        }

        @Override
        public void close() throws Exception {
            if (writer != null) {
                writer.close();
            }
        }

        @Override
        public void processElement(OrderStatusChange change, Context context, Collector<Void> out) throws Exception {
            OrderRuntimeState current = state.value();
            boolean isQ = isQStatus(change.afterStatus);
            long now = System.currentTimeMillis();

            if (isQ) {
                long qStart = current != null && current.qStartTime > 0
                        ? current.qStartTime
                        : change.eventTimeMillis(now);
                boolean emitted = current != null && current.timeoutEmitted;

                state.update(new OrderRuntimeState(change.orderNo, change.orderType, change.afterStatus, qStart, emitted));
                context.timerService().registerProcessingTimeTimer(qStart + TEN_MINUTES_MS);
                return;
            }

            if (current != null && current.timeoutEmitted) {
                writer.upsertDws(change.orderId, change.orderNo, change.orderType, current.qStartTime,
                        now, change.afterStatus, 0);
                writer.upsertAdsSummary();
            }
            state.clear();
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext context, Collector<Void> out) throws Exception {
            OrderRuntimeState current = state.value();
            if (current == null || current.timeoutEmitted || !isQStatus(current.currentStatus)) {
                return;
            }

            writer.upsertDws(context.getCurrentKey(), current.orderNo, current.orderType, current.qStartTime,
                    timestamp, current.currentStatus, 1);
            writer.upsertAdsSummary();

            current.timeoutEmitted = true;
            state.update(current);
        }

        private boolean isQStatus(String status) {
            String normalized = status == null ? "" : status.trim().toUpperCase();
            return normalized.equals("Q") || normalized.equals("IN_QUEUE") || normalized.equals("CREATED");
        }
    }

    public static class OrderStatusChange {
        public Long orderId;
        public String orderNo;
        public String orderType;
        public String afterStatus;
        public String eventTime;
        private static final ObjectMapper MAPPER = new ObjectMapper();

        static OrderStatusChange fromJson(String json) throws Exception {
            JsonNode node = MAPPER.readTree(json);
            OrderStatusChange change = new OrderStatusChange();
            change.orderId = longValue(node, "order_id", "orderId");
            change.orderNo = text(node, "order_no", "orderNo");
            change.orderType = text(node, "order_type", "orderType");
            change.afterStatus = text(node, "after_status", "afterStatus");
            change.eventTime = text(node, "event_time", "eventTime");
            return change;
        }

        long eventTimeMillis(long fallback) {
            if (eventTime == null || eventTime.isBlank()) {
                return fallback;
            }

            try {
                return Instant.parse(eventTime).toEpochMilli();
            } catch (RuntimeException ignored) {
                return parseLocalDateTime(fallback);
            }
        }

        private long parseLocalDateTime(long fallback) {
            try {
                String normalized = eventTime.trim().replace(' ', 'T');
                return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private static Long longValue(JsonNode node, String snake, String camel) {
            JsonNode value = node.has(snake) ? node.path(snake) : node.path(camel);
            return value.isMissingNode() || value.isNull() ? null : value.asLong();
        }

        private static String text(JsonNode node, String snake, String camel) {
            JsonNode value = node.has(snake) ? node.path(snake) : node.path(camel);
            return value.isMissingNode() || value.isNull() ? null : value.asText();
        }
    }

    public static class OrderRuntimeState {
        public String orderNo;
        public String orderType;
        public String currentStatus;
        public long qStartTime;
        public boolean timeoutEmitted;

        public OrderRuntimeState() {
        }

        public OrderRuntimeState(String orderNo, String orderType, String currentStatus, long qStartTime, boolean timeoutEmitted) {
            this.orderNo = orderNo;
            this.orderType = orderType;
            this.currentStatus = currentStatus;
            this.qStartTime = qStartTime;
            this.timeoutEmitted = timeoutEmitted;
        }
    }

    private static class ClickHouseWriter implements AutoCloseable {
        private final Connection connection;
        private final PreparedStatement dws;
        private final PreparedStatement ads;

        ClickHouseWriter() throws Exception {
            Class.forName("com.clickhouse.jdbc.ClickHouseDriver");
            connection = DriverManager.getConnection(
                    env("CLICKHOUSE_JDBC_URL", "jdbc:clickhouse://clickhouse:8123/smart_wms_dw"),
                    env("CLICKHOUSE_USER", "default"),
                    env("CLICKHOUSE_PASSWORD", ""));
            dws = connection.prepareStatement("""
                    insert into dws_order_q_10m_timeout_detail
                    (order_id, order_no, order_type, q_start_time, timeout_time, current_status, is_timeout, update_time)
                    values (?, ?, ?, ?, ?, ?, ?, now())
                    """);
            ads = connection.prepareStatement("""
                    insert into ads_order_q_10m_timeout_summary
                    (metric_code, metric_name, metric_value, update_time)
                    select ?, ?, count(), now()
                    from dws_order_q_10m_timeout_detail final
                    where is_timeout = 1
                    """);
        }

        void upsertDws(Long orderId, String orderNo, String orderType, long qStartTime, long timeoutTime,
                       String currentStatus, int isTimeout) throws Exception {
            dws.setLong(1, orderId);
            dws.setString(2, orderNo);
            dws.setString(3, orderType == null ? "" : orderType);
            dws.setString(4, formatTime(qStartTime));
            dws.setString(5, formatTime(timeoutTime));
            dws.setString(6, currentStatus);
            dws.setInt(7, isTimeout);
            dws.executeUpdate();
        }

        void upsertAdsSummary() throws Exception {
            ads.setString(1, METRIC_CODE);
            ads.setString(2, METRIC_NAME);
            ads.executeUpdate();
        }

        private String formatTime(long epochMillis) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                    .toString()
                    .replace('T', ' ');
        }

        @Override
        public void close() throws Exception {
            dws.close();
            ads.close();
            connection.close();
        }
    }
}
