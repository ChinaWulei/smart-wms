# Agent module convention

Each agent owns its prompt, skills, memory policy, and optional knowledge:

```text
agents/<name>/
  agent.py
  prompt.py
  memory/AGENTS.md
  skills/
  knowledge/   # optional
```

- `agent.py`: constructs the specialist with `create_deep_agent`.
- `prompt.py`: system instructions owned by that agent.
- `skills/`: explicit capabilities. Skills that write data must document their scope.
- `memory/AGENTS.md`: persistent agent instructions loaded by Deep Agents.
- `knowledge/`: static domain material loaded only by that agent.

Shared configuration, Gemini client, backend transport, top-level Deep Agent,
and checkpointers live under `app/core`.

The top-level supervisor wraps each specialist using `CompiledSubAgent` and
delegates with Deep Agents' built-in `task` tool.

Checkpointers currently use process memory. Replace `app/core/memory.py` with
PostgreSQL checkpointers when conversations must survive restarts or run across
multiple AI-service replicas.
