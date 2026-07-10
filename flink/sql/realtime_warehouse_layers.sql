-- Realtime warehouse data layers for the Q-status order panel.
-- The Flink cluster is provided by docker-compose.yml. Add the required JDBC/CDC
-- connector jars to the Flink image or /opt/flink/lib before submitting this job.

CREATE TABLE ods_outbound_orders (
  id BIGINT,
  created_at TIMESTAMP(3),
  order_no STRING,
  status STRING,
  WATERMARK FOR created_at AS created_at - INTERVAL '5' SECOND
) WITH (
  'connector' = 'jdbc',
  'url' = 'jdbc:postgresql://postgres:5432/smart_wms',
  'table-name' = 'outbound_orders',
  'username' = 'postgres',
  'password' = 'postgres'
);

CREATE TABLE ods_outbound_order_items (
  order_id BIGINT,
  warehouse_id BIGINT
) WITH (
  'connector' = 'jdbc',
  'url' = 'jdbc:postgresql://postgres:5432/smart_wms',
  'table-name' = 'outbound_order_items',
  'username' = 'postgres',
  'password' = 'postgres'
);

CREATE TABLE dwd_order_status_detail (
  event_time TIMESTAMP(3),
  warehouse_id BIGINT,
  order_no STRING,
  status_code STRING
) WITH (
  'connector' = 'jdbc',
  'url' = 'jdbc:clickhouse://clickhouse:8123/smart_wms_dw',
  'table-name' = 'dwd_order_status_detail',
  'username' = 'default',
  'password' = ''
);

CREATE TABLE dws_q_order_minute (
  minute TIMESTAMP(3),
  warehouse_id BIGINT,
  status_code STRING,
  order_count BIGINT
) WITH (
  'connector' = 'jdbc',
  'url' = 'jdbc:clickhouse://clickhouse:8123/smart_wms_dw',
  'table-name' = 'dws_q_order_minute',
  'username' = 'default',
  'password' = ''
);

CREATE TABLE ads_q_order_10m (
  minute TIMESTAMP(3),
  warehouse_id BIGINT,
  status_code STRING,
  order_no STRING
) WITH (
  'connector' = 'jdbc',
  'url' = 'jdbc:clickhouse://clickhouse:8123/smart_wms_dw',
  'table-name' = 'ads_q_order_10m',
  'username' = 'default',
  'password' = ''
);

INSERT INTO dwd_order_status_detail
SELECT DISTINCT o.created_at, i.warehouse_id, o.order_no, 'Q'
FROM ods_outbound_orders o
JOIN ods_outbound_order_items i ON o.id = i.order_id
WHERE o.status IN ('Q', 'IN_QUEUE', 'CREATED');

INSERT INTO dws_q_order_minute
SELECT TUMBLE_START(o.created_at, INTERVAL '1' MINUTE), i.warehouse_id, 'Q', COUNT(DISTINCT o.order_no)
FROM ods_outbound_orders o
JOIN ods_outbound_order_items i ON o.id = i.order_id
WHERE o.status IN ('Q', 'IN_QUEUE', 'CREATED')
GROUP BY TUMBLE(o.created_at, INTERVAL '1' MINUTE), i.warehouse_id;

INSERT INTO ads_q_order_10m
SELECT DISTINCT TUMBLE_START(o.created_at, INTERVAL '1' MINUTE), i.warehouse_id, 'Q', o.order_no
FROM ods_outbound_orders o
JOIN ods_outbound_order_items i ON o.id = i.order_id
WHERE o.status IN ('Q', 'IN_QUEUE', 'CREATED');
