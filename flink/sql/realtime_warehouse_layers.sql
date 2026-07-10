-- Realtime warehouse data layers for the Q-status order panel.
--
-- clickhouse/init/001_realtime_warehouse.sql creates real physical tables in ClickHouse.
-- The CREATE TABLE statements below create Flink table mappings. They tell Flink
-- how to read PostgreSQL CDC tables and how to write ClickHouse tables; they do
-- not create another copy of those database tables inside Flink.
--
-- Required connector jars before submitting this job:
--   - Flink PostgreSQL CDC connector
--   - Flink JDBC connector
--   - PostgreSQL JDBC driver
--   - ClickHouse JDBC driver

CREATE TABLE ods_outbound_orders (
  id BIGINT,
  created_at TIMESTAMP(3),
  order_no STRING,
  status STRING,
  PRIMARY KEY (id) NOT ENFORCED,
  WATERMARK FOR created_at AS created_at - INTERVAL '5' SECOND
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

CREATE TABLE ods_outbound_order_items (
  id BIGINT,
  order_id BIGINT,
  warehouse_id BIGINT,
  PRIMARY KEY (id) NOT ENFORCED
) WITH (
  'connector' = 'postgres-cdc',
  'hostname' = 'postgres',
  'port' = '5432',
  'username' = 'postgres',
  'password' = 'postgres',
  'database-name' = 'smart_wms',
  'schema-name' = 'public',
  'table-name' = 'outbound_order_items',
  'slot.name' = 'flink_outbound_order_items_slot',
  'decoding.plugin.name' = 'pgoutput'
);

CREATE TABLE realtime_ticks (
  tick_id BIGINT,
  proc_time AS PROCTIME()
) WITH (
  'connector' = 'datagen',
  'rows-per-second' = '1',
  'fields.tick_id.kind' = 'sequence',
  'fields.tick_id.start' = '1',
  'fields.tick_id.end' = '9223372036854775807'
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
  created_at TIMESTAMP(3),
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
SELECT DISTINCT o.created_at, i.warehouse_id, 'Q', o.order_no
FROM ods_outbound_orders o
JOIN ods_outbound_order_items i ON o.id = i.order_id
JOIN realtime_ticks t ON MOD(t.tick_id, 60) = 0
WHERE o.status IN ('Q', 'IN_QUEUE', 'CREATED')
  AND o.created_at <= t.proc_time - INTERVAL '10' MINUTE;
