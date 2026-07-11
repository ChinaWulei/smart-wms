-- Realtime order status data warehouse layers.
--
-- PostgreSQL is only the OLTP source. Do not create DWD/DWS/ADS tables there.
-- Kafka is the realtime ODS layer.
-- ClickHouse is the warehouse query layer for DWD/DWS/ADS.
--
-- ClickHouse init SQL creates real physical warehouse tables.
-- Flink CREATE TABLE creates connector mappings used by Flink at runtime.

CREATE TABLE cdc_outbound_orders (
  id BIGINT,
  created_at TIMESTAMP(3),
  updated_at TIMESTAMP(3),
  order_no STRING,
  type STRING,
  status STRING,
  PRIMARY KEY (id) NOT ENFORCED
) WITH (
  'connector' = 'postgres-cdc',
  'hostname' = 'postgres',
  'port' = '5432',
  'username' = 'postgres',
  'password' = 'postgres',
  'database-name' = 'smart_wms',
  'schema-name' = 'public',
  'table-name' = 'outbound_orders',
  'slot.name' = 'flink_outbound_orders_slot',
  'decoding.plugin.name' = 'pgoutput'
);

CREATE TABLE ods_order_status_change_raw_sink (
  orderId BIGINT,
  orderNo STRING,
  orderType STRING,
  beforeStatus STRING,
  afterStatus STRING,
  changeTime TIMESTAMP(3),
  eventTime TIMESTAMP(3),
  op STRING,
  PRIMARY KEY (orderId) NOT ENFORCED
) WITH (
  'connector' = 'upsert-kafka',
  'topic' = 'ods_order_status_change_raw',
  'properties.bootstrap.servers' = 'kafka:9092',
  'key.format' = 'json',
  'value.format' = 'json',
  'value.json.timestamp-format.standard' = 'ISO-8601'
);

CREATE TABLE ods_order_status_change_raw (
  orderId BIGINT,
  orderNo STRING,
  orderType STRING,
  beforeStatus STRING,
  afterStatus STRING,
  changeTime TIMESTAMP(3),
  eventTime TIMESTAMP(3),
  op STRING,
  WATERMARK FOR eventTime AS eventTime - INTERVAL '5' SECOND
) WITH (
  'connector' = 'kafka',
  'topic' = 'ods_order_status_change_raw',
  'properties.bootstrap.servers' = 'kafka:9092',
  'properties.group.id' = 'wms-dwd-order-status-change',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json',
  'json.timestamp-format.standard' = 'ISO-8601',
  'json.ignore-parse-errors' = 'true'
);

CREATE TABLE dwd_order_status_change_kafka_sink (
  order_id BIGINT,
  order_no STRING,
  order_type STRING,
  before_status STRING,
  after_status STRING,
  is_q_status INT,
  change_time TIMESTAMP(3),
  event_time TIMESTAMP(3),
  op STRING,
  update_time TIMESTAMP(3)
) WITH (
  'connector' = 'kafka',
  'topic' = 'dwd_order_status_change',
  'properties.bootstrap.servers' = 'kafka:9092',
  'format' = 'json',
  'json.timestamp-format.standard' = 'ISO-8601'
);

-- CDC to Kafka ODS. For a full beforeStatus value, production Debezium should
-- emit both before/after payloads. This mapping keeps the raw event contract and
-- leaves beforeStatus null when Flink CDC SQL cannot expose it directly.
INSERT INTO ods_order_status_change_raw_sink
SELECT
  id,
  order_no,
  type,
  CAST(NULL AS STRING),
  status,
  COALESCE(updated_at, created_at),
  CURRENT_TIMESTAMP,
  'u'
FROM cdc_outbound_orders
WHERE id IS NOT NULL
  AND order_no IS NOT NULL
  AND status IS NOT NULL;

-- Kafka ODS to Kafka DWD. ClickHouse consumes this topic through
-- a Kafka Engine table and a materialized view.
INSERT INTO dwd_order_status_change_kafka_sink
SELECT
  orderId,
  TRIM(orderNo),
  TRIM(orderType),
  UPPER(TRIM(beforeStatus)),
  UPPER(TRIM(afterStatus)),
  CASE WHEN UPPER(TRIM(afterStatus)) = 'Q'
         OR UPPER(TRIM(afterStatus)) IN ('IN_QUEUE', 'CREATED')
       THEN 1 ELSE 0 END,
  changeTime,
  eventTime,
  op,
  CURRENT_TIMESTAMP
FROM ods_order_status_change_raw
WHERE orderId IS NOT NULL
  AND orderNo IS NOT NULL
  AND afterStatus IS NOT NULL
  AND TRIM(orderNo) <> ''
  AND TRIM(afterStatus) <> '';
