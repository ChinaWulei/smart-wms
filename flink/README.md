# Smart WMS Realtime Warehouse Jobs

## Layer Ownership

- PostgreSQL `smart_wms`: OLTP source only. Do not create DWD, DWS, or ADS tables here.
- Kafka `ods_order_status_change_raw`: realtime ODS topic for raw order status change events.
- ClickHouse `smart_wms_dw`: warehouse tables for DWD, DWS, and ADS.

## Why ClickHouse and Flink Both Have `CREATE TABLE`

`clickhouse/init/*.sql` creates physical tables in ClickHouse.

`flink/sql/*.sql` creates Flink connector mappings. Those mappings tell Flink how to read PostgreSQL CDC/Kafka and write Kafka/ClickHouse. They are not another set of physical database tables.

## Jobs

1. `sql/realtime_warehouse_layers.sql`
   - PostgreSQL CDC to Kafka ODS topic `ods_order_status_change_raw`
   - Kafka ODS topic to ClickHouse DWD table `dwd_order_status_change`
   - Kafka ODS topic to Kafka DWD topic `dwd_order_status_change`

2. `datastream/OrderQ10mTimeoutJob.java`
   - Consumes Kafka DWD topic `dwd_order_status_change`
   - Uses keyed state per order
   - Registers a 10 minute timer when an order enters Q
   - Writes ClickHouse DWS table `dws_order_q_10m_timeout_detail`
   - Updates ClickHouse ADS table `ads_order_q_10m_timeout_summary`

## Build Flink Image

Download connector jars before building the Flink image:

```bash
sh flink/download-connectors.sh
sudo docker compose build --no-cache flink-jobmanager flink-taskmanager
sudo docker compose up -d --force-recreate flink-jobmanager flink-taskmanager
```

The Dockerfile copies local `flink/lib/*.jar` files into `/opt/flink/lib`.
