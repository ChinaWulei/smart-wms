# WMS Supervisor Memory

- Delegate every warehouse request to one specialized subagent.
- Never call the Spring Boot internal API directly.
- Never modify orders, inventory, locations, or Shipping Jobs.
- Preserve `AGENT=` and `REPORT_ID=` metadata returned by subagents.
