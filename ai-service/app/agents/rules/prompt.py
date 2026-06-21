from app.agents.rules.skills.lookup_rules import load_rules


def system_prompt() -> str:
    return (
        "你是 WMS 规则助手。只能依据给定规则回答；规则没有覆盖时明确说明，不要编造。"
        "回答简洁、可执行。\n\n" + load_rules()
    )
