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
   - PostgreSQL CDC for inbound and outbound orders to Kafka ODS topic `ods_order_status_change_raw`
   - Kafka ODS topic to ClickHouse DWD table `dwd_order_status_change`
   - Kafka ODS topic to Kafka DWD topic `dwd_order_status_change`
   - Uses an internal numeric warehouse order id to avoid collisions: inbound `id * 2`, outbound `id * 2 + 1`

2. `../flink-job/src/main/java/com/example/wms/realtime/OrderQ10mTimeoutJob.java`
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

The compose file gives the Flink TaskManager 4 task slots. The realtime flow normally runs at least three jobs at the same time: CDC to ODS, ODS to DWD, and the DWS/ADS timer job.

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

This starts the DWS/ADS timer job. Keep the SQL jobs running too, because they move PostgreSQL CDC events into Kafka ODS and then Kafka DWD.
