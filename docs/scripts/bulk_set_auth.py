# -*- coding: utf-8 -*-
"""批量给 Apifox 项目所有接口设置 BearerAuth + {{ACCESS_TOKEN}}。

做法:对每个人口 `endpoint get` 拿完整结构 -> 改 auth/securityScheme -> `endpoint update` 回写。
安全:update 会整体覆盖,所以先 get 全量再在原结构上改,避免丢字段。
"""
import json
import subprocess
import sys
import uuid

PROJECT_ID = "8662286"
SCHEME_ID = 1687020          # BearerAuth
TOKEN_VAR = "{{ACCESS_TOKEN}}"
CLI = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"
TMP = r"C:\Users\uio8k\AppData\Local\Temp\opencode"


def run_cli(args):
    cmd = [CLI] + args
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    return p.stdout, p.stderr, p.returncode


def get_endpoint(eid):
    out = f"{TMP}\\ep_get_{eid}.json"
    cmd = f'cmd /c "{CLI} endpoint get {eid} --project {PROJECT_ID} > "{out}""'
    subprocess.run(cmd, shell=True, check=True)
    with open(out, encoding="utf-8") as f:
        return json.load(f)["data"]


def already_ok(ep):
    if ep.get("auth", {}).get("type") != "securityscheme":
        return False
    ss = ep.get("securityScheme") or {}
    cfg = (ss.get("use", {}).get("configs", {}).get(str(SCHEME_ID), {}))
    tok = cfg.get("authConfigs", {}).get("x-apifox", {}).get("token")
    return tok == TOKEN_VAR


def apply_auth(ep):
    """在原 endpoint 上原地设置 BearerAuth。返回是否发生了变化。"""
    if already_ok(ep):
        return False
    gid = "bearer-grp-" + uuid.uuid4().hex[:8]
    new_auth = {"type": "securityscheme"}
    new_ss = {
        "schemeGroups": [{"id": gid, "schemeIds": [SCHEME_ID]}],
        "required": False,
        "use": {
            "id": gid,
            "configs": {
                str(SCHEME_ID): {
                    "authConfigs": {"x-apifox": {"token": TOKEN_VAR}}
                }
            },
        },
        "scopes": {gid: {str(SCHEME_ID): []}},
    }
    changed = False
    if ep.get("auth") != new_auth:
        ep["auth"] = new_auth
        changed = True
    if ep.get("securityScheme") != new_ss:
        ep["securityScheme"] = new_ss
        changed = True
    return changed


def update_endpoint(ep):
    out = f"{TMP}\\ep_upd_{ep['id']}.json"
    with open(out, "w", encoding="utf-8") as f:
        json.dump(ep, f, ensure_ascii=False)
    so, se, rc = run_cli(["endpoint", "update", str(ep["id"]), "--project", PROJECT_ID, "--file", out])
    if rc != 0:
        return False, se
    return True, so


def main():
    # 从已导出的列表读取所有 endpoint id
    with open(f"{TMP}\\endpoints_list.json", encoding="utf-8") as f:
        listing = json.load(f)
    ids = [it["id"] for it in listing["data"]]

    only_id = sys.argv[1] if len(sys.argv) > 1 else None
    if only_id:
        ids = [int(only_id)]

    ok, skip, fail = 0, 0, 0
    for i, eid in enumerate(ids):
        try:
            if i % 20 == 0 and i > 0:
                import time
                time.sleep(1)  # 轻量限流保护
            ep = get_endpoint(eid)
            if not apply_auth(ep):
                skip += 1
                print(f"[skip] {eid} 已配置")
                continue
            ok_u, msg = update_endpoint(ep)
            if ok_u:
                ok += 1
                print(f"[ ok ] {eid} {ep.get('method','').upper()} {ep.get('path')}")
            else:
                fail += 1
                print(f"[FAIL] {eid}: {msg[:200]}")
        except Exception as e:
            fail += 1
            print(f"[ERR ] {eid}: {e}")
    print(f"\n完成: ok={ok} skip={skip} fail={fail} total={len(ids)}")


if __name__ == "__main__":
    main()
