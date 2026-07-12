-- Realtime order status data warehouse layers.
--
-- PostgreSQL is only the OLTP source. Do not create DWD, DWS, or ADS tables there.
-- Debezium Kafka Connect captures PostgreSQL WAL and writes raw CDC events to Kafka ODS.
-- Kafka is the realtime ODS layer.
-- ClickHouse is the warehouse query layer for DWD/DWS/ADS.
--
-- ClickHouse init SQL creates real physical warehouse tables.
-- Flink CREATE TABLE creates connector mappings used by Flink at runtime.

CREATE TABLE ods_order_status_change_raw (
  `before` ROW<
    id BIGINT,
    created_at STRING,
    updated_at STRING,
    order_no STRING,
    type STRING,
    status STRING
  >,
  `after` ROW<
    id BIGINT,
    created_at STRING,
    updated_at STRING,
    order_no STRING,
    type STRING,
    status STRING
  >,
  `source` ROW<
    db STRING,
    schema STRING,
    `table` STRING
  >,
  op STRING,
  ts_ms BIGINT
) WITH (
  'connector' = 'kafka',
  'topic' = 'ods_order_status_change_raw',
  'properties.bootstrap.servers' = 'kafka:9092',
  'properties.group.id' = 'wms-dwd-order-status-change',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json',
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

-- Debezium Kafka ODS to Kafka DWD. ClickHouse consumes this DWD topic through
-- a Kafka Engine table and a materialized view.
INSERT INTO dwd_order_status_change_kafka_sink
SELECT
  CASE WHEN `source`.`table` = 'inbound_orders'
       THEN `after`.id * 2
       ELSE `after`.id * 2 + 1
  END,
  TRIM(`after`.order_no),
  CONCAT(
    CASE WHEN `source`.`table` = 'inbound_orders' THEN 'INBOUND_' ELSE 'OUTBOUND_' END,
    TRIM(`after`.type)
  ),
  CASE WHEN `before` IS NULL THEN CAST(NULL AS STRING) ELSE UPPER(TRIM(`before`.status)) END,
  UPPER(TRIM(`after`.status)),
  CASE WHEN UPPER(TRIM(`after`.status)) = 'Q'
         OR UPPER(TRIM(`after`.status)) IN ('IN_QUEUE', 'CREATED')
       THEN 1 ELSE 0 END,
  CAST(TO_TIMESTAMP_LTZ(ts_ms, 3) AS TIMESTAMP(3)),
  CAST(TO_TIMESTAMP_LTZ(ts_ms, 3) AS TIMESTAMP(3)),
  op,
  CURRENT_TIMESTAMP
FROM ods_order_status_change_raw
WHERE `after` IS NOT NULL
  AND `after`.id IS NOT NULL
  AND `after`.order_no IS NOT NULL
  AND `after`.status IS NOT NULL
  AND TRIM(`after`.order_no) <> ''
  AND TRIM(`after`.status) <> ''
  AND `source`.`table` IN ('inbound_orders', 'outbound_orders');
