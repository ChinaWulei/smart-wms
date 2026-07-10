CREATE DATABASE IF NOT EXISTS smart_wms_dw;

CREATE TABLE IF NOT EXISTS smart_wms_dw.ods_order_events
(
    event_time DateTime,
    warehouse_id UInt64,
    order_no String,
    status_code LowCardinality(String),
    source_table LowCardinality(String)
)
ENGINE = MergeTree
ORDER BY (event_time, warehouse_id, order_no);

CREATE TABLE IF NOT EXISTS smart_wms_dw.dwd_order_status_detail
(
    event_time DateTime,
    warehouse_id UInt64,
    order_no String,
    status_code LowCardinality(String)
)
ENGINE = ReplacingMergeTree
ORDER BY (warehouse_id, order_no, event_time);

CREATE TABLE IF NOT EXISTS smart_wms_dw.dws_q_order_minute
(
    minute DateTime,
    warehouse_id UInt64,
    status_code LowCardinality(String),
    order_count UInt32
)
ENGINE = SummingMergeTree
ORDER BY (warehouse_id, status_code, minute);

CREATE TABLE IF NOT EXISTS smart_wms_dw.ads_q_order_10m
(
    minute DateTime,
    warehouse_id UInt64,
    status_code LowCardinality(String),
    order_no String
)
ENGINE = ReplacingMergeTree
ORDER BY (warehouse_id, status_code, minute, order_no);
