// ============================================================
// Apifox 前置脚本（career-ai / 8000 环境）：注入网关密钥 Bearer
// 用法：
//   1) 新建 Apifox 环境（如「career-ai(8000)」），配置环境变量：
//        BASE_URL_AI   = http://127.0.0.1:8000
//        GATEWAY_API_KEY = （复制自 career-ai/.env 的 GATEWAY_API_KEY 真实值，勿入仓）
//   2) 把本脚本粘贴到该环境所属「项目/目录/接口」的前置脚本。
//   3) 调用时自动带上 Authorization: Bearer <GATEWAY_API_KEY>。
// 说明：
//   - 仅网关类接口需要该密钥：/v1/chat/completions、/api/v1/gateway/generate
//     （routes_gateway.py 的 _require_gateway_key 校验 Bearer）。
//   - /api/v1/ai/* 等 AI 接口由服务内部持密钥调用大模型，不校验请求方的该头，带上也无副作用。
//   - career-ai 无用户登录态，因此不需要 accessToken / 登录前置逻辑。
// ============================================================

const key = pm.environment.get("GATEWAY_API_KEY");
if (key) {
  pm.request.headers.remove("Authorization");
  pm.request.headers.add({ key: "Authorization", value: "Bearer " + key });
}
