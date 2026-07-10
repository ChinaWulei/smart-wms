CREATE DATABASE IF NOT EXISTS smart_wms_dw;

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
