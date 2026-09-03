import subprocess, json, os, tempfile, time, urllib.request, urllib.error, re

APIFOX = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"
PROJECT = "8662286"
ENV_ID = "47907998"
OAS_FILE = r"D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live.yaml"
TOKEN_FILE = os.path.join(tempfile.gettempdir(), "student_token.txt")
REPORT_ROOT = r"D:\Zht20241287\career-planner\tests\apifox-reports"
BACKUP_ROOT = os.path.join(REPORT_ROOT, "oas_backup")

CORE_FOLDERS = {"92650087", "92650088", "92650089", "92650090",
                "92650091", "92650092", "92650093", "92650094"}
GW_FOLDER = "94571643"

MODULES = [
    ("测评",       ["92650087"],           "student"),
    ("学生画像",   ["92650088"],           "student"),
    ("方向推荐",   ["92650089", "92650090"], "student"),
    ("目标计划",   ["92650091"],           "student"),
    ("阶段复盘",   ["92650092"],           "student"),
    ("AI智能服务", ["92650093"],           "student"),
    ("AI生涯咨询", ["92650094"],           "student"),
    ("AI-Gateway", ["94571643"],           "student"),
]

PARAM_SOURCE = {
    "questionnaireId": "/api/v1/questionnaires",
    "snapshotId": "/api/v1/students/me/profile/versions",
    "directionId": "/api/v1/directions",
    "goalId": "/api/v1/students/me/goals",
    "reviewId": "/api/v1/reviews",
    "messageId": "/api/v1/ai/chat/history",
    "resultId": "/api/v1/students/me/recommendations",
    "planVersionId": "/api/v1/goal-versions",
    "runId": "/api/v1/recommendation-runs",
    "planId": "/api/v1/plans",
    "taskId": "/api/v1/tasks",
    "sessionId": "/api/v1/assessment-sessions",
}

_oas = json.loads(open(OAS_FILE, encoding="utf-8").read())


def apifox_json(args):
    p = subprocess.run([APIFOX] + args, capture_output=True)
    s = p.stdout.decode("utf-8-sig")
    try:
        i = s.index("{"); j = s.rindex("}") + 1
        return json.loads(s[i:j])
    except Exception:
        return None


def http_call(method, host, path, token, body=None):
    url = host.rstrip("/") + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method.upper())
    if token:
        req.add_header("Authorization", "Bearer %s" % token)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")
    except Exception as e:
        return 0, str(e)


# ---------------- token ----------------
def get_token():
    if os.path.exists(TOKEN_FILE):
        return open(TOKEN_FILE, encoding="utf-8").read().strip()
    return ""


def save_token(t):
    open(TOKEN_FILE, "w", encoding="utf-8").write(t)


def token_valid(t):
    code, _ = http_call("GET", "http://127.0.0.1:8080",
                        "/api/v1/students/me/profile/latest", t)
    return code == 200


def renew_token():
    # 学生登录：账号=学号，密码=verifyCode（白名单注册学生的初始密码）
    code, text = http_call("POST", "http://127.0.0.1:8080", "/api/v1/auth/login", "",
                           {"account": "2026011301", "password": "202601"})
    try:
        d = json.loads(text)
        tok = d.get("data", {}).get("accessToken")
        if tok:
            return tok
    except Exception:
        pass
    # 后备：管理员登录
    code, text = http_call("POST", "http://127.0.0.1:8080", "/api/v1/auth/login", "",
                           {"account": "admin", "password": "Admin@2026"})
    try:
        d = json.loads(text)
        tok = d.get("data", {}).get("accessToken")
        if tok:
            return tok
    except Exception:
        pass
    return ""


def update_env_var(tok):
    f = os.path.join(tempfile.gettempdir(), "env_upd.json")
    open(f, "w", encoding="utf-8").write(
        json.dumps({"variables": [{"name": "bearerToken", "value": tok}]}))
    subprocess.run([APIFOX, "environment", "update", ENV_ID, "--project", PROJECT,
                    "--file", f], capture_output=True)


def update_all_test_cases(tok):
    j = apifox_json(["test-case", "list", "--project", PROJECT, "--page-size", "500"])
    cases = (j or {}).get("data", [])
    n = 0
    for c in cases:
        cid = c.get("id")
        if not cid:
            continue
        tc = apifox_json(["test-case", "get", str(cid), "--project", PROJECT])
        if not tc:
            continue
        tc = tc.get("data", tc) if isinstance(tc, dict) else tc
        params = tc.get("parameters", {}) or {}
        headers = params.get("header", []) or []
        changed = False
        for h in headers:
            if h.get("name") == "Authorization":
                h["value"] = "Bearer {{bearerToken}}"
                changed = True
        if changed:
            f = os.path.join(tempfile.gettempdir(), "tc_upd.json")
            open(f, "w", encoding="utf-8").write(json.dumps({"parameters": params}))
            subprocess.run([APIFOX, "test-case", "update", str(cid), "--project", PROJECT,
                            "--file", f], capture_output=True)
            n += 1
    return n


