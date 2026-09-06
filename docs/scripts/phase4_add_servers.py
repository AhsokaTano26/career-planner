"""
Phase 4: 给 OAS 补全 servers 字段（之前是空数组）
- 线上生产: https://career-planner.tano.asia
- 本地开发: http://localhost:8080
- 通用: /
"""
import json
from pathlib import Path

YAML_PATH = Path(r"D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live.yaml")

with open(YAML_PATH, "r", encoding="utf-8") as f:
    doc = json.load(f)

doc["servers"] = [
    {
        "url": "https://career-planner.tano.asia",
        "description": "线上生产环境"
    },
    {
        "url": "http://localhost:8080",
        "description": "本地开发环境"
    }
]

# 写回
out_bytes = json.dumps(doc, ensure_ascii=False, indent=2).encode("utf-8")
out_bytes = out_bytes.replace(b"\n", b"\r\n")
with open(YAML_PATH, "wb") as f:
    f.write(out_bytes)

print(f"OK: servers updated, new size: {len(out_bytes)} bytes")
print(f"servers: {doc['servers']}")
