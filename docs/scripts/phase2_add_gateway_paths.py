"""
Phase 2: 补回 AI-Gateway 两条路径到已拉取的 OAS。
- /v1/chat/completions -> /api/v1/gateway/chat/completions
- /api/v1/gateway/generate 保留
- 新增 AI-Gateway tag
"""
import json
from pathlib import Path

YAML_PATH = Path(r"D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live.yaml")
BACKUP_PATH = Path(r"D:\Zht20241287\career-planner\docs\openapi\_local_backup_pre_sync.yaml")

with open(BACKUP_PATH, "r", encoding="utf-8") as f:
    backup = json.load(f)

with open(YAML_PATH, "r", encoding="utf-8") as f:
    target = json.load(f)

print(f"Backup openapi: {backup['openapi']}, paths: {len(backup['paths'])}, tags: {len(backup['tags'])}")
print(f"Target openapi: {target['openapi']}, paths: {len(target['paths'])}, tags: {len(target['tags'])}")

# 1. 复制两个 gateway 路径并重命名 chat/completions
gateway_generate = backup["paths"]["/api/v1/gateway/generate"]
chat_completions_old = backup["paths"]["/v1/chat/completions"]
chat_completions_new = json.loads(json.dumps(chat_completions_old))

# 重命名 operationId (chat_completions_v1_chat_completions_post -> chat_completions_api_v1_gateway_chat_completions_post)
for method, op in chat_completions_new.items():
    if "operationId" in op:
        op["operationId"] = op["operationId"].replace(
            "v1_chat_completions_post",
            "api_v1_gateway_chat_completions_post"
        )

target["paths"]["/api/v1/gateway/generate"] = gateway_generate
target["paths"]["/api/v1/gateway/chat/completions"] = chat_completions_new

print(f"After add: paths = {len(target['paths'])}")

# 2. 新增 AI-Gateway tag (若不存在)
existing_tag_names = {t["name"] for t in target["tags"]}
if "AI-Gateway" not in existing_tag_names:
    target["tags"].append({"name": "AI-Gateway"})
    print(f"Added tag: AI-Gateway, total tags: {len(target['tags'])}")

# 3. 校验：所有 $ref 引用的 schema 都存在
import re
def collect_refs(obj, refs):
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k == "$ref" and isinstance(v, str):
                refs.add(v)
            else:
                collect_refs(v, refs)
    elif isinstance(obj, list):
        for item in obj:
            collect_refs(item, refs)

all_refs = set()
collect_refs(target, all_refs)
schema_refs = {r.replace("#/components/schemas/", "") for r in all_refs if r.startswith("#/components/schemas/")}
defined = set(target["components"]["schemas"].keys())
missing = schema_refs - defined
print(f"Total $refs to schemas: {len(schema_refs)}, missing: {len(missing)}")
if missing:
    print(f"Missing: {sorted(missing)[:10]}")

# 4. 写回（保持 UTF-8 + CRLF，与原文件风格一致）
target_bytes = json.dumps(target, ensure_ascii=False, indent=2).encode("utf-8")
target_bytes = target_bytes.replace(b"\n", b"\r\n")

with open(YAML_PATH, "wb") as f:
    f.write(target_bytes)

print(f"\nWritten: {YAML_PATH}")
print(f"New size: {len(target_bytes)} bytes")
