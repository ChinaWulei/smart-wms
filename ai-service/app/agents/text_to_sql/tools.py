import json
import re

from langchain_core.tools import tool
import psycopg
from psycopg.rows import dict_row

from app.core.config import WMS_DB_DSN


MAX_LIMIT = 100
DEFAULT_LIMIT = 50
BUSINESS_TABLES = {
    "products": "商品主数据，包含 SKU、条码、名称、分类、安全库存和当前库存。",
    "warehouses": "仓库主表，保存仓库编码、名称、地址和负责人。",
    "warehouse_zones": "库区表，隶属于仓库。",
    "shelves": "货架表，隶属于库区。",
    "storage_locations": "库位表，记录库位编码、容量、占用量、状态、所属仓库和货架。",
    "stocks": "实时库存表，按商品、仓库、库位记录现有库存和已分配数量。",
    "stock_movements": "库存流水表，记录入库、出库、盘点盈亏等库存变动。",
    "stock_alerts": "库存预警表，主要用于低库存、积压等异常提醒。",
    "inbound_orders": "入库单主表，记录采购入库、退货入库等入库流程状态。",
    "inbound_order_items": "入库单明细，记录商品、仓库、库位、应收数量、实收数量和跟踪号。",
    "outbound_orders": "出库单主表，记录销售、领用、报损等出库流程状态、收货人和拣货时间。",
    "outbound_order_items": "出库单明细，记录商品、仓库、库位、需求数量、分配数量和已拣数量。",
    "inventory_checks": "盘点任务主表，记录盘点单号、状态、操作人和确认时间。",
    "inventory_check_items": "盘点明细，记录账面数量、实盘数量和差异数量。",
    "shipping_jobs": "发运任务表，将同车次同日期的多个出库单组成 Shipping Job，orders 为 jsonb 快照。",
    "ai_reports": "AI 运营报表存档。",
    "app_users": "系统用户表。不要查询或展示 password 字段。",
}
RELATIONSHIPS = {
    "warehouse_zones": ["warehouse_id -> warehouses.id"],
    "shelves": ["zone_id -> warehouse_zones.id"],
    "storage_locations": ["warehouse_id -> warehouses.id", "shelf_id -> shelves.id"],
    "stocks": ["product_id -> products.id", "warehouse_id -> warehouses.id", "location_id -> storage_locations.id"],
    "stock_movements": ["product_id -> products.id", "warehouse_id -> warehouses.id", "location_id -> storage_locations.id"],
    "stock_alerts": ["product_id -> products.id"],
    "inbound_order_items": ["order_id -> inbound_orders.id", "product_id -> products.id", "warehouse_id -> warehouses.id", "location_id -> storage_locations.id"],
    "outbound_order_items": ["order_id -> outbound_orders.id", "product_id -> products.id", "warehouse_id -> warehouses.id", "location_id -> storage_locations.id"],
    "inventory_check_items": ["check_task_id -> inventory_checks.id", "product_id -> products.id", "warehouse_id -> warehouses.id", "location_id -> storage_locations.id"],
}
FORBIDDEN_SQL = re.compile(
    r"\b(insert|update|delete|drop|alter|truncate|create|replace|merge|grant|revoke|copy|call|do|vacuum|analyze|refresh|lock|comment)\b",
    re.IGNORECASE,
)
SQL_COMMENT = re.compile(r"(--|/\*|\*/)")


def _connect():
    return psycopg.connect(WMS_DB_DSN, row_factory=dict_row, connect_timeout=10)


def _normalize_readonly_sql(sql: str) -> str:
    if not sql or not sql.strip():
        raise ValueError("SQL must not be blank")
    normalized = sql.strip()
    if normalized.endswith(";"):
        normalized = normalized[:-1].strip()
    if ";" in normalized:
        raise ValueError("Only one SQL statement is allowed")
    lower = normalized.lower()
    if not (lower.startswith("select ") or lower.startswith("with ")):
        raise ValueError("Only SELECT or read-only WITH queries are allowed")
    if FORBIDDEN_SQL.search(normalized) or SQL_COMMENT.search(normalized):
        raise ValueError("Forbidden SQL keyword or comment detected")
    return normalized


@tool
def list_wms_tables() -> str:
    """List allowed WMS business tables with Chinese descriptions."""
    tables = [{"table": name, "description": desc} for name, desc in sorted(BUSINESS_TABLES.items())]
    return json.dumps(tables, ensure_ascii=False)


@tool
def get_wms_schema(table_names: str = "") -> str:
    """Get column metadata and relationships for comma-separated WMS table names."""
    requested = [item.strip().lower() for item in table_names.split(",") if item.strip()]
    selected = requested or sorted(BUSINESS_TABLES)
    selected = [table for table in selected if table in BUSINESS_TABLES]
    if not selected:
        raise ValueError("No allowed WMS table names were requested")
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                select table_name, column_name, data_type, is_nullable
                from information_schema.columns
                where table_schema = 'public' and table_name = any(%s)
                order by table_name, ordinal_position
                """,
                (selected,),
            )
            rows = cur.fetchall()
    result = {}
    for table in selected:
        result[table] = {
            "description": BUSINESS_TABLES[table],
            "relationships": RELATIONSHIPS.get(table, []),
            "columns": [],
        }
    for row in rows:
        result[row["table_name"]]["columns"].append({
            "name": row["column_name"],
            "type": row["data_type"],
            "nullable": row["is_nullable"] == "YES",
        })
    return json.dumps(result, ensure_ascii=False)


@tool
def execute_readonly_sql(sql: str, limit: int = 50) -> str:
    """Execute one tool-validated read-only SELECT SQL statement against WMS PostgreSQL."""
    safe_limit = max(1, min(int(limit or 50), 100))
    normalized = _normalize_readonly_sql(sql)
    wrapped_sql = f"select * from ({normalized}) ai_sql_result limit %s"
    with _connect() as conn:
        conn.execute("set transaction read only")
        with conn.cursor() as cur:
            cur.execute(wrapped_sql, (safe_limit + 1,))
            rows = cur.fetchall()
    truncated = len(rows) > safe_limit
    if truncated:
        rows = rows[:safe_limit]
    return json.dumps(
        {
            "rows": rows,
            "rowCount": len(rows),
            "limit": safe_limit,
            "truncated": truncated,
            "executedSql": wrapped_sql.replace("%s", str(safe_limit + 1)),
        },
        ensure_ascii=False,
        default=str,
    )
