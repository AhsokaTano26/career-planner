# -*- coding: utf-8 -*-
"""生成 DELETE /tasks/{taskId} 测试用例 JSON（供 apifox test-case create 使用）。"""
import json
import os

TMP = os.environ.get("TEMP", ".")
out_path = os.path.join(TMP, "case_delete_task.json")

data = {
    "name": "删除任务-正向 (DELETE /tasks/{taskId})",
    "type": "TEST_CASE",
    "categoryId": 12521868,
    "ordering": 11,
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
    "apiDetailId": 497109143,
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
