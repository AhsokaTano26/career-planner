# 标准工作流（改后端的固定闭环）

1. **动手前先探测状态**：确认 8080（career-core）与 3306（MySQL）是否在跑、是否需要重启。
2. **修改 → 构建 → 重启**：改完执行 `.\mvnw.cmd clean package -DskipTests`（在 `career-core` 下，JAVA_HOME 指向 JDK 21），停旧 java 进程后用 JDK 21/25 绝对路径启动 jar。
3. **验证**：用 `python tests\smoke_api.py` 或 curl 冒烟，报告每个接口的 HTTP 状态与结果；涉及表结构时先看 `db/schema.sql`（启动幂等执行）。
4. **异常定位**：先看应用日志（后台终端输出），再回查代码；PowerShell 中文乱码时用 `curl.exe -o 文件` 存原始字节 + `Get-Content -Encoding utf8` / `Get-FileHash` 比对。