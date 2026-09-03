"""
Phase 4 重写: 从本地 career-core-apis-live.yaml 生成 summary。
原 organize_apifox_apis.py 走 CLI 但不含本地补的 2 个 gateway 路径，
所以基于 YAML 重新生成，与本地 OAS 完全同步。
"""
import json
from pathlib import Path
from collections import OrderedDict, defaultdict

YAML_PATH = Path(r"D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live.yaml")
OUT = Path(r"D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live-summary.md")

with open(YAML_PATH, "r", encoding="utf-8") as f:
    doc = json.load(f)

paths = doc["paths"]
tags_list = doc.get("tags", [])

# tag 名 → 序号（按 tags 列表出现顺序）
tag_order = {t["name"]: i + 1 for i, t in enumerate(tags_list)}

# 收集 (method, path, summary) 并按 tag 分组
grouped = OrderedDict()
method_count = defaultdict(int)
total = 0

for path_name in sorted(paths.keys()):
    path_item = paths[path_name]
    for method in ("get", "post", "put", "patch", "delete", "head", "options"):
        if method not in path_item:
            continue
        op = path_item[method]
        # 默认 tag：路径前缀推断（兜底）
        op_tags = op.get("tags") or []
        primary_tag = op_tags[0] if op_tags else None
        if not primary_tag:
            for prefix in ("ai", "advisor", "admin", "auth", "students", "assessment",
                           "questionnaires", "profile-snapshots", "directions", "paths",
                           "recommendation", "reviews", "gateway"):
                if f"/{prefix}" in path_name:
                    primary_tag = prefix
                    break
        primary_tag = primary_tag or "其他"

        grouped.setdefault(primary_tag, []).append({
            "method": method.upper(),
            "path": path_name,
            "summary": op.get("summary") or "",
        })
        method_count[method.upper()] += 1
        total += 1

# 按 tag 出现顺序排序分组
def tag_sort_key(tag_name):
    return (tag_order.get(tag_name, 9999), tag_name)

sorted_tags = sorted(grouped.keys(), key=tag_sort_key)

# 生成 Markdown
lines = []
lines.append("# 生涯规划系统 · 全量 API 接口清单（Apifox 线上 + 本地补全）")
lines.append("")
lines.append(
    f"> 数据来源：Apifox 项目「生涯规划系统」(ID 8662286) 主分支 + 本地手工补全 ｜ "
    f"接口总数：**{total}** ｜ 目录：{len(sorted_tags)} 个"
)
lines.append("")
lines.append("## 一、统计概览")
lines.append("")
lines.append("| HTTP 方法 | 数量 |")
lines.append("|-----------|------|")
for m in ["GET", "POST", "PUT", "PATCH", "DELETE"]:
    lines.append(f"| {m} | {method_count.get(m, 0)} |")
lines.append("")

for idx, tag_name in enumerate(sorted_tags, 1):
    items = grouped[tag_name]
    lines.append(f"## 二.{idx} {tag_name}（{len(items)} 个）")
    lines.append("")
    lines.append("| # | 方法 | 路径 | 说明 |")
    lines.append("|---|------|------|------|")
    items_sorted = sorted(items, key=lambda x: x["path"])
    for i, it in enumerate(items_sorted, 1):
        lines.append(f"| {i} | `{it['method']}` | `{it['path']}` | {it['summary']} |")
    lines.append("")

content = "\n".join(lines)
# 与原文件风格一致：UTF-8 + CRLF
content_bytes = content.encode("utf-8").replace(b"\n", b"\r\n")
with open(OUT, "wb") as f:
    f.write(content_bytes)

print(f"OK: {total} interfaces / {len(sorted_tags)} folders -> {OUT}")
print(f"Size: {len(content_bytes)} bytes")
