# 生涯规划系统 · 核心业务服务（career-core）

第一版 Demo 后端，实现 5 个接口（接口路径与主结构按线上 Apifox 为准），目标「最小可用、跑通流程」。
技术栈：**Spring Boot 3.5 + Java 21 + MySQL + JdbcTemplate**（贴合正式架构 Spring Boot/MySQL 8）。

> 说明：线上 Apifox 无“提交阶段复盘”接口，故复盘接口已按线上为准删除；本 Demo 保留「画像」「推荐」「计划」模块。

## 一、接口总览（以线上 Apifox 为准）

| 接口 | 方法 | 路径 |
|------|------|------|
| 1. 查看最新画像 | GET | `/api/v1/profiles/latest?studentId={studentId}` |
| 2. 生成推荐 | POST | `/api/v1/recommendations/run?studentId={studentId}` |
| 3. 查询推荐结果 | GET | `/api/v1/recommendations/latest?studentId={studentId}` |
| 4. 推荐反馈 | POST | `/api/v1/recommendations/{id}/feedback` |
| 5. AI生成计划草案 | POST | `/api/v1/planning/plans/generate?studentId={studentId}` |

所有接口统一返回 `{ code, message, data }`，`code=0` 表示成功。
> 线上接口无 studentId 入参（依赖登录态）；Demo 无鉴权，故 studentId 为可选增强参数，缺省取 1001。

## 二、环境依赖

- JDK 21（本机：`D:\devtools\jdk21\jdk-21.0.12+8`）
- Maven 3.9+（本机：`D:\devtools\maven\apache-maven-3.9.16`）
- MySQL（本机：`D:\devtools\mysql\mysql-26.7.0-winx64`，端口 3306）

## 三、数据库

- 数据库名：`career_core`
- 账号：`career`（可经 `$env:DB_USERNAME` 覆盖）/ 密码经 `$env:DB_PASSWORD` 注入（缺省 `career123` 供本地直启，不硬编码入库）
- 建表与种子数据脚本：`src/main/resources/db/schema.sql`、`data.sql`
  - 应用启动时通过 `spring.sql.init` 自动幂等执行（可重复启动）
  - 也可手动执行（连接参数经环境变量注入，缺省供本地直启）：
    ```powershell
    $env:DB_USERNAME ??= 'career'
    $env:DB_PASSWORD ??= 'career123'
    mysql -u"$env:DB_USERNAME" -p"$env:DB_PASSWORD" --default-character-set=utf8mb4 career_core < schema.sql
    mysql -u"$env:DB_USERNAME" -p"$env:DB_PASSWORD" --default-character-set=utf8mb4 career_core < data.sql
    ```
- 种子数据：学生 1001（完整画像）、1002（无画像快照）、9 个方向（1 个 INACTIVE 用于验证规则过滤）

## 四、构建与运行

```powershell
$env:JAVA_HOME='D:\devtools\jdk21\jdk-21.0.12+8'
cd career-core
mvn -DskipTests package
java -jar target\career-core-0.0.1-SNAPSHOT.jar
```

启动后访问 `http://127.0.0.1:8080`。

## 五、接口示例与验收

### 1. 查看最新画像
```powershell
curl "http://127.0.0.1:8080/api/v1/profiles/latest?studentId=1001"
# 200，data = { dimensions(六维), summary, version } + 增强字段 completeness/studentId/experiences
curl "http://127.0.0.1:8080/api/v1/profiles/latest?studentId=9999"
# 无效ID / 画像未生成 → 200，data 为空对象 {}
```

### 2. 生成推荐
```powershell
curl -X POST "http://127.0.0.1:8080/api/v1/recommendations/run?studentId=1001"
# 200，data 为空对象 {}（线上定义）；触发一次计算并落库 recommendation_run/result
```

### 3. 查询推荐结果
```powershell
curl "http://127.0.0.1:8080/api/v1/recommendations/latest?studentId=1001"
# 200，data = { results: [ {directionId, name, type, score, rank, confidence, reason} ] }
# 线上字段 directionId/score/rank/confidence + 增强字段 name/type/reason；多次调用结果一致
```

### 4. 推荐反馈
```powershell
curl -X POST -H "Content-Type: application/json" --data-binary @scripts\feedback.json "http://127.0.0.1:8080/api/v1/recommendations/{id}/feedback"
# 200，data 为空对象 {}（线上定义）；写入 recommendation_feedback
```

### 5. AI生成计划草案（调用推荐数据）
```powershell
curl -X POST "http://127.0.0.1:8080/api/v1/planning/plans/generate?studentId=1001"
# 200，data = { studentId, direction(推荐第一名), goal, semester, status=DRAFT, tasks[...] }
# 数据来源：调用 /api/v1/recommendations/latest 的推荐结果；落库 student_goal/semester_plan/plan_task
```

## 六、Demo 精简点（后续迭代替换位置）

| 位置 | Demo 处理 | 后续迭代 |
|------|-----------|----------|
| `StudentProfileService` | 直接查库、无缓存 | 可加 Redis 缓存 |
| `StudentProfileService` | 完整度 = 非空字段数/总字段数 | 可换加权完整度算法 |
| `RecommendationEngine` | 规则过滤仅按状态、无 penalty | 叠加路径/专业/必要条件过滤与惩罚项 |
| `RecommendationEngine.buildReason` | 预置模板拼接 | 替换为调用 career-ai FastAPI 大模型生成解释 |
| 数据访问 | JdbcTemplate | 可替换 MyBatis/MyBatis-Plus |
