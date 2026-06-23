---
name: report-generation
description: Generate and persist a PDF-downloadable WMS operations report from approved structured warehouse data.
---

# WMS report generation

1. Extract the requested warehouse and report scope.
2. Call `load_report_data`.
3. Write a report with core metrics, anomalies, risks, and actions.
4. Call `persist_report` using the exact JSON returned by `load_report_data`.
5. Return the exact `REPORT_ID=<number>` from the persistence tool.
6. Finish with `AGENT=report`.
