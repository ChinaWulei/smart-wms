---
name: schema-exploration
description: Lists Smart WMS tables, describes columns, explains table meanings, and maps warehouse entity relationships. Use when the user asks what data exists, what a table means, which fields to use, or how tables relate.
---

# Smart WMS Schema Exploration

## Workflow

### 1. List Tables

Use `list_wms_tables` to see the allowed business tables and Chinese table
descriptions.

### 2. Inspect Relevant Schemas

Use `get_wms_schema` with comma-separated table names, for example:

`products,stocks,warehouses,storage_locations`

Inspect:

- Column names and PostgreSQL data types
- Nullable fields
- Table descriptions
- Relationship hints

### 3. Map Business Relationships

Common relationship paths:

- Current inventory:
  `stocks -> products`, `stocks -> warehouses`,
  `stocks -> storage_locations`
- Location hierarchy:
  `warehouses -> warehouse_zones -> shelves -> storage_locations`
- Inbound details:
  `inbound_orders -> inbound_order_items -> products`
- Outbound details:
  `outbound_orders -> outbound_order_items -> products`
- Inventory movement history:
  `stock_movements -> products/warehouses/storage_locations`
- Inventory check differences:
  `inventory_checks -> inventory_check_items -> products`

### 4. Answer Clearly

When answering schema questions:

- Explain what each table represents in WMS business terms.
- Mention the key fields the user should care about.
- Mention joins needed for typical questions.
- Do not expose `app_users.password`.

## Table Groups

- Master data: `products`, `warehouses`, `warehouse_zones`, `shelves`,
  `storage_locations`, `app_users`
- Inventory: `stocks`, `stock_movements`, `stock_alerts`
- Inbound: `inbound_orders`, `inbound_order_items`
- Outbound and picking: `outbound_orders`, `outbound_order_items`
- Stocktaking: `inventory_checks`, `inventory_check_items`
- Shipping: `shipping_jobs`
- AI history: `ai_reports`
