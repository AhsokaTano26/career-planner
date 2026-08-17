"""对 career-ai 五个「AI 智能服务」接口做端到端冒烟测试（对齐 Apifox 契约后）。

用法（服务已在本机 8000 运行）：
    & "D:\\Zht20241287\\career-planner\\career-ai\\.venv\\Scripts\\python.exe" \\
        "D:\\Zht20241287\\career-planner\\career-ai\\tests\\test_five_ai_endpoints.py"

仅依赖 httpx + 标准库；pdf/parse 用本地临时 HTTP 文件服务模拟内网 fileUrl。
"""

from __future__ import annotations

import http.server
import json
import socketserver
import sys
import threading

import httpx

BASE = "http://127.0.0.1:8000"
TIMEOUT = 60.0

RESULTS = []


def report(name: str, resp: httpx.Response, body) -> None:
    ok = 200 <= resp.status_code < 300
    RESULTS.append((name, ok, resp.status_code))
    print(f"\n===== {name} =====")
    print(f"HTTP {resp.status_code}  {'OK' if ok else 'FAIL'}")
    try:
        print(json.dumps(body, ensure_ascii=False, indent=2)[:1500])
    except Exception:
        print(str(body)[:1500])


class _Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        data = "课程：数据结构\n课程：操作系统\n课程：数据库原理\n".encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/pdf")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *args):
        pass


def _start_file_server() -> str:
    httpd = socketserver.TCPServer(("127.0.0.1", 0), _Handler)
    port = httpd.server_address[1]
    thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    thread.start()
    return f"http://127.0.0.1:{port}/cj-001.pdf"


def main() -> int:
    file_url = _start_file_server()
    with httpx.Client(timeout=TIMEOUT) as client:
        r = client.get(f"{BASE}/health")
        report("GET /health", r, r.json())

        # 1. PDF 解析（JSON body + fileUrl，非 multipart）
        r = client.post(f"{BASE}/api/v1/ai/pdf/parse", json={
            "jobId": "CJ-001",
            "fileUrl": file_url,
            "filename": "软件工程培养方案2026.pdf",
        })
        report("POST /api/v1/ai/pdf/parse", r, r.json())

        # 2. 生涯咨询问答（依赖大模型，Key 无效时 503）
        r = client.post(f"{BASE}/api/v1/ai/chat", json={
            "studentRef": "student_ref_8f3a",
            "sessionId": "CHAT-001",
            "question": "后端开发和数据分析师怎么选？",
            "context": {"directionId": "employment_backend", "goalSummary": "本学期入门后端基础"},
        })
        report("POST /api/v1/ai/chat", r, r.json())

        # 3. 生成学期计划草案（依赖大模型）
        r = client.post(f"{BASE}/api/v1/ai/plan/generate", json={
            "studentRef": "student_ref_8f3a",
            "directionId": "employment_backend",
            "semester": "2026-1",
            "goalSummary": "本学期完成后端技术基础入门",
        })
        report("POST /api/v1/ai/plan/generate", r, r.json())

        # 4. 生成推荐解释（依赖大模型）
        r = client.post(f"{BASE}/api/v1/ai/recommendation/explain", json={
            "studentRef": "student_ref_8f3a",
            "ruleVersion": "R1.0",
            "profileVersion": 2,
            "profile": {"interest": 0.78, "ability": 0.62},
            "results": [{"directionId": "employment_backend", "score": 82.4, "rank": 1}],
        })
        report("POST /api/v1/ai/recommendation/explain", r, r.json())

        # 5. 生成阶段总结（依赖大模型）
        r = client.post(f"{BASE}/api/v1/ai/review/summarize", json={
            "studentRef": "student_ref_8f3a",
            "cycle": "2026-09",
            "reviewContent": {"done": "完成 Java 语法", "next": "聚焦数据结构"},
            "taskSummary": "完成 4/6 项任务",
        })
        report("POST /api/v1/ai/review/summarize", r, r.json())

    print("\n\n===== 汇总 =====")
    failed = 0
    for name, ok, code in RESULTS:
        print(f"[{'PASS' if ok else 'FAIL'}] {code}  {name}")
        if not ok:
            failed += 1
    print(f"\n共 {len(RESULTS)} 项，失败 {failed} 项")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
