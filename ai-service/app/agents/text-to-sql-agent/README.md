# Smart WMS Text-to-SQL Agent

This agent answers warehouse data questions from the AI chat by inspecting the
PostgreSQL schema, generating read-only SQL, executing it, and explaining the
result in business language.

It uses the project's configured Gemini model and connects directly to the
Smart WMS PostgreSQL database.

## Runtime

The production entrypoint is:

`app.agents.text_to_sql.agent:create_text_to_sql_agent`

The files in this directory provide the agent memory and skills:

- `AGENTS.md`: WMS table meanings, relationships, and safety rules
- `skills/schema-exploration/SKILL.md`: schema discovery workflow
- `skills/query-writing/SKILL.md`: PostgreSQL query workflow and examples

## Environment

The AI service reads:

```text
WMS_DB_DSN=postgresql://user:password@postgres:5432/smart_wms
```

For local docker compose, the default points to the existing `postgres` service.
For production or interview demo hardening, create a read-only database user and
set `AI_DB_USERNAME` / `AI_DB_PASSWORD` or override `WMS_DB_DSN`.

Recommended PostgreSQL setup:

```sql
create user ai_reader with password 'change-this-password';
grant connect on database smart_wms to ai_reader;
grant usage on schema public to ai_reader;
grant select on all tables in schema public to ai_reader;
alter default privileges in schema public grant select on tables to ai_reader;
```

The Python tool also rejects non-query SQL and executes queries in a read-only
transaction.

## Manual Test

From `ai-service`:

```bash
python app/agents/text-to-sql-agent/agent.py "当前仓库低库存商品有哪些？"
```
