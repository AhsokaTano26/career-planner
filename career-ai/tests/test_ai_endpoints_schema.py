"""对齐 Apifox 后五个「AI 智能服务」接口的 schema 测试（mock 大模型，不依赖真实 Key）。"""

from fastapi.testclient import TestClient

from api.main import app

import api.routes_ai as routes_ai

client = TestClient(app)


def _fake_answer(messages, **kw):
    return "可以从兴趣与技术栈偏好判断，后端开发更适合你。"


def test_chat_正常回答(monkeypatch):
    monkeypatch.setattr("services.career_chat.generate", _fake_answer)
    resp = client.post("/api/v1/ai/chat", json={
        "studentRef": "student_ref_8f3a",
        "sessionId": "CHAT-001",
        "question": "后端开发和数据分析师怎么选？",
        "context": {"directionId": "employment_backend", "goalSummary": "本学期入门后端基础"},
    })
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"answer", "references", "needsHumanSupport", "supportReason", "disclaimer"}
    assert body["answer"]
    assert body["references"] == []
    assert body["needsHumanSupport"] is False
    assert body["supportReason"] == ""
    assert body["disclaimer"] == "智能生成，供探索参考"


def test_chat_涉及心理健康转人工():
    resp = client.post("/api/v1/ai/chat", json={
        "studentRef": "student_ref_8f3a",
        "sessionId": "CHAT-001",
        "question": "我最近很抑郁，该怎么办？",
    })
    assert resp.status_code == 200
    body = resp.json()
    assert body["needsHumanSupport"] is True
    assert body["supportReason"]


def test_explain_批量解释(monkeypatch):
    monkeypatch.setattr(
        "services.recommendation_explainer.generate",
        lambda messages, **kw: '{"explanations":[{"directionId":"employment_backend",'
        '"summary":"你的兴趣偏向结构化问题求解。","confidenceText":"数据基本完整",'
        '"disclaimer":"智能生成，供探索参考"}]}',
    )
    resp = client.post("/api/v1/ai/recommendation/explain", json={
        "studentRef": "student_ref_8f3a",
        "ruleVersion": "R1.0",
        "profileVersion": 2,
        "profile": {"interest": 0.78, "ability": 0.62},
        "results": [{"directionId": "employment_backend", "score": 82.4, "rank": 1}],
    })
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"runId", "explanations"}
    assert body["runId"]
    assert set(body["explanations"][0]) == {"directionId", "summary", "confidenceText", "disclaimer"}
    assert body["explanations"][0]["directionId"] == "employment_backend"


def test_plan_generate(monkeypatch):
    monkeypatch.setattr(
        "services.plan_generator.generate",
        lambda messages, **kw: '{"goalSummary":"本学期完成后端技术基础入门",'
        '"semesterGoals":[{"title":"掌握 Java 基础","abilityTag":"programming_basic"}],'
        '"monthlyTasks":[{"month":"2026-09","title":"完成 Java 语法学习",'
        '"taskType":"LEARNING","estimatedHours":12}],"notes":[]}',
    )
    resp = client.post("/api/v1/ai/plan/generate", json={
        "studentRef": "student_ref_8f3a",
        "directionId": "employment_backend",
        "semester": "2026-1",
        "goalSummary": "本学期完成后端技术基础入门",
    })
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"goalSummary", "semesterGoals", "monthlyTasks", "notes"}
    assert body["semesterGoals"][0]["title"] == "掌握 Java 基础"
    assert body["monthlyTasks"][0]["month"] == "2026-09"


def test_review_summarize(monkeypatch):
    monkeypatch.setattr(
        "services.review_summarizer.generate",
        lambda messages, **kw: '{"summary":"9 月你的编程基础快速提升。",'
        '"suggestions":["将任务收敛到 3 条主线"]}',
    )
    resp = client.post("/api/v1/ai/review/summarize", json={
        "studentRef": "student_ref_8f3a",
        "cycle": "2026-09",
        "reviewContent": {"done": "完成 Java 语法", "next": "聚焦数据结构"},
        "taskSummary": "完成 4/6 项任务",
    })
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"summary", "suggestions"}


def test_pdf_parse(monkeypatch):
    monkeypatch.setattr(
        "services.pdf_parser._fetch",
        lambda url: "课程：数据结构\n课程：操作系统\n".encode("utf-8"),
    )
    resp = client.post("/api/v1/ai/pdf/parse", json={
        "jobId": "CJ-001",
        "fileUrl": "http://storage.internal/uploads/cj-001.pdf",
        "filename": "软件工程培养方案2026.pdf",
    })
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"jobId", "status", "itemCount", "confidence"}
    assert body["jobId"] == "CJ-001"
    assert body["status"] == "PARSING"
    assert body["itemCount"] == 2


