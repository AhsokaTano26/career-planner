// ============================================================
// Apifox 前置脚本（career-core 后端）：自动登录 + 注入 Authorization 头
// 适用：项目级 / 目录级 / 接口级前置脚本
// 用法：
//   1) 在 Apifox「环境管理 → 开发环境 → 环境变量」中配置：
//        BASE_URL                 = http://127.0.0.1:8080
//        LOGIN_ACCOUNT            = 2026011301      （示例：学生）
//        LOGIN_PASSWORD           = 202601
//        ACCESS_TOKEN             = （留空，脚本自动写入）
//        ACCESS_TOKEN_EXPIRES_AT  = （留空，脚本自动写入）
//   2) 把本脚本贴到「项目设置 → 前置脚本」(或目录级)。
//   3) Auth 标签可保持默认（脚本会强制覆盖 Authorization 头，更稳）。
// 说明：
//   - 不依赖 Auth 标签；脚本用 pm.request.headers.upsert 直接覆盖。
//   - 跳过登录/注册/刷新/SMS/健康检查等不需要 token 的接口。
//   - token 缺失或过期时，自动调 /auth/login 刷新，刷新过程中本次请求
//     仍带空 token（可能 401），刷新成功后下一次请求生效。
// ============================================================

const baseUrl  = pm.environment.get("BASE_URL");
const account  = pm.environment.get("LOGIN_ACCOUNT");
const password = pm.environment.get("LOGIN_PASSWORD");
const TOKEN_VAR  = "ACCESS_TOKEN";
const EXPIRE_VAR = "ACCESS_TOKEN_EXPIRES_AT";

const urlStr = (pm.request.url && pm.request.url.toString) ? pm.request.url.toString() : String(pm.request.url);

// 跳过登录/注册/刷新/SMS/健康检查等不需要 token 的接口
var skipAuth = /\/auth\/(login|register|refresh|sms|captcha)/.test(urlStr)
            || /\/health(\b|\/)/.test(urlStr)
            || /\/actuator\//.test(urlStr)
            || /\/v3\/api-docs/.test(urlStr)
            || /\/doc\.html/.test(urlStr);
if (skipAuth) {
  // 不注入 Authorization，也不自动登录
  return;
}

var tk    = pm.environment.get(TOKEN_VAR);
var expAt = parseInt(pm.environment.get(EXPIRE_VAR) || "0", 10);
var valid = tk && expAt > Date.now() + 5000;

if (valid) {
  // 复用现有 token，直接注入（同步，立刻生效）
  pm.request.headers.upsert({ key: "Authorization", value: "Bearer " + tk });
  console.log("[pre] 已注入 Authorization（缓存，未过期）expires=" + new Date(expAt).toISOString());
  return;
}

// token 缺失或过期：自动登录（异步）。本次请求仍带 token=空（可能 401），
// 但脚本会把新 token 写入环境变量，下次请求会带上。
console.log("[pre] ACCESS_TOKEN 不存在或已过期，开始自动登录...");
if (!baseUrl || !account || !password) {
  console.error("[pre] 缺少环境变量 BASE_URL / LOGIN_ACCOUNT / LOGIN_PASSWORD，请先配置环境变量");
  return;
}

pm.sendRequest({
  url: baseUrl + "/api/v1/auth/login",
  method: "POST",
  header: { "Content-Type": "application/json" },
  body: { mode: "raw", raw: JSON.stringify({ account: account, password: password }) }
}, function (err, res) {
  if (err || !res) {
    console.error("[pre] 登录请求失败:", err);
    return;
  }
  var json = res.json();
  var tok = json && json.data && (json.data.accessToken || json.data.token);
  if (!tok) {
    console.error("[pre] 登录失败，未获取到 token:", JSON.stringify(json));
    return;
  }
  pm.environment.set(TOKEN_VAR, tok);
  var exp = (json.data && json.data.expiresIn) || 7200;
  pm.environment.set(EXPIRE_VAR, String(Date.now() + (exp - 60) * 1000));
  console.log("[pre] 已自动登录并写入 ACCESS_TOKEN，expiresIn=" + exp + "s（提前 60s 判定过期）");
});
