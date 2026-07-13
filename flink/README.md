# Smart WMS Realtime Warehouse Jobs

## Layer Ownership

- PostgreSQL `smart_wms`: OLTP source only. Do not create DWD, DWS, or ADS tables here.
- Kafka `ods_order_status_change_raw`: realtime ODS topic for raw order status change events.
- ClickHouse `smart_wms_dw`: warehouse tables for DWD, DWS, and ADS.

## Why ClickHouse and Flink Both Have `CREATE TABLE`

`clickhouse/init/*.sql` creates physical tables in ClickHouse.

`flink/sql/*.sql` creates Flink connector mappings. Those mappings tell Flink how to read Kafka ODS and write Kafka DWD. They are not another set of physical database tables.

## Jobs

1. `sql/realtime_warehouse_layers.sql`
   - Reads Debezium raw CDC events from Kafka ODS topic `ods_order_status_change_raw`
   - Parses Debezium `before`, `after`, and `op`
   - Kafka ODS topic to Kafka DWD topic `dwd_order_status_change`
   - Uses an internal numeric warehouse order id to avoid collisions: inbound `id * 2`, outbound `id * 2 + 1`

Debezium Kafka Connect captures PostgreSQL `inbound_orders` and `outbound_orders` and routes both tables into the raw ODS topic `ods_order_status_change_raw`.

2. `../flink-job/src/main/java/com/example/wms/realtime/OrderQ10mTimeoutJob.java`
   - Consumes Kafka DWD topic `dwd_order_status_change`
   - Uses keyed state per order
   - Registers a 10 minute timer when an order enters Q
   - Writes ClickHouse DWS table `dws_order_q_10m_timeout_detail`
   - Updates ClickHouse ADS table `ads_order_q_10m_timeout_summary`
   - Refreshes ClickHouse ADS tables `ads_order_status_count` and `ads_order_creation_7d_trend` for dashboard charts

## Build Flink Image

Download connector jars before building the Flink image:

```bash
sh flink/download-connectors.sh
sudo docker compose build --no-cache flink-jobmanager flink-taskmanager
sudo docker compose up -d --force-recreate flink-jobmanager flink-taskmanager
```

The Dockerfile copies local `flink/lib/*.jar` files into `/opt/flink/lib`.

The compose file gives the Flink TaskManager 4 task slots. The realtime flow normally runs Debezium for CDC to ODS, one Flink SQL job for ODS to DWD, and one DataStream timer job for DWS/ADS.

The DataStream timer job connects to ClickHouse as `wms_dw` with no password by default. Existing ClickHouse volumes do not rerun init scripts, so create this user manually once on already-deployed servers:

```bash
sudo docker exec -it smart-wms-clickhouse-1 clickhouse-client --query "CREATE USER IF NOT EXISTS wms_dw IDENTIFIED WITH no_password"
sudo docker exec -it smart-wms-clickhouse-1 clickhouse-client --query "GRANT SELECT, INSERT ON smart_wms_dw.* TO wms_dw"
```

## Package and Submit DataStream Job

Build the job jar:

```bash
cd flink-job
mvn clean package -DskipTests
```

Copy the jar into the Flink JobManager container and submit it:

```bash
sudo docker cp target/wms-flink-job.jar smart-wms-flink-jobmanager-1:/opt/flink/usrlib/wms-flink-job.jar
sudo docker exec -it smart-wms-flink-jobmanager-1 \
  /opt/flink/bin/flink run \
  -c com.example.wms.realtime.OrderQ10mTimeoutJob \
  /opt/flink/usrlib/wms-flink-job.jar
```

This starts the DWS/ADS timer job. Keep Debezium Connect and the Flink SQL job running too, because Debezium moves PostgreSQL CDC events into Kafka ODS and Flink SQL moves Kafka ODS into Kafka DWD.
