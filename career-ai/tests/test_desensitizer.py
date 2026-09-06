"""脱敏服务测试：对照《具体实现细节_MVP_V1.0.md》4.6。

覆盖：手机号（裸号/+86/分隔符）、邮箱、身份证（18位/15位/X结尾）、学号、
中文夹杂数字（回归 \b 边界 bug）、混合文本；断言出站无原值 + 类型化占位符 + 命中计数。
顺带校验各 AI 能力入口（career_chat / review_summarize）出站 prompt 无 PII。
"""

import pytest

from services.desensitizer import (
    desensitize,
    desensitize_detailed,
    mask_free_text,
    truncate,
)


def _no_leak(masked: str, *originals: str) -> None:
    for orig in originals:
        if not orig:
            continue
        assert orig not in masked, f"原值未脱敏: {orig!r}"


# ---------------------------------------------------------------- 单元：单类型
def test_手机号_裸号():
    masked, hits = desensitize_detailed("我的手机号13812345678，请联系")
    assert masked == "我的手机号[手机号]，请联系"
    assert hits == {"手机号": 1}
    _no_leak(masked, "13812345678")


def test_手机号_带86前缀与分隔符():
    masked, hits = desensitize_detailed("+86 138-1234-5678 或 0086 13912345678")
    assert hits.get("手机号") == 2
    _no_leak(masked, "13812345678", "13912345678")


def test_邮箱_标准格式():
    masked, hits = desensitize_detailed("邮箱 test@example.com 和 foo.bar_99@mail.cn")
    assert hits == {"邮箱": 2}
    _no_leak(masked, "test@example.com", "foo.bar_99@mail.cn")


def test_身份证_18位():
    masked, hits = desensitize_detailed("身份证110101199001011234，请核对")
    assert masked == "身份证[身份证号]，请核对"
    assert hits == {"身份证号": 1}
    _no_leak(masked, "110101199001011234")


def test_身份证_15位旧版():
    masked, hits = desensitize_detailed("旧版110105900101123")
    assert hits == {"身份证号": 1}
    _no_leak(masked, "110105900101123")


def test_身份证_X结尾():
    masked, hits = desensitize_detailed("尾号X: 11010119900101123X")
    assert hits == {"身份证号": 1}
    _no_leak(masked, "11010119900101123X")


def test_学号_10位():
    masked, hits = desensitize_detailed("学号2026011301，归属计科")
    assert masked == "学号[学号]，归属计科"
    assert hits == {"学号": 1}
    _no_leak(masked, "2026011301")


# ---------------------------------------------------------------- 单元：中文夹杂数字（回归 \b bug）
def test_中文夹杂数字_学号仍被识别():
    # 旧实现用 \b，汉字与数字间无边界 → 静默失效；环视方案应命中
    masked, hits = desensitize_detailed("同学的学号是2026011301，请查收成绩")
    assert hits == {"学号": 1}, f"中文夹杂数字未被脱敏: {masked!r}"
    _no_leak(masked, "2026011301")


def test_中文夹杂数字_手机号仍被识别():
    masked, hits = desensitize_detailed("打我电话13812345678就行")
    assert hits.get("手机号") == 1
    _no_leak(masked, "13812345678")


# ---------------------------------------------------------------- 单元：混合文本 + 占位符语义
def test_混合文本_类型化占位符与计数():
    text = "张三手机13812345678，邮箱a@b.com，身份证110101199001011234，学号2026011301"
    masked, hits = desensitize_detailed(text)
    assert masked == "张三手机[手机号]，邮箱[邮箱]，身份证[身份证号]，学号[学号]"
    assert hits == {"手机号": 1, "邮箱": 1, "身份证号": 1, "学号": 1}
    _no_leak(masked, "13812345678", "a@b.com", "110101199001011234", "2026011301")


def test_无敏感信息_原样返回():
    masked, hits = desensitize_detailed("今天天气不错，想聊聊职业方向")
    assert masked == "今天天气不错，想聊聊职业方向"
    assert hits == {}


# ---------------------------------------------------------------- 单元：位数阈值与误吞回归
def test_学号_8位与12位阈值():
    masked_8, hits_8 = desensitize_detailed("学号12345678")
    masked_12, hits_12 = desensitize_detailed("学号123456789012")
    assert hits_8 == {"学号": 1} and masked_8 == "学号[学号]"
    assert hits_12 == {"学号": 1} and masked_12 == "学号[学号]"


def test_学号_13位不误匹配():
    # 13 位超出学号兜底区间 (8-12)，且非合法身份证 → 不应脱敏
    masked, hits = desensitize_detailed("编号1234567890123")
    assert hits == {}
    assert "1234567890123" in masked


