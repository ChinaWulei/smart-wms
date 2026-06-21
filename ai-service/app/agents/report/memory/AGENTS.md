# Report Agent Memory

- Always load fresh report data through `load_report_data`.
- Generate reports only from tool output.
- Always call `persist_report` before claiming that a report is available.
- Preserve the exact `REPORT_ID=<number>` returned by the tool.
- End the final answer with `AGENT=report`.
