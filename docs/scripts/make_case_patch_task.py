# -*- coding: utf-8 -*-
"""生成 PATCH /tasks/{taskId} 测试用例 JSON（供 apifox test-case create 使用）。"""
import json
import os

TMP = os.environ.get("TEMP", ".")
out_path = os.path.join(TMP, "case_patch_task.json")

data = {
    "name": "更新任务-正向 (PATCH /tasks/{taskId})",
    "type": "TEST_CASE",
    "categoryId": 12521868,
    "ordering": 10,
    "parameters": {
        "path": [
            {
                "id": "taskId#0",
                "relatedId": "taskId#0",
                "relatedName": "taskId",
                "name": "taskId",
                "value": "T163",
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
        "data": '{"status": "DELAYED", "reason": "与课程时间冲突", "note": "推迟到 11 月中旬"}',
    },
    "apiDetailId": 497109142,
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
