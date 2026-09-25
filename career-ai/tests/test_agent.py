"""Agent 模块测试：安全节点 + 工具定义 + 图结构。
"""

from __future__ import annotations

import pytest

from agent.safety import CRISIS_RESPONSE, detect_crisis, safety_check_node


class TestSafetyDetection:
    """危机关键词检测测试。"""

    def test_detect_crisis_positive(self):
        assert detect_crisis("我想自杀") is True
        assert detect_crisis("我最近很抑郁") is True
        assert detect_crisis("我有心理疾病") is True

    def test_detect_crisis_negative(self):
        assert detect_crisis("我想了解计算机方向") is False
        assert detect_crisis("我的画像是什么？") is False
        assert detect_crisis("") is False
        assert detect_crisis(None) is False

    @pytest.mark.asyncio
    async def test_safety_node_blocks_crisis(self):
        from langchain_core.messages import HumanMessage

        state = {"messages": [HumanMessage(content="我想自杀")]}
        result = await safety_check_node(state)
        assert result["safety"]["needs_human_support"] is True
        assert result["messages"][0].content == CRISIS_RESPONSE

    @pytest.mark.asyncio
    async def test_safety_node_allows_normal(self):
        from langchain_core.messages import HumanMessage

        state = {"messages": [HumanMessage(content="我适合什么职业？")]}
        result = await safety_check_node(state)
        assert result["safety"]["needs_human_support"] is False
        assert "messages" not in result


class TestTools:
    """工具定义测试。"""

    def test_tools_registered(self):
        from agent.tools import ALL_TOOLS

        tool_names = {t.name for t in ALL_TOOLS}
        assert "get_student_portrait" in tool_names
        assert "get_assessment_scores" in tool_names
        assert "get_recommendation_results" in tool_names
        assert "get_plan_progress" in tool_names
        assert "get_career_directions" in tool_names
        assert "get_direction_detail" in tool_names
        assert "search_knowledge" in tool_names
        assert len(ALL_TOOLS) == 7

    def test_tools_have_docstrings(self):
        from agent.tools import ALL_TOOLS

        for tool in ALL_TOOLS:
            assert tool.description, f"工具 {tool.name} 缺少描述"


class TestGraph:
    """Agent 图结构测试。"""

    def test_graph_builds(self):
        from agent.graph import build_graph

        graph = build_graph()
        assert graph is not None

    def test_graph_has_expected_nodes(self):
        from agent.graph import build_graph

        graph = build_graph()
        nodes = set(graph.nodes.keys())
        assert "safety" in nodes
        assert "model" in nodes
        assert "tools" in nodes


class TestPrompt:
    """系统提示词测试。"""

    def test_system_prompt_persona(self):
        from agent.prompts import SYSTEM_PROMPT

        assert "职业顾问" in SYSTEM_PROMPT
        assert "脱敏" in SYSTEM_PROMPT
        assert "不虚构" in SYSTEM_PROMPT


class TestLLMFactory:
    """LLM 工厂测试（不实际调用网关）。"""

    def test_get_agent_model_returns_chatopenai(self, monkeypatch):
        monkeypatch.setenv("GATEWAY_API_KEY", "test-key")
        monkeypatch.setenv("LLM_MODEL", "deepseek-v4-flash")
        from agent.llm import get_agent_model

        get_agent_model.cache_clear()
        model = get_agent_model()
        assert model.model_name == "deepseek-v4-flash"
        get_agent_model.cache_clear()
