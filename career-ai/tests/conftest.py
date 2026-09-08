"""pytest 全局前置（稳定性 2026-09）：AI 路由要求内部鉴权，测试默认自动带头。

- GATEWAY_API_KEY 缺省为 gw-test-key（load_dotenv 不覆盖已存在环境变量，隔离 CI/本地 .env）；
- TestClient.request 自动补 Authorization（显式传了则不覆盖，401 用例可传错 key）。
"""

import os

os.environ.setdefault("GATEWAY_API_KEY", "gw-test-key")

from starlette.testclient import TestClient as _TestClient  # noqa: E402

_orig_request = _TestClient.request


def _request_with_key(self, method, url, **kwargs):
    headers = dict(kwargs.pop("headers", None) or {})
    headers.setdefault("Authorization", "Bearer " + os.getenv("GATEWAY_API_KEY", ""))
    kwargs["headers"] = headers
    return _orig_request(self, method, url, **kwargs)


_TestClient.request = _request_with_key
