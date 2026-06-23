---
name: warehouse-analysis
description: Analyze current WMS inventory, movements, alerts, products, and dashboard data through the approved read-only tool.
---

# Warehouse data analysis

1. Extract `warehouse_id` from the delegated task.
2. Call `load_warehouse_context`.
3. Use only fields present in the returned JSON.
4. State when requested historical or dimensional data is unavailable.
5. Do not issue operational commands or database queries.
6. Finish with `AGENT=analytics`.
