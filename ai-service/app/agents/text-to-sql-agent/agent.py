import argparse
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.agents.text_to_sql.agent import create_text_to_sql_agent  # noqa: E402


def main():
    parser = argparse.ArgumentParser(description="Smart WMS Text-to-SQL Agent")
    parser.add_argument("question", type=str, help="Warehouse data question")
    args = parser.parse_args()

    agent = create_text_to_sql_agent()
    result = agent.invoke({"messages": [{"role": "user", "content": args.question}]})
    print(result["messages"][-1].content)


if __name__ == "__main__":
    main()