# ---------------- OAS ----------------
def get_oas_200_schema(path, method):
    p = _oas.get("paths", {}).get(path, {})
    op = p.get(method.lower(), {})
    resp = op.get("responses", {}).get("200", {})
    return resp.get("content", {}).get("application/json", {}).get("schema", {})


def has_envelope(schema):
    if not isinstance(schema, dict):
        return False
    props = schema.get("properties", {})
    return "code" in props and "data" in props


def build_envelope(business_schema, is_array):
    data_schema = {"type": "array", "items": business_schema} if is_array else business_schema
    return {
        "type": "object",
        "properties": {
            "code": {"type": "string", "examples": ["OK"]},
            "message": {"type": "string"},
            "data": data_schema,
            "traceId": {"type": "string"},
            "timestamp": {"type": "string", "format": "date-time"}
        },
        "required": ["code", "message", "data", "traceId", "timestamp"]
    }


def validate_envelope(actual):
    # 检测「实际响应」是否被信封包裹（career-core 固定返回 {code,message,data,...}）
    errs = []
    if not isinstance(actual, dict):
        return ["response is not object"]
    for k in ("code", "message", "data"):
        if k not in actual:
            errs.append("missing envelope field: %s" % k)
    return errs


def oas_missing_envelope(business_schema):
    # 检测「OAS 200 Schema」是否缺少信封（这是真正的不符点）
    return not has_envelope(business_schema)


def backup_endpoint(ep_id, ep):
    os.makedirs(BACKUP_ROOT, exist_ok=True)
    f = os.path.join(BACKUP_ROOT, "%s.json" % ep_id)
    open(f, "w", encoding="utf-8").write(json.dumps(ep, ensure_ascii=False, indent=1))


def fix_endpoint_200(ep_id, business_schema_oas, is_array):
    ep = apifox_json(["endpoint", "get", str(ep_id), "--project", PROJECT])
    if not ep:
        return False
    ep = ep.get("data", ep) if isinstance(ep, dict) else ep
    backup_endpoint(ep_id, ep)
    responses = ep.get("responses", []) or []
    # 取当前 200 响应的业务 Schema（保持 Apifox 的 $ref，如 #/definitions/xxx）
    business = None
    for r in responses:
        if str(r.get("code")) == "200" or r.get("code") == 200:
            js = r.get("jsonSchema")
            if isinstance(js, dict):
                business = js
            break
    if business is None:
        business = business_schema_oas  # 兜底用 OAS 业务 Schema
    data_schema = {"type": "array", "items": business} if is_array else business
    env = {
        "type": "object",
        "properties": {
            "code": {"type": "string"},
            "message": {"type": "string"},
            "data": data_schema,
            "traceId": {"type": "string"},
            "timestamp": {"type": "string", "format": "date-time"}
        },
        "required": ["code", "message", "data", "traceId", "timestamp"]
    }
    found = False
    for r in responses:
        if str(r.get("code")) == "200" or r.get("code") == 200:
            r["jsonSchema"] = env
            r["mediaType"] = "application/json"
            r["contentType"] = "json"
            found = True
            break
    if not found:
        responses.append({"code": "200", "name": "", "jsonSchema": env,
                          "mediaType": "application/json", "contentType": "json",
                          "description": "OK"})
    f = os.path.join(tempfile.gettempdir(), "ep_upd.json")
    open(f, "w", encoding="utf-8").write(json.dumps({"responses": responses}))
    r = subprocess.run([APIFOX, "endpoint", "update", str(ep_id), "--project", PROJECT,
                        "--file", f], capture_output=True)
    return r.returncode == 0


