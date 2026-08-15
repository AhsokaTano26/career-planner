# -*- coding: utf-8 -*-
"""将 CLI 生成的 collection 中 {taskId} 替换为 T163，生成可运行的 collection 文件。"""
import json
import os

TMP = os.environ.get("TEMP", ".")
src = os.path.join(TMP, "extracted_collection.json")
out = os.path.join(TMP, "collection_task_fixed.json")

with open(src, "r", encoding="utf-8-sig") as f:
    data = json.load(f)

for group in data.get("item", []):
    for it in group.get("item", []):
        url = it.get("request", {}).get("url", {})
        path = url.get("path", [])
        url["path"] = [p.replace("{taskId}", "T163") if isinstance(p, str) else p for p in path]
        url["variable"] = []

with open(out, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("WROTE", out)
