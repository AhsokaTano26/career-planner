# -*- coding: utf-8 -*-
"""
用 Apifox CLI 为 8 个模块逐个接口创建持久测试用例 + 调试运行，并捕获返回响应。
- apifox test-case create / run : 持久测试用例 + 状态（调试视图）
- 同时用直连 HTTP 调用同一接口，保存完整返回响应体供查看（Apifox CLI 报告不含响应体）
"""
import subprocess, json, os, sys, urllib.request, urllib.error, ssl, time

APIFOX = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"
PROJECT = "8662286"
ENV_ID = "47907998"
# 环境「本地环境」(47907998): default = http://127.0.0.1:8080(career-core);
# bc3ac089... = http://127.0.0.1:8000(career-ai). 核心 /api/v1/* 走 default(8080)，
# 仅 AI-Gateway(94571643) 走 career-ai(8000)。
CORE_SRV = ""  # 核心接口：空 serverId = 继承环境 default(8080)
GW_SRV = "bc3ac089-4fe2-441b-9a61-e921625e4e2d"  # AI-Gateway 专用(8000)
PY = r"C:/Users/uio8k/.local/bin/python.exe"

CORE_FOLDERS = {"92650087","92650088","92650089","92650090","92650091","92650092","92650093","92650094"}
AI_FOLDER = "94571643"

MODULES = [
    ("测评",       ["92650087"], "student"),
    ("学生画像",   ["92650088"], "student"),
    ("方向推荐",   ["92650089","92650090"], "student"),
    ("目标计划",   ["92650091"], "student"),
    ("阶段复盘",   ["92650092"], "student"),
    ("AI智能服务", ["92650093"], "student"),
    ("AI生涯咨询", ["92650094"], "student"),
    ("AI-Gateway", ["94571643"], "student"),
]

CATEGORY_NORMAL = 12521868

# 路径参数 -> 用于取样本 ID 的列表接口（均为 career-core）
PARAM_SOURCE = {
    "questionnaireId": "/api/v1/questionnaires",
    "snapshotId": "/api/v1/students/me/profile/versions",
    "directionId": "/api/v1/directions",
    "goalId": "/api/v1/students/me/goals",
    "reviewId": "/api/v1/reviews",
    "messageId": "/api/v1/ai/chat/history",
    "resultId": "/api/v1/students/me/recommendations",
    "planVersionId": "/api/v1/goal-versions",
}

REPORT_ROOT = r"D:\Zht20241287\career-planner\tests\apifox-reports"
os.makedirs(REPORT_ROOT, exist_ok=True)

# ---------- Apifox CLI 封装 ----------
def apifox(args):
    cmd = [APIFOX, *args, "--project", PROJECT]
    p = subprocess.run(cmd, capture_output=True)
    return p.stdout.decode("utf-8-sig"), p.stderr.decode("utf-8-sig")

def apifox_json(args):
    out, err = apifox(args)
    # Apifox 输出可能在前缀提示文字后再跟 JSON，需截取第一个 '{' 到最后一个 '}'
    try:
        s = out[out.index("{"): out.rindex("}") + 1]
        return json.loads(s)
    except Exception:
        return {"__raw__": out, "__err__": err}

