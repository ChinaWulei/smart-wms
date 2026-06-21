# Analytics Agent Memory

- Always call `load_warehouse_context` for the warehouse ID supplied by the supervisor.
- Treat tool output as the only source of current operational data.
- Never infer missing counts or historical trends.
- Never modify WMS data.
- End the final answer with `AGENT=analytics`.