def test_身份证_不被内部手机形态抢匹配():
    # 18 位身份证中若含 1[3-9] 形态子串，应整体标 [身份证号]，而非被手机规则截断
    idc = "110101199301151234"
    masked, hits = desensitize_detailed(f"证件{idc}号")
    assert hits == {"身份证号": 1}
    assert masked == "证件[身份证号]号"
    _no_leak(masked, idc)


def test_多命中计数_同类型累加():
    masked, hits = desensitize_detailed("手机13812345678和13987654321都行")
    assert hits == {"手机号": 2}
    assert masked.count("[手机号]") == 2


def test_三类互不重叠_顺序正确():
    text = "手机13812345678 / 身份证110101199001011234 / 学号2026011301"
    masked, hits = desensitize_detailed(text)
    assert hits == {"手机号": 1, "身份证号": 1, "学号": 1}
    # 任一类型不应被另一类型占位符覆盖
    for ph in ("[手机号]", "[身份证号]", "[学号]"):
        assert ph in masked


def test_邮箱_含特殊字符():
    masked, hits = desensitize_detailed("联系 a%b+c@sub.example.com.cn 或 d_e@mail.io")
    assert hits == {"邮箱": 2}


# ---------------------------------------------------------------- 合规：命中计数写入日志且无原值
def test_mask_free_text_命中写入日志(monkeypatch):
    records = []

    class _Rec:
        def __init__(self, msg):
            self.msg = msg

    def fake_info(msg, *args):
        records.append(msg)

    monkeypatch.setattr("services.desensitizer.logger.info", fake_info)
    out = mask_free_text("手机13812345678", limit=None)
    assert "[手机号]" in out
    assert any("脱敏命中" in r for r in records)
    assert all("13812345678" not in r for r in records)


# ---------------------------------------------------------------- 性能回归
def test_性能_5千字含50个PII():
    import time

    chunks = [f"同学{i}手机1380000{i:04d}邮箱u{i}@example.com " for i in range(50)]
    text = ("正文" * 40 + "{}").join(chunks) + "正文" * 40
    start = time.perf_counter()
    masked, hits = desensitize_detailed(text)
    elapsed = (time.perf_counter() - start) * 1000
    assert hits.get("手机号") == 50 and hits.get("邮箱") == 50
    assert elapsed < 50, f"脱敏耗时 {elapsed:.1f}ms 超基线"


def test_性能_mask_free_text含截断():
    import time

    text = "手机13812345678" + "x" * 5000
    start = time.perf_counter()
    mask_free_text(text, limit=1500)
    elapsed = (time.perf_counter() - start) * 1000
    assert elapsed < 60, f"截断+脱敏耗时 {elapsed:.1f}ms 超基线"


# ---------------------------------------------------------------- 截断
def test_truncate_超过上限被截断():
    assert len(truncate("x" * 2500, 2000)) == 2000
    assert truncate("短文本", 2000) == "短文本"


# ---------------------------------------------------------------- 能力入口：出站无 PII
def test_career_chat_出站无原值(monkeypatch):
    captured = {}

    def fake_generate(messages, **kwargs):
        captured["messages"] = messages
        return "回答"

    monkeypatch.setattr("services.career_chat.generate", fake_generate)
    from services.career_chat import chat

    chat("我的手机号是13812345678，邮箱a@b.com，学号2026011301", user_ref="S1001")
    user_content = captured["messages"][1]["content"]
    _no_leak(user_content, "13812345678", "a@b.com", "2026011301")


def test_review_summarize_出站无原值(monkeypatch):
    captured = {}

    def fake_generate(messages, **kwargs):
        captured["messages"] = messages
        return '{"summary":"s","suggestions":[]}'

    monkeypatch.setattr("services.review_summarizer.generate", fake_generate)
    from services.review_summarizer import summarize

    summarize({"done": "完成了13812345678项目", "undone": "没做完 a@b.com", "next": "学号2026011301"},
              cycle="2026春", task_summary=None, user_ref="S1001")
    user_content = captured["messages"][1]["content"]
    _no_leak(user_content, "13812345678", "a@b.com", "2026011301")


def test_plan_generate_goalSummary脱敏(monkeypatch):
    captured = {}

    def fake_generate(messages, **kwargs):
        captured["messages"] = messages
        return '{"goalSummary":"g","semesterGoals":[],"monthlyTasks":[],"notes":[]}'

    monkeypatch.setattr("services.plan_generator.generate", fake_generate)
    from services.plan_generator import generate_plan

    generate_plan(direction_id="D1", semester="2026", goal_summary="我的手机13812345678，邮箱a@b.com",
                  user_ref="S1001")
    user_content = captured["messages"][1]["content"]
    _no_leak(user_content, "13812345678", "a@b.com")


def test_mask_free_text_截断与脱敏():
    long_text = "手机13812345678" + "x" * 2000
    masked = mask_free_text(long_text, limit=1500)
    assert len(masked) <= 1500
    assert "[手机号]" in masked