# ---------- 登录 ----------
def login(account, password):
    body = json.dumps({"account": account, "password": password}).encode()
    req = urllib.request.Request(
        "http://127.0.0.1:8080/api/v1/auth/login",
        data=body, headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            d = json.loads(r.read().decode("utf-8-sig"))
        return find_token(d)
    except Exception as e:
        print("  [login fail]", e)
        return None

def find_token(o):
    if isinstance(o, dict):
        for k, v in o.items():
            if k.lower() in ("token", "accesstoken", "access_token"):
                return v
            r = find_token(v)
            if r:
                return r
    if isinstance(o, list):
        for i in o:
            r = find_token(i)
            if r:
                return r
    return None

# ---------- OAS 样例体 ----------
OAS = None
def load_oas():
    global OAS
    if OAS is None:
        p = r"D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live.yaml"
        OAS = json.loads(open(p, encoding="utf-8-sig").read())
    return OAS

def oas_body(path, method):
    try:
        o = load_oas()["paths"][path][method.lower()]
        rb = o.get("requestBody", {})
        c = rb.get("content", {}).get("application/json", {})
        return c.get("example")
    except Exception:
        return None

# ---------- 直连 HTTP ----------
def http_call(method, url, token, body_obj):
    headers = {"Authorization": "Bearer " + token}
    data = None
    if method.lower() in ("post", "put", "patch"):
        headers["Content-Type"] = "application/json"
        data = json.dumps(body_obj, ensure_ascii=False).encode("utf-8") if body_obj is not None else b"{}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method.upper())
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            code = r.status
            text = r.read().decode("utf-8-sig", "ignore")
    except urllib.error.HTTPError as e:
        code = e.code
        text = e.read().decode("utf-8-sig", "ignore")
    except Exception as e:
        code = -1
        text = "HTTP_ERROR: %s" % e
    return code, text

# ---------- serverId 修正 ----------
def ensure_server(ep_id, full, target):
    if not isinstance(full, dict) or "id" not in full:
        return False  # 未拿到完整对象，跳过，避免破坏性更新
    if full.get("serverId") == target:
        return True
    e = dict(full)
    e["serverId"] = target
    for k in ["createdAt","updatedAt","creatorId","editorId","creatorUserId","editorUserId"]:
        e.pop(k, None)
    tmp = os.path.join(REPORT_ROOT, "_ep_%s.json" % ep_id)
    open(tmp, "w", encoding="utf-8").write(json.dumps(e, ensure_ascii=False))
    out, err = apifox(["endpoint", "update", ep_id, "--file", tmp])
    try:
        return json.loads(out[out.index("{"):out.rindex("}")+1]).get("success", False)
    except Exception:
        return False

# ---------- 取样本 ID ----------
harvest_cache = {}
def harvest_ids(token):
    for param, src in PARAM_SOURCE.items():
        if param in harvest_cache:
            continue
        url = "http://127.0.0.1:8080" + src
        code, text = http_call("GET", url, token, None)
        val = None
        try:
            d = json.loads(text)
            arr = None
            if isinstance(d, dict):
                data = d.get("data")
                if isinstance(data, list):
                    arr = data
                elif isinstance(data, dict):
                    for kk in ("list","items","content","records"):
                        if isinstance(data.get(kk), list):
                            arr = data[kk]; break
            if arr:
                first = arr[0] if isinstance(arr[0], dict) else {}
                val = first.get(param) or first.get("id")
        except Exception:
            pass
        harvest_cache[param] = val
        print("    harvest %s <- %s = %s" % (param, src, val))

def resolve_path_params(ep, token):
    params = ep.get("parameters", {}).get("path", []) or []
    out = []
    for p in params:
        name = p.get("name")
        # 优先用真实 harvested ID；其次 endpoint example；最后占位
        if name in harvest_cache and harvest_cache.get(name):
            val = harvest_cache[name]
        elif name.endswith("Id") and harvest_cache.get("id"):
            val = harvest_cache["id"]
        else:
            ex = (p.get("schema") or {}).get("examples")
            if ex:
                val = ex[0] if isinstance(ex, list) else ex
            else:
                val = (p.get("example") or name + "_demo")
        out.append({"name": name, "value": str(val), "type": "string", "enable": True})
    return out

def build_url(folder, path, path_params):
    host = "http://127.0.0.1:8080" if folder in CORE_FOLDERS else "http://127.0.0.1:8000"
    url = path
    for pp in path_params:
        url = url.replace("{%s}" % pp["name"], pp["value"])
    return host + url

# ---------- 主流程 ----------
def process_module(name, folders, role, tokens):
    token = tokens[role]
    print("\n===== 模块: %s (folders=%s) =====" % (name, folders))
    mod_dir = os.path.join(REPORT_ROOT, name)
    os.makedirs(mod_dir, exist_ok=True)
    results = []
    for folder in folders:
        target_srv = CORE_SRV if folder in CORE_FOLDERS else GW_SRV
        # 列出接口
        ol = apifox_json(["endpoint", "list", "--folder-id", folder, "--page-size", "500"])
        eps = ol.get("data", []) if isinstance(ol, dict) else []
        print("  folder %s -> %d endpoints" % (folder, len(eps)))
        for ep in eps:
            ep_id = str(ep["id"])
            method = ep.get("method", "get")
            path = ep.get("path", "")
            print("  - %s %s" % (method.upper(), path))
            try:
                # 修正 serverId
                full = apifox_json(["endpoint", "get", ep_id])
                full = full.get("data", {}) if isinstance(full, dict) else {}
                ensure_server(ep_id, full, target_srv)
                harvest_ids(token)
                path_params = resolve_path_params(full if full else ep, token)
                url = build_url(folder, path, path_params)
                # 请求体
                body = None
                if method.lower() in ("post","put","patch"):
                    body = oas_body(path, method)
                    if body is None:
                        body = {}
                    else:
                        # 将 body 中与 harvested id 同名的字段替换为真实 id
                        if isinstance(body, dict):
                            for k in list(body.keys()):
                                if k in harvest_cache and harvest_cache.get(k):
                                    body[k] = harvest_cache[k]
                # 1) 创建持久测试用例
                case = {
                    "apiDetailId": int(ep_id),
                    "categoryId": CATEGORY_NORMAL,
                    "name": "%s %s" % (method.upper(), path),
                    "method": method.lower(),
                    "path": path,
                    "parameters": {
                        "path": path_params,
                        "query": [],
                        "header": [{"name": "Authorization", "value": "Bearer " + token, "type": "string", "enable": True}],
                        "cookie": [],
                    },
                    "commonParameters": {},
                    "requestBody": {"type": ("application/json" if body is not None else "none"), "data": (json.dumps(body, ensure_ascii=False) if body is not None else "")},
                }
                tmp = os.path.join(REPORT_ROOT, "_case_%s.json" % ep_id)
                open(tmp, "w", encoding="utf-8").write(json.dumps(case, ensure_ascii=False))
                oc = apifox_json(["test-case", "create", "--file", tmp])
                case_id = None
                if isinstance(oc, dict):
                    case_id = oc.get("data", {}).get("id")
                # 2) 运行（调试视图，取状态）
                status_apifox = None
                if case_id:
                    out, err = apifox(["test-case", "run", str(case_id), "-e", ENV_ID, "-r", "cli,json", "--out-dir", mod_dir])
                    import re
                    m = re.search(r"\[(\d{3})[^\]]*\]", out)
                    if m:
                        status_apifox = int(m.group(1))
                # 3) 直连捕获返回响应体
                code, text = http_call(method, url, token, body)
                # 捕获创建出的 id，供后续详情接口复用
                if method.lower() in ("post", "put", "patch") and str(code).startswith("2"):
                    try:
                        bd = json.loads(text)
                        created = None
                        if isinstance(bd, dict):
                            d = bd.get("data")
                            if isinstance(d, dict):
                                created = d.get("id") or d.get("sessionId") or d.get("snapshotId") or d.get("reviewId") or d.get("goalId") or d.get("directionId")
                        if created:
                            harvest_cache["id"] = str(created)
                    except Exception:
                        pass
                # 保存响应
                resp_file = os.path.join(mod_dir, "%s_%s.response.json" % (ep_id, method.lower()))
                open(resp_file, "w", encoding="utf-8").write(json.dumps({"url": url, "status": code, "body": text}, ensure_ascii=False, indent=2))
                results.append({"ep": ep_id, "method": method, "path": path, "caseId": case_id, "apifoxStatus": status_apifox, "httpStatus": code})
                print("      apifox=%s http=%s case=%s" % (status_apifox, code, case_id))
            except Exception as ex:
                print("      ERROR:", ex)
                results.append({"ep": ep_id, "method": method, "path": path, "error": str(ex)})
    # 模块汇总
    sum_file = os.path.join(mod_dir, "SUMMARY.md")
    lines = ["# 模块 %s 测试汇总" % name, "", "| 接口 | 方法 | 路径 | 测试用例ID | Apifox状态 | HTTP状态 |", "|---|---|---|---|---|---|"]
    for r in results:
        lines.append("| %s | %s | %s | %s | %s | %s |" % (r.get("ep"), r.get("method"), r.get("path"), r.get("caseId"), r.get("apifoxStatus"), r.get("httpStatus")))
    open(sum_file, "w", encoding="utf-8").write("\n".join(lines))
    return results

def load_student_token():
    # 学生通过白名单注册（无密码），优先用已注册拿到的 token 文件
    f = os.path.join(os.environ.get("TEMP", "/tmp"), "student_token.txt")
    if os.path.exists(f):
        t = open(f, encoding="utf-8-sig").read().strip()
        if t:
            return t
    # 否则尝试注册一个新学生
    body = json.dumps({"studentNo": "2026011301", "name": "测试同学", "className": "计科2601", "verifyCode": "202601"}).encode()
    req = urllib.request.Request("http://127.0.0.1:8080/api/v1/auth/register",
                                 data=body, headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            d = json.loads(r.read().decode("utf-8-sig"))
        return find_token(d)
    except Exception as e:
        print("  [register fail]", e)
        return None

def main():
    only = sys.argv[1] if len(sys.argv) > 1 else None
    student = load_student_token()
    admin = login("admin", "Admin@2026")
    tokens = {"student": student, "admin": admin}
    print("student token:", "OK" if student else "NONE", "| admin token:", "OK" if admin else "NONE")
    all_results = {}
    for name, folders, role in MODULES:
        if only and name != only:
            continue
        all_results[name] = process_module(name, folders, role, tokens)
    # 总汇总
    total = sum(len(v) for v in all_results.values())
    ok = sum(1 for v in all_results.values() for r in v if str(r.get("httpStatus")).startswith("2"))
    print("\n===== 总计: %d 接口, HTTP 2xx: %d =====" % (total, ok))

if __name__ == "__main__":
    main()
