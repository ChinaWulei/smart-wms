---
name: query-writing
description: Writes and executes PostgreSQL SELECT queries for Smart WMS inventory, inbound, outbound, picking, shipping, alerts, stock movements, and warehouse reports.
---

# Smart WMS Query Writing

## Required Workflow

1. Identify the business object in the question.
2. Use `get_wms_schema` for all likely tables before writing SQL.
3. Write one PostgreSQL `SELECT` or read-only `WITH` query.
4. Use `execute_readonly_sql` to run it.
5. Explain the result, including filters, date range, warehouse scope, and
   calculation logic.

## Safety

- Only read data. Never write, update, delete, create, alter, or drop.
- Do not query `app_users.password`.
- Do not use `SELECT *`.
- Default to small result sets. The tool caps results at 100 rows.
- If the user asks for all data, summarize or aggregate first.

## Common Query Patterns

### Current Stock by SKU

```sql
select
  p.sku,
  p.name,
  w.code as warehouse_code,
  l.code as location_code,
  s.quantity,
  s.allocated_quantity,
  s.quantity - s.allocated_quantity as available_quantity
from stocks s
join products p on p.id = s.product_id
join warehouses w on w.id = s.warehouse_id
join storage_locations l on l.id = s.location_id
where p.sku = 'SKU001'
order by available_quantity desc
```

### Low Stock Products

```sql
select
  p.sku,
  p.name,
  p.category,
  p.current_stock,
  p.safety_stock,
  p.safety_stock - p.current_stock as shortage_quantity
from products p
where p.current_stock < p.safety_stock
order by shortage_quantity desc
```

### Outbound Quantity by Product

```sql
select
  p.sku,
  p.name,
  sum(oi.quantity) as outbound_quantity
from outbound_order_items oi
join outbound_orders o on o.id = oi.order_id
join products p on p.id = oi.product_id
where o.created_at >= current_date - interval '30 days'
group by p.sku, p.name
order by outbound_quantity desc
```

### Inbound Receiving Progress

```sql
select
  o.order_no,
  o.status,
  sum(i.quantity) as expected_quantity,
  sum(i.received_quantity) as received_quantity,
  round(sum(i.received_quantity)::numeric / nullif(sum(i.quantity), 0) * 100, 2) as progress_percent
from inbound_orders o
join inbound_order_items i on i.order_id = o.id
group by o.order_no, o.status
order by o.created_at desc
```

### Picking Progress

```sql
select
  o.order_no,
  o.status,
  sum(i.quantity) as required_quantity,
  sum(i.picked_quantity) as picked_quantity
from outbound_orders o
join outbound_order_items i on i.order_id = o.id
where o.status in ('READY_TO_PICK', 'PICKING', 'PICKED')
group by o.order_no, o.status
order by o.created_at desc
```

## Warehouse Scope

If runtime context contains `warehouse_id=<id>`, filter warehouse-level queries:

- `stocks.warehouse_id = <id>`
- `stock_movements.warehouse_id = <id>`
- item tables such as `inbound_order_items.warehouse_id = <id>`
- `shipping_jobs.warehouse_id = <id>`

If the user explicitly asks for all warehouses, do not apply the runtime
warehouse filter.

## Date and Time

- Use `created_at` for record creation time.
- Use `movement_time` for stock movement time.
- Use `completed_at`, `received_at`, `picked_at`, `shipped_at` for business
  completion timestamps.
- For "today", use `current_date`.
- For "last 7/30 days", use `current_date - interval '7 days'` or
  `current_date - interval '30 days'`.

## Output

After executing SQL, explain:

- The answer in plain Chinese.
- The table and field basis.
- Any truncation or missing data.
- Any assumptions, especially date range and warehouse scope.
