CREATE DATABASE IF NOT EXISTS smart_wms_dw;

CREATE USER IF NOT EXISTS wms_dw IDENTIFIED WITH no_password;
GRANT SELECT, INSERT ON smart_wms_dw.* TO wms_dw;

CREATE TABLE IF NOT EXISTS smart_wms_dw.dwd_order_status_change
(
    order_id UInt64,
    order_no String,
    order_type LowCardinality(String),
    before_status Nullable(String),
    after_status String,
    is_q_status UInt8,
    change_time DateTime,
    event_time DateTime,
    op LowCardinality(String),
    update_time DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(update_time)
ORDER BY (order_id, event_time, after_status);

CREATE TABLE IF NOT EXISTS smart_wms_dw.dwd_order_status_change_kafka
(
    order_id UInt64,
    order_no String,
    order_type String,
    before_status Nullable(String),
    after_status String,
    is_q_status UInt8,
    change_time String,
    event_time String,
    op String,
    update_time String
)
ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka:9092',
    kafka_topic_list = 'dwd_order_status_change',
    kafka_group_name = 'clickhouse_dwd_order_status_change',
    kafka_format = 'JSONEachRow',
    kafka_num_consumers = 1;

CREATE MATERIALIZED VIEW IF NOT EXISTS smart_wms_dw.mv_dwd_order_status_change
TO smart_wms_dw.dwd_order_status_change
AS
SELECT
    order_id,
    order_no,
    order_type,
    before_status,
    after_status,
    is_q_status,
    parseDateTimeBestEffortOrZero(change_time),
    parseDateTimeBestEffortOrZero(event_time),
    op,
    parseDateTimeBestEffortOrZero(update_time)
FROM smart_wms_dw.dwd_order_status_change_kafka;

CREATE TABLE IF NOT EXISTS smart_wms_dw.dws_order_q_10m_timeout_detail
(
    order_id UInt64,
    order_no String,
    order_type LowCardinality(String),
    q_start_time DateTime,
    timeout_time DateTime,
    current_status LowCardinality(String),
    is_timeout UInt8,
    update_time DateTime
)
ENGINE = ReplacingMergeTree(update_time)
ORDER BY order_id;

CREATE TABLE IF NOT EXISTS smart_wms_dw.ads_order_q_10m_timeout_summary
(
    metric_code LowCardinality(String),
    metric_name String,
    metric_value UInt64,
    update_time DateTime
)
ENGINE = ReplacingMergeTree(update_time)
ORDER BY metric_code;

CREATE TABLE IF NOT EXISTS smart_wms_dw.ads_order_status_count
(
    direction LowCardinality(String),
    status LowCardinality(String),
    order_count UInt64,
    update_time DateTime
)
ENGINE = ReplacingMergeTree(update_time)
ORDER BY (direction, status);

CREATE TABLE IF NOT EXISTS smart_wms_dw.ads_order_creation_7d_trend
(
    direction LowCardinality(String),
    metric_date Date,
    order_count UInt64,
    update_time DateTime
)
ENGINE = ReplacingMergeTree(update_time)
ORDER BY (direction, metric_date);
