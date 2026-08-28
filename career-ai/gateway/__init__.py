"""AI 网关包：基于 LiteLLM 的统一大模型出口。

职责（对应《AI 网关计划 v2》）：
    config          环境变量 → 渠道/模型组/限流配置
    ratelimit       进程内令牌桶限流（rpm）
    db              ai_call_log 落库（仅写日志表，失败不阻断主调用）
    logging_callback LiteLLM CustomLogger 回调 → ai_call_log
    client          统一 generate() 入口：Router 多渠道负载均衡 + fallback + 重试

Demo 边界：网关自身不做密钥/额度管理（未选范围）；后续迭代替换位置见各文件标注。
"""
