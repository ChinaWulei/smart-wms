from langgraph.checkpoint.memory import MemorySaver


# Short-term conversation memory for the supervisor and each compiled subagent.
# Replace with a PostgreSQL checkpointer when sessions must survive restarts.
supervisor_checkpointer = MemorySaver()
rules_checkpointer = MemorySaver()
analytics_checkpointer = MemorySaver()
report_checkpointer = MemorySaver()
