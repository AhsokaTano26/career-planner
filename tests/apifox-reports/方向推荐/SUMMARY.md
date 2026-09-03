# 模块 方向推荐 测试汇总

| 接口 | 方法 | 路径 | 测试用例ID | Apifox状态 | HTTP状态 |
|---|---|---|---|---|---|
| 497109120 | get | /api/v1/paths | 408828344 | 404 | 404 |
| 497109121 | get | /api/v1/directions | 408828345 | 404 | 404 |
| 497109122 | get | /api/v1/directions/{directionId} | 408828347 | 404 | 404 |
| 497109123 | get | /api/v1/directions/compare | 408828350 | 404 | 404 |
| 497109124 | get | /api/v1/students/me/favorites | 408828433 | 200 | 200 |
| 497109125 | post | /api/v1/students/me/favorites/{directionId} | 408828434 | 404 | 404 |
| 497109126 | delete | /api/v1/students/me/favorites/{directionId} | 408828438 | 200 | 200 |
| 497109127 | post | /api/v1/students/me/recommendations/runs | 408828453 | 200 | 200 |
| 497109128 | get | /api/v1/students/me/recommendations/latest | 408828455 | 200 | 200 |
| 497109129 | get | /api/v1/students/me/recommendations | 408828466 | 200 | 200 |
| 497109130 | get | /api/v1/recommendation-runs/{runId} | 408828478 | 404 | 404 |
| 497109131 | post | /api/v1/recommendation-results/{resultId}/feedback | 408828482 | 404 | 404 |