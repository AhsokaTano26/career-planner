# -*- coding: utf-8 -*-
"""生成 POST /tasks/{taskId}/checkin 测试用例 JSON（供 apifox test-case create 使用）。"""
import json
import os

TMP = os.environ.get("TEMP", ".")
out_path = os.path.join(TMP, "case_checkin_task.json")

data = {
    "name": "任务打卡-正向 (POST /tasks/{taskId}/checkin)",
    "type": "TEST_CASE",
    "categoryId": 12521868,
    "ordering": 12,
    "parameters": {
        "path": [
            {
                "id": "taskId#0",
                "relatedId": "taskId#0",
                "relatedName": "taskId",
                "name": "taskId",
                "value": "{{taskId}}",
                "enable": True,
            }
        ],
        "query": [],
        "cookie": [],
        "header": [
            {
                "name": "Content-Type",
                "value": "application/json",
                "enable": True,
                "id": "HDR1",
                "relatedName": "Content-Type",
            }
        ],
    },
    "commonParameters": {"path": [], "query": [], "cookie": [], "header": []},
    "requestBody": {
        "type": "application/json",
        "data": '{"doneDesc": "已完成，掌握了类与对象、集合基础。", "gains": "理解了面向对象三大特性。", "difficulties": "泛型部分较抽象。"}',
    },
    "apiDetailId": 497109144,
    "responseId": "0",
    "projectId": 8662286,
    "moduleId": 8309868,
    "preProcessors": [],
    "postProcessors": [],
    "inheritPreProcessors": {},
    "inheritPostProcessors": {},
    "auth": {},
    "advancedSettings": {"disabledSystemHeaders": {}},
    "options": {},
    "visibility": "INHERITED",
    "securityScheme": {},
    "tagIds": [],
}

with open(out_path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("WROTE", out_path)