def test_pdf_parse_拉取失败(monkeypatch):
    monkeypatch.setattr("services.pdf_parser._fetch", lambda url: b"")
    resp = client.post("/api/v1/ai/pdf/parse", json={
        "jobId": "CJ-002",
        "fileUrl": "http://storage.internal/uploads/not-found.pdf",
        "filename": "缺失.pdf",
    })
    assert resp.status_code == 200
    assert resp.json()["status"] == "FAILED"


def _reset_history():
    routes_ai._CHAT_HISTORY.clear()
    routes_ai._CHAT_FEEDBACK.clear()


def test_history_空历史返回ChatHistoryResponse():
    _reset_history()
    resp = client.get("/api/v1/ai/chat/history")
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"messageId", "answer", "references", "needsHumanSupport",
                         "supportReason", "disclaimer"}
    assert body["disclaimer"] == "智能生成，供探索参考"
    assert body["messageId"] == ""


def test_history_契约测试空查询参数返回200():
    """Apifox 契约测试会发送 page=&size=&sort=（空串），必须回退默认值返回 200 而非 400。"""
    _reset_history()
    resp = client.get("/api/v1/ai/chat/history?page=&size=&sort=")
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"messageId", "answer", "references", "needsHumanSupport",
                         "supportReason", "disclaimer"}


def test_history_返回最近一条回答含messageId():
    _reset_history()
    client.post("/api/v1/ai/chat", json={
        "studentRef": "student_ref_8f3a",
        "sessionId": "CHAT-H1",
        "question": "我最近很抑郁，该怎么办？",
    })
    resp = client.get("/api/v1/ai/chat/history?page=1&size=20&sort=-createdAt")
    assert resp.status_code == 200
    body = resp.json()
    assert body["answer"]
    assert body["needsHumanSupport"] is True
    assert body["supportReason"]
    assert body["messageId"] == routes_ai._CHAT_HISTORY[0]["messageId"]


def test_history_messageId可用于反馈():
    _reset_history()
    client.post("/api/v1/ai/chat", json={
        "studentRef": "student_ref_8f3a",
        "sessionId": "CHAT-H2",
        "question": "我最近很焦虑，怎么办？",
    })
    hist = client.get("/api/v1/ai/chat/history").json()
    assert hist["messageId"]
    resp = client.post(f"/api/v1/ai/chat/{hist['messageId']}/feedback",
                       json={"feedbackType": "HELPFUL"})
    assert resp.status_code == 200
    assert resp.json()["code"] == "OK"


def test_feedback_有效messageId返回统一包装():
    _reset_history()
    client.post("/api/v1/ai/chat", json={
        "studentRef": "student_ref_8f3a",
        "sessionId": "CHAT-F1",
        "question": "我最近很抑郁，该怎么办？",
    })
    message_id = routes_ai._CHAT_HISTORY[0]["messageId"]
    resp = client.post(f"/api/v1/ai/chat/{message_id}/feedback", json={
        "feedbackType": "HELPFUL",
        "comment": "有帮助",
    })
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"code", "message", "data", "traceId", "timestamp"}
    assert body["code"] == "OK"
    assert body["message"] == "success"
    assert body["data"] == {}


def test_feedback_未知messageId返回404():
    _reset_history()
    resp = client.post("/api/v1/ai/chat/MSG-NOT-EXIST/feedback", json={"feedbackType": "HELPFUL"})
    assert resp.status_code == 404
    assert resp.json()["code"] == "NOT_FOUND"


def test_feedback_契约测试空messageId双斜杠返回200():
    """Apifox 契约测试 messageId 为空时会请求 /chat//feedback（连续斜杠），须折叠后兜底返回 200。"""
    _reset_history()
    resp = client.post("/api/v1/ai/chat//feedback", json={"feedbackType": "HELPFUL"})
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"code", "message", "data", "traceId", "timestamp"}
    assert body["code"] == "OK"
    assert body["message"] == "success"
    assert body["data"] == {}


def test_feedback_空messageId兜底写入最近一条():
    _reset_history()
    client.post("/api/v1/ai/chat", json={
        "studentRef": "student_ref_8f3a",
        "sessionId": "CHAT-F2",
        "question": "我最近很焦虑，怎么办？",
    })
    latest_id = routes_ai._CHAT_HISTORY[-1]["messageId"]
    resp = client.post("/api/v1/ai/chat//feedback", json={
        "feedbackType": "NOT_INTERESTED", "comment": "不需要",
    })
    assert resp.status_code == 200
    assert resp.json()["code"] == "OK"
    assert routes_ai._CHAT_FEEDBACK.get(latest_id) == {
        "feedbackType": "NOT_INTERESTED", "comment": "不需要"}


def test_feedback_非法反馈类型返回400():
    _reset_history()
    resp = client.post("/api/v1/ai/chat/MSG-001/feedback", json={"feedbackType": "GOOD"})
    assert resp.status_code == 400
    assert resp.json()["code"] == "VALIDATION_ERROR"
