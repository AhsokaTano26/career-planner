# 模块 阶段复盘 测试汇总

| 接口 | 方法 | 路径 | 测试用例ID | Apifox状态 | HTTP状态 |
|---|---|---|---|---|---|
| 497109146 | get | /api/v1/reviews | 408828631 | 200 | 200 |
| 497109147 | post | /api/v1/reviews/drafts | 408828634 | 200 | 200 |
| 497109148 | get | /api/v1/reviews/{reviewId} | 408828637 | 200 | 200 |
| 497109149 | put | /api/v1/reviews/{reviewId}/draft | 408828638 | 400 | 400 |
| 497109150 | post | /api/v1/reviews/{reviewId}/submit | 408828641 | 200 | 200 |
| 497109151 | post | /api/v1/reviews/{reviewId}/ai-summary | 408828642 | 200 | 200 |
| 497109152 | post | /api/v1/reviews/{reviewId}/guidance-request | 408828655 | 200 | 200 |
| 497109153 | post | /api/v1/reviews/{reviewId}/adopt-advice | 408828657 | 200 | 200 |