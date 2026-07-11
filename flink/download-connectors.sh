#!/usr/bin/env sh
set -eu

mkdir -p "$(dirname "$0")/lib"
cd "$(dirname "$0")/lib"

download() {
  url="$1"
  file="${url##*/}"
  if [ -s "$file" ]; then
    echo "exists $file"
    return
  fi
  echo "download $file"
  curl -fL --retry 5 --retry-delay 5 -o "$file" "$url"
}

BASE="${MAVEN_REPO:-https://repo.maven.apache.org/maven2}"

download "$BASE/org/apache/flink/flink-sql-connector-kafka/3.2.0-1.19/flink-sql-connector-kafka-3.2.0-1.19.jar"
download "$BASE/org/apache/flink/flink-connector-kafka/3.2.0-1.19/flink-connector-kafka-3.2.0-1.19.jar"
download "$BASE/org/apache/kafka/kafka-clients/3.7.0/kafka-clients-3.7.0.jar"
download "$BASE/org/apache/flink/flink-connector-jdbc/3.2.0-1.19/flink-connector-jdbc-3.2.0-1.19.jar"
download "$BASE/org/apache/flink/flink-sql-connector-postgres-cdc/3.2.1/flink-sql-connector-postgres-cdc-3.2.1.jar"
download "$BASE/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar"
download "$BASE/com/clickhouse/clickhouse-jdbc/0.6.3/clickhouse-jdbc-0.6.3-all.jar"