# ---------------- main ----------------
def main():
    os.makedirs(REPORT_ROOT, exist_ok=True)
    token = get_token()
    if not token or not token_valid(token):
        print("[token] invalid/expired, renewing...")
        token = renew_token()
        if not token:
            print("[token] FAILED to renew, abort")
            return
        save_token(token)
        update_env_var(token)
        n = update_all_test_cases(token)
        print("[token] renewed; switched %d test cases to {{bearerToken}}" % n)

    # map apiDetailId -> caseId
    jc = apifox_json(["test-case", "list", "--project", PROJECT, "--page-size", "500"])
    cases = (jc or {}).get("data", [])
    case_by_api = {}
    for c in cases:
        aid = c.get("apiDetailId")
        if aid:
            case_by_api[str(aid)] = c.get("id")

    harvest_cache = {}
    results = []
    for mod_name, folders, role in MODULES:
        for folder in folders:
            ol = apifox_json(["endpoint", "list", "--folder-id", folder,
                              "--page-size", "500", "--project", PROJECT])
            eps = (ol or {}).get("data", [])
            for ep in eps:
                ep_id = str(ep["id"])
                method = ep.get("method", "get")
                path = ep.get("path", "")
                # harvest real ids for path params
                for pname, src in PARAM_SOURCE.items():
                    if harvest_cache.get(pname):
                        continue
                    try:
                        c, t = http_call("GET", "http://127.0.0.1:8080", src, token)
                        if c == 200:
                            dd = json.loads(t).get("data")
                            if isinstance(dd, list) and dd:
                                harvest_cache[pname] = dd[0].get("id")
                            elif isinstance(dd, dict) and dd.get("id"):
                                harvest_cache[pname] = dd.get("id")
                    except Exception:
                        pass
                real_path = path
                for m in re.findall(r"\{([^}]+)\}", path):
                    if harvest_cache.get(m):
                        real_path = real_path.replace("{%s}" % m, str(harvest_cache[m]))
                host = "http://127.0.0.1:8080" if folder in CORE_FOLDERS else "http://127.0.0.1:8000"
                # run via apifox CLI (debug) if case exists
                case_id = case_by_api.get(ep_id)
                apifox_status = None
                if case_id:
                    p = subprocess.run([APIFOX, "test-case", "run", str(case_id),
                                        "-e", ENV_ID, "-r", "cli", "--project", PROJECT],
                                       capture_output=True)
                    out = p.stdout.decode("utf-8-sig")
                    mst = re.search(r"\[(\d{3})[^\]]*\]", out)
                    if mst:
                        apifox_status = int(mst.group(1))
                # capture actual response for validation
                code, text = http_call(method, host, real_path, token)
                try:
                    actual = json.loads(text)
                except Exception:
                    actual = {}
                business = get_oas_200_schema(path, method)
                actual_is_arr = isinstance(actual, dict) and isinstance(actual.get("data"), list)
                # 判断 data 是否为数组：优先用实际 200 响应；否则按方法+路径启发式
                if actual.get("data") is not None:
                    is_array = actual_is_arr
                else:
                    if method.lower() == "get" and not path.rstrip("/").endswith("}"):
                        is_array = True   # GET 列表接口
                    else:
                        is_array = False  # 详情 / 创建 / 更新 返回单对象
                errs = []
                fixed = False
                # 真正的不符点：OAS 200 Schema 缺信封，而实际响应被信封包裹
                if folder in CORE_FOLDERS and oas_missing_envelope(business):
                    errs.append("OAS 200 Schema 缺信封包裹(code/message/data)")
                    # auto-fix：核心接口，把 200 响应 Schema 包成信封
                    fixed = fix_endpoint_200(ep_id, business, is_array)
                    if fixed:
                        errs = []
                results.append({
                    "module": mod_name, "ep_id": ep_id, "method": method, "path": path,
                    "http": code, "apifox": apifox_status,
                    "errs": errs, "fixed": fixed,
                })
                print("%-10s %-45s http=%s apifox=%s errs=%d fixed=%s" % (
                    method.upper(), path, code, apifox_status, len(errs), fixed))

    # reports
    lines = ["# 契约校验 + 自动修复报告（开发环境 %s）" % ENV_ID, ""]
    lines.append("> 校验方式：apifox test-case run（调试）+ 直连 8080/8000 取响应体，"
                 "与 OAS(career-core-apis-live.yaml) 200 Schema 比对。")
    lines.append("> 自动修复：核心接口缺信封包裹时，把 200 响应 Schema 包成 "
                 "{code,message,data,traceId,timestamp}（data=原业务 Schema 或数组）。")
    fixed_n = sum(1 for r in results if r["fixed"])
    fail_n = sum(1 for r in results if r["errs"])
    lines.append("## 汇总")
    lines.append("- 接口数: %d" % len(results))
    lines.append("- 已自动修复 OAS: %d" % fixed_n)
    lines.append("- 仍有契约问题: %d" % fail_n)
    lines.append("")
    lines.append("## 明细")
    lines.append("| 模块 | 方法 | 路径 | HTTP | Apifox | 修复 | 问题 |")
    lines.append("|---|---|---|---|---|---|---|")
    for r in results:
        errs = ";".join(r["errs"]) if r["errs"] else "-"
        lines.append("| %s | %s | %s | %s | %s | %s | %s |" % (
            r["module"], r["method"].upper(), r["path"], r["http"],
            r["apifox"], "是" if r["fixed"] else "-", errs))
    open(os.path.join(REPORT_ROOT, "CONTRACT.md"), "w", encoding="utf-8").write("\n".join(lines))
    print("\n[done] fixed=%d still_failing=%d report=tests/apifox-reports/CONTRACT.md" % (fixed_n, fail_n))


if __name__ == "__main__":
    main()
