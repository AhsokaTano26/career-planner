---
name: troubleshooting
description: 本机开发排障与避坑手册（Windows + PowerShell）。遇到 PowerShell 中文乱码/伪空格、curl JSON 请求体转义、bat 双击闪退（GBK 编码+CRLF）、SQL 含 $ 特殊字符、JVM file.encoding=GBK 种子乱码、JDK 版本冲突、服务启动失败、分支名带括号、Apifox CLI（中文乱码/uv 托管 pip/ps1 调用/test-case run 不替换 path 变量/CLI 路径）等"踩坑/报错/乱码/闪退/500/Unable to access"类问题时使用。
---

# 本机排障与避坑手册

> 原 `.opencode/instructions/pitfalls.md` 第 4/5 节（2026-08-21 拆出为技能，按需加载）。
> 环境：Windows 10 企业版 + PowerShell（pwsh）。**不要 `pip install`（uv 托管）**；**bat 必须 GBK+CRLF**。

## 一、本机常用注意事项（避坑）

- **PowerShell 编码**：PowerShell 管道会把 curl 输出的 UTF-8 按 GBK 解码，导致中文乱码/伪空格。对比接口结果请用 `curl.exe -o 文件` 存原始字节 + `Get-FileHash` 比对，或用 `Get-Content -Encoding utf8` 读取；查库用 `mysql ... -e "SELECT HEX(...)"`。
- **JSON 请求体**：PowerShell 中 `curl -d "{\"...\"}"` 的 `\"` 不会转义，会原样发送反斜杠导致服务端 JSON 解析失败（500）。请用 `--data-binary @文件`。
- **接口返回字段一般不允许为 null**：成功响应的业务对象字段尽量不返回 `null`，否则可能触发 Apifox 契约校验失败。实现约定：
  - 给 DTO/record 加 `@JsonInclude(JsonInclude.Include.NON_NULL)` 过滤空值；
  - 空态统一为默认值/空串/空数组，而非 `null`；
  - 涉及返回结构改动时对照 `docs/reports/后端对齐Apifox记录.md` 与 `docs/openapi/career-core-apis-live.yaml`。
- **分支名带括号**：PowerShell 会把 `git ... origin/career-core(back-end)` 里的 `(back-end)` 解析成命令调用，报错 `back-end\` 不是命令。引用含括号的分支名/引用必须加引号，如 `$br = 'origin/career-core(back-end)'` 或 `git log 'origin/career-core(back-end)'`。
- **终端工具会简化命令（丢掉 `cd`）**：后台长命令（如 `java -jar`、`python -m uvicorn`）可能被工具剥离开头的 `cd <目录>`，导致相对路径（如 `target\xxx.jar`）解析失败，报 `Unable to access jarfile`。启动服务请用绝对路径，或用 `--app-dir`（uvicorn）等不依赖当前目录的方式。
- **Maven 构建**：本地与 Docker 统一用 `mvnw`（wrapper 3.9.16）；若本地用 `mvn` 也需确保 Maven 3.9.x。改 pom 后先 `clean` 再 `package`，避免 target 残留旧版本 class（如 major 69）导致 `Unsupported class file major version` 报错。
- **保留字**：MySQL 8+ 中 `rank` 为保留字，SQL 需写成 `` `rank` ``。
- **外网下载（镜像 + 代理，2026-08-29）**：archive.apache.org 极慢，Maven 优先用 dlcdn.apache.org；MySQL 到 dev.mysql.com/downloads/mysql/ 下载 LTS 版本（当前 8.4.11）。⚠️ 本机 git 配了 `http.proxy=http://127.0.0.1:7897`（Clash/V2Ray），但 `HTTP_PROXY`/`HTTPS_PROXY` 环境变量是空的——`curl`/`Invoke-WebRequest`/`python urllib` 默认不走代理，直连 GitHub 超时、MySQL CDN 大文件 TLS 断连（error 35）。**所有外网下载必须显式** `-x http://127.0.0.1:7897`，或一次性持久化：`[Environment]::SetEnvironmentVariable("HTTPS_PROXY","http://127.0.0.1:7897","User")`。代理是否可达：`curl -sI -x http://127.0.0.1:7897 https://github.com` → `200 Connection established` 即通。
- **Apifox**：MCP 服务只读（无法直接写线上文档）；接口定义以 `docs/openapi/career-core-apis-live.yaml` 供导入。Apifox CLI 的详细排坑见本技能下文「二、Apifox CLI 排坑记录」。
- **无 winget / Docker**：安装软件一律手动下载 ZIP/安装包解压配置。
- **Playwright（前端页面测试）**：opencode 的 Playwright MCP（`@playwright/mcp`，已配置于 `opencode.json` 的 `mcp.playwright`）用于 AI 实时驱动浏览器逐个测试前端页面。浏览器内核已装到 `%LOCALAPPDATA%\ms-playwright`（chromium-1234 / chromium_headless_shell-1234 / ffmpeg-1011 / winldd-1007）。安装命令 `npx -y playwright@latest install chromium`；**cdn.playwright.dev 在本机不可达（下载卡 0%），必须设镜像** `$env:PLAYWRIGHT_DOWNLOAD_HOST='https://npmmirror.com/mirrors/playwright/'` 后重装。
- **前端 dev server**：`fronted/` 下 `npm run dev`（vite 6，端口 **5173**，`/api` 已代理到 8080）。后台启动可用 `Start-Process cmd.exe -ArgumentList '/c','cd /d D:\Zht20241287\career-planner\fronted && npm run dev > 日志 2>&1' -WindowStyle Hidden`。账号/密码/端口等详见 pitfalls.md §1「前端」节。
- **前端已修 bug（2026-08-21）**：
  - `App.vue` 的 `toNumber` 原为 `value.trim()?...`，但 number 类型 input 的 v-model 返回 number，保存资料时报 `value.trim is not a function`；已改为 `value==null||value===''?undefined:Number(value)`。
  - 经历类型下拉原用英文枚举 `project/internship/competition/club`，后端只接受中文 `竞赛/项目/学生工作/志愿服务`，保存经历报 400「经历类别不合法」；已统一为中文值。
- **LLM API Key（career-ai 集成用）**：本地可用的 DeepSeek key 在 `career-ai/.env`（`LLM_API_KEY=sk-...`，已 gitignore 不入库）。career-core 的 AI 模块从环境变量 `LLM_API_KEY` 读取；本地启动时可从 `career-ai/.env` 取值注入。⚠️ 不要把 key 写进任何会入库的文件。
- **bat 脚本必须 GBK 编码 + CRLF 行尾（2026-08-21 坑，`start-all.bat`/`start-backend.bat` 双击「窗口闪现即消失」）**：本机 cmd 代码页是 936（GBK），bat 文件规范是 **GBK 编码 + CRLF 行尾**。用 AI 工具（write 等）写出的 .bat 默认 UTF-8 + LF，必须转码。两个独立根因，缺一不可：
  - **根因 1：UTF-8 编码 → 中文乱码 + 吞命令**。cmd 按 GBK 逐字节解析 bat，UTF-8 中文 3 字节序列被 GBK 误读成"1.5 个汉字"，残留字节与后面的 ASCII 命令字符拼成一个怪字 → 命令字母被吞（`@echo off` 被吞成 `'R' 不是内部或外部命令`、`set "JAVA_HOME=..."` 被吞成 `'K_HOME"' 不是命令`、`echo` 被吞成 `'?echo'`），`if (...)` 括号失配 → cmd 语法错误直接退出。**解决**：`[IO.File]::WriteAllText(路径, 内容, [Text.Encoding]::GetEncoding(936))` 写为 GBK，并去掉 `chcp 65001`。
  - **根因 2：LF 行尾 → 行首命令与上一行中文合并**。即使转成 GBK，若文件是 LF-only（0 个 CRLF），cmd 期望 CRLF，遇"GBK 汉字紧接 `0x0A`"时把 LF 与上一行内容合并解析，行首 `set "JAVA_` 等命令被上一行中文"吃掉" → 仍是语法错误。**解决**：`-replace "`r?`n","`r`n"` 全量转 CRLF。
  - **诊断方法**：
    - 判断编码：扫描字节统计 `0xE4-0xE9`（UTF-8 中文首字节）vs `0xB0-0xF7`（GBK 中文首字节）数量；无 BOM 且 GBK 首字节占多数 = 已是 GBK。
    - 判断行尾：统计字节中 `0x0A` 前是 `0x0D` 的 CRLF 数 vs 单独 `0x0A` 的 LF 数。
    - 实测脚本（喂 stdin 跳过 pause）：`cmd /c "call f.bat <nul > out.log 2>&1"`，`EXIT=0` 且日志无 `不是内部或外部命令` = 解析正常。
  - **新建/修改 .bat 后必须转码**（先 write 成 UTF-8 再转）：PowerShell 一行转码：
    ```powershell
    $gbk=[Text.Encoding]::GetEncoding(936); $c=[IO.File]::ReadAllText('f.bat',[Text.Encoding]::UTF8) -replace "`r?`n","`r`n"; [IO.File]::WriteAllText('f.bat',$c,$gbk)
    ```
  - **验证**：`Get-Content f.bat -Encoding Default` 中文正常、无 `chcp 65001`；双击能正常停在 `pause`。
- **AI 写 .bat 的两条可靠路径（2026-08-29 实战）**：上文"GBK+CRLF"是根本修复；实战更简单、零风险的备选是**让 .bat 全文用纯 ASCII（英文）**——ASCII 字节在 UTF-8/ANSI 下完全一致，没有编码歧义，无需任何转码。代价是脚本提示只能是英文（系统弹窗仍是中文）。本轮 `scripts\enable-node20-admin.bat` 第一版含中文（`正在请求管理员权限...` 等），被 cmd 当 GBK 解析后**把命令字符吞成碎片**（`'20"` / `'ell.Application')'` / `'汉'` 都不是内部或外部命令），表现是满屏报错、连 VBS 提权都没跑到。**重写为纯英文版后一次通过**。
- **.bat 自提权最稳是 VBS（2026-08-29）**：`powershell -Command "Start-Process x.bat -Verb RunAs"` 在某些环境下不可靠（弹不出 UAC 或静默失败），`runas` 命令行语法容易写错（手敲 `runas /user:...` 容易弹出帮助页而非真正提权）。推荐 VBS 两行——最稳：
  ```bat
  echo Set UAC = CreateObject("Shell.Application") > "%TEMP%\getadmin.vbs"
  echo UAC.ShellExecute "%~f0", "", "", "runas", 1 >> "%TEMP%\getadmin.vbs"
  cscript //nologo "%TEMP%\getadmin.vbs"
  ```
  检测管理员：`net session >nul 2>&1`（errorlevel=0 = 已是管理员；非 0 则走上面的 VBS 重启自己）。
- **PowerShell 双引号字符串会把 `$` 当变量插值（2026-08-21 坑，重置密码哈希被写坏）**：在 PowerShell 命令里内嵌含 `$` 的 SQL（如 bcrypt 哈希 `$2a$10$...`）时，`$2a`、`$AEX...` 会被当作变量求值为空，哈希被破坏。**解决**：不要在命令字符串里内嵌含 `$` 的数据，改用 write 工具写 `.sql` 文件（UTF-8，`$` 原样保留），再用 mysql 客户端从文件执行。恢复密码哈希可用 `jshell` + `spring-security-crypto` 的 `BCrypt.hashpw("密码", BCrypt.gensalt(10))` 生成（`BCrypt.checkpw` 验证旧哈希），jar 路径 `C:\Users\uio8k\.m2\repository\org\springframework\security\spring-security-crypto\6.3.3\spring-security-crypto-6.3.3.jar`（勿用 `BCryptPasswordEncoder`，它缺 commons-logging 依赖会 `NoClassDefFoundError`）。
- **改库/执行 SQL 一律走 SQL 文件 + cmd 重定向（2026-08-21 经验）**：含特殊字符（`$`、反斜杠、中文）的 SQL 不要拼在 `-e "..."` 命令行里（PowerShell 插值 + 引号转义双重坑）。可靠做法：用 write 工具写 SQL 文件 → `cmd /c "mysql.exe -uroot --default-character-set=utf8mb4 career_core < 文件.sql"`（用 cmd 包装重定向比 PowerShell 的 `<` 更稳）。同理，含特殊字符的 JSON 请求体用 `curl.exe --data-binary @文件`。
- **JVM file.encoding=GBK 导致 spring.sql.init 把 UTF-8 种子脚本按 GBK 解码入库（2026-08-21 坑，生涯测评/路径探索中文乱码）**：本机 JDK 17 默认 `file.encoding=GBK`（`jcmd <pid> VM.system_properties` 可查），而 `spring.sql.init` 读取 SQL 脚本文件时若未指定编码会用平台默认字符集 → 把 UTF-8 的 `seed-questionnaires.sql`/`seed-directions.sql` 按 GBK 解码，中文被双重重编码成乱码（「能力自评」→ `鑳藉姏鑷瘎`，HEX `E9 91 B3...`）写进 utf8mb4 库。特征：**同表 admin 通过 JDBC(UTF-8) 建的行正常、种子行乱码**。**根因修复**：`application.yml` 加 `spring.sql.init.encoding: UTF-8`（已加注释），保证任何 JVM 默认字符集都按 UTF-8 读种子。**存量修复**：种子用 `INSERT IGNORE` 不覆盖旧行，需手动 UPDATE 被污染行（思路见临时目录 `fix_mojibake.sql`：questionnaire/question/question_option/career_direction/student_goal 按 seed 文件正确值回写）。排查用 `HEX()` 比对「表内字节 vs seed 文件 UTF-8 字节」。

## 二、Apifox CLI 排坑记录

> 原 `docs/Apifox-CLI与开发环境排坑记录.md`（2026-08-15 整理，2026-08-21 合并入指令，2026-08-21 拆入本技能）。

### 2.1 PowerShell 中文乱码问题（最高频）

**现象**：PowerShell 管道会把命令输出的 UTF-8 字节按 GBK 解码，导致中文变成乱码 / 伪空格。例如 `apifox endpoint list --project 8662286 | ConvertFrom-Json` 解析失败，报 `Unexpected character was encountered`。

**根因**：PowerShell 控制台默认编码是 GBK（代码页 936），而 CLI 输出的是 UTF-8，两者不一致。

**解决方式**（三种，按推荐度排序）：
1. **cmd 重定向存原始字节**（最稳妥，推荐）：
   ```powershell
   cmd /c "apifox endpoint list --project 8662286 > %TEMP%\apifox_endpoints.json"
   $j = Get-Content "$env:TEMP\apifox_endpoints.json" -Raw | ConvertFrom-Json
   ```
   `cmd /c` 的重定向不做编码转换，文件保留原始 UTF-8 字节。
2. **Python 脚本里用 subprocess 捕获原始字节**（写脚本时最稳）：
   ```python
   proc = subprocess.run(cmd, capture_output=True)
   text = proc.stdout.decode("utf-8-sig")   # 兼容 BOM
   obj = json.loads(text)
   ```
3. **存文件 + 显式指定 UTF-8 读取**（对比/核对场景）：
   ```powershell
   curl.exe -o file.json "http://..."
   Get-Content -Encoding utf8 file.json
   ```
   查库校验中文用 `mysql ... -e "SELECT HEX(...)"`。

**避坑要点**：凡是把 CLI 输出接进 PowerShell 变量/管道再做文本处理的，一律先落盘原始字节，再读取。

### 2.2 Python 解释器被 uv 托管，`pip install` 不可用

**现象**：工作区 Python 是 uv 托管的（`C:\Users\uio8k\AppData\Roaming\uv\python\cpython-3.14-...`），直接 `pip install pyyaml` 报 `error: externally-managed-environment ... managed by uv`。

**根因**：uv 管理的解释器受 PEP 668 保护，不允许直接修改。

**解决方式**：
1. **优先写不依赖第三方库的脚本**（推荐）：需要解析 JSON → 用标准库 `json`；需要调 CLI → 用标准库 `subprocess`。本次 `organize_apifox_apis.py` 即全程只用标准库。
2. **临时注入依赖**（非交互有 `uv` 时）：`uv run --with pyyaml python script.py`；⚠️ 本机 `uv` 不在 PATH 时此命令不可用。
3. **务必用正确解释器路径运行**：终端 `python` 可能与 IDE 选中的解释器不是同一个。本机实际可执行路径为 `C:/Users/uio8k/.local/bin/python.exe`：
   ```powershell
   & "C:/Users/uio8k/.local/bin/python.exe" script.py
   ```

**避坑要点**：不要对 uv 托管解释器执行 `pip install`；脚本优先纯标准库实现。

### 2.3 PowerShell 字符串插值转义错误导致输出异常

**现象**：在 PowerShell 里用双引号字符串拼接 Markdown 时，想输出反引号（Markdown 行内代码 `` ` ``），用了反引号转义 `$`，结果 `$($ep.method.ToUpper())` 没有被求值，而是把整个对象字面量（`@{id=...; name=...}`）拼进了文本，生成文档全是乱码表达式。

**根因**：PowerShell 中 `` ` `` 是转义字符，`` `$ `` 表示字面 `$`；在双引号字符串里混用 `` `$ `` 和 `` $() `` 子表达式时极易出错——`$` 被转义成字面量后，后面的表达式不再按插值求值，或把对象整体序列化成 `@{key=value}` 字符串。

**解决方式**：
1. **不要在 PowerShell 字符串插值里混用代码标记**，先算成变量再拼：
   ```powershell
   $m = $ep.method.ToUpper()
   $p = $ep.path
   $sb.AppendLine("| $i | `` $m `` | `` $p `` | $($ep.name) |")
   ```
2. **更彻底：生成结构化文档用 Python 脚本**（推荐），PowerShell 只负责抓数据：
   ```python
   lines.append(f"| {i} | `{e['method'].upper()}` | `{e['path']}` | {e['name']} |")
   ```
   Python 的 f-string 对反引号无特殊语义，Markdown 拼接更干净。

**避坑要点**：PowerShell 拼接含 Markdown 反引号/大括号的内容时，优先改用 Python f-string；实在要用 PowerShell，先把值算好再插值。

### 2.4 Apifox CLI 的完整路径

**现象**：终端里 `apifox` 能直接用，但 Python `subprocess.run(["apifox", ...])` 报 `FileNotFoundError`（WinError 2）。

**根因**：`apifox` 是通过 npm 全局安装的，`Get-Command apifox` 解析到的是 `.ps1` 包装脚本，其真实可执行文件在 npm 目录下。

**本机完整路径**：
```text
# 命令包装脚本
C:\Users\uio8k\AppData\Roaming\npm\apifox.ps1
C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd   # Python 应调用这个
```

**定位方法**：
```powershell
Get-Command apifox | Select-Object Source     # 找到 .ps1/.cmd 所在目录
Test-Path "C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"   # 确认 .cmd 存在
```

**避坑要点**：给 CLI 定位时，同时看 `.ps1` 与 `.cmd` 两个包装脚本；它们位于同一 npm 目录。

### 2.5 apifox 是 npm 的 `.ps1` 包装脚本，Python 无法直接调用 → 用 `apifox.cmd`

**现象**：Python `subprocess.run` 直接执行 `apifox` 失败（见 2.4）。

**根因**：
- PowerShell 里 `apifox` 实际执行的是 `apifox.ps1`（PowerShell 脚本），只有 PowerShell 解释器能跑；
- Python 的 `subprocess` 用 Windows `CreateProcess` 启动进程，无法执行 `.ps1`；
- npm 同时生成了 `apifox.cmd`（批处理包装），可由任何进程通过标准 `CreateProcess` 调用。

**解决方式**：在 Python 脚本中显式写 `.cmd` 完整路径：
```python
APIFOX_CMD = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"
cmd = [APIFOX_CMD, *args, "--project", PROJECT_ID]
proc = subprocess.run(cmd, capture_output=True)
```

**避坑要点**：任何语言/工具想跨进程调用 npm 全局 CLI，优先用 `<name>.cmd` 而非 `<name>.ps1`。

### 2.6 将脚本改为调用 `apifox.cmd` 完整路径（示例）

修复后的可复跑脚本片段（`docs/scripts/organize_apifox_apis.py`）：
```python
PROJECT_ID = "8662286"
APIFOX_CMD = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"

def run_apifox(args: list) -> dict:
    """调用 apifox 命令并解析 stdout 为 JSON（按 UTF-8 解码原始字节）"""
    cmd = [APIFOX_CMD, *args, "--project", PROJECT_ID]
    proc = subprocess.run(cmd, capture_output=True)
    text = proc.stdout.decode("utf-8-sig")   # 解决中文乱码（见 2.1）
    obj = json.loads(text)
    if not obj.get("success", True):
        raise RuntimeError(f"apifox 命令失败: {obj}")
    return obj
```

**完整脚本**：`docs/scripts/organize_apifox_apis.py`
- 功能：`endpoint list` + `folder list` → 按目录分组生成接口清单 Markdown。
- 运行（用 uv 托管解释器的绝对路径）：
  ```powershell
  & "C:/Users/uio8k/.local/bin/python.exe" "D:\Zht20241287\career-planner\docs\scripts\organize_apifox_apis.py"
  ```
- 输出：`docs/openapi/career-core-apis-live-summary.md`

### 2.7 JDK 版本冲突（构建/运行 JDK 必须与 pom 一致 = Java 17）

**现象**：`java -jar target\career-core-0.0.1-SNAPSHOT.jar` 启动后端失败，报：
```
UnsupportedClassVersionError: ... has been compiled by a more recent version
of the Java Runtime (class file version 69.0), this version ... only recognizes
class file versions up to 65.0
```

**根因**：pom 要求 Java 17（class file version 61.0），若 jar 用更高版本 JDK 编译（如 21 → 65.0、25 → 69.0），而运行端 JDK 更旧，或运行端 JDK 新但 class 旧（不影响）——关键是**构建与运行的 JDK 必须与 pom（17）一致**。且新开的终端默认 `java` 可能是 JDK 21（只识别到 65.0），`JAVA_HOME` User 级变量新会话可能读不到。

**解决方式**（任选其一）：
1. **直接用 JDK 17 绝对路径启动**（最稳，不依赖 PATH）：
   ```powershell
   & "D:\devtools\jdk17\jdk-17.0.20+8\bin\java.exe" -jar "D:\Zht20241287\career-planner\career-core\target\career-core-0.0.1-SNAPSHOT.jar"
   ```
2. **修正当前会话 JAVA_HOME / PATH 后再构建与启动**：
   ```powershell
   $env:JAVA_HOME='D:\devtools\jdk17\jdk-17.0.20+8'
   $env:Path='D:\devtools\jdk17\jdk-17.0.20+8\bin;D:\devtools\maven\apache-maven-3.9.16\bin;'+$env:Path
   .\mvnw.cmd clean package -DskipTests
   & "D:\devtools\jdk17\jdk-17.0.20+8\bin\java.exe" -jar target\career-core-0.0.1-SNAPSHOT.jar
   ```

**避坑要点**：路径与 class file version 对照见 pitfalls.md §1；本地 jar 以 Java 17 编译（major 61），**用 JDK 17/21/25 均可运行**（17 为 Docker 运行时），但**编译必须用 JDK 17** 以保证与 Docker 产物一致。

### 2.8 本地服务启动命令速记（Apifox 调试前置）

> Apifox 桌面端测试报 `connect ECONNREFUSED 127.0.0.1:8080` = 后端没启动；先启动 MySQL 再启动后端。

```powershell
# 1) 启动 MySQL（3306；mysqld 后台终端不要关）
& D:\devtools\mysql\mysql-8.4.11-winx64\bin\mysqld.exe --defaults-file=D:\devtools\mysql\my84.ini --console

# 2) 启动后端（8080；JDK 17 绝对路径 + jar 绝对路径，避免丢 cd / 版本不匹配）
& "D:\devtools\jdk17\jdk-17.0.20+8\bin\java.exe" -jar "D:\Zht20241287\career-planner\career-core\target\career-core-0.0.1-SNAPSHOT.jar"

# 3) 验证端口与接口
Test-NetConnection 127.0.0.1 -Port 8080 -WarningAction SilentlyContinue | Select TcpTestSucceeded
Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/v1/students/me/profile/latest?studentId=1001" -UseBasicParsing | Select StatusCode
```

**排障顺序**：① 3306 是否监听（MySQL 没起）→ ② 8080 是否监听（后端没起）→ ③ 后端启动日志（JDK 版本 / 端口占用 / 数据库连接）。

### 2.9 `test-case run` 不替换 path 变量（2026-08-15）

> CLI 2.2.9 直接跑单接口测试用例时，URL 中 `{taskId}` 不会被替换成实际值，导致请求打到字面路径返回 400。这是 CLI 行为，不是接口问题。

**现象**：`apifox test-case run <id> -e <envId>` 输出的请求 URL 仍是 `PATCH http://127.0.0.1:8080/api/v1/tasks/{taskId}`（字面量），返回 400；JSON 报告里 collection 的 `url.variable` 为空、`url.path` 仍含 `{taskId}`。`--env-var` / `--global-var` / `--variables` 注入均不生效（报告 globals/environment 为空）。

**可靠跑法**（基于线上测试用例，绕开 CLI 缺陷）：
```bash
# 1) 生成 JSON 报告，拿到 CLI 自己构造的 collection 段
apifox test-case run <id> --project 8662286 -e <envId> -r json --out-dir <dir> --out-file <name>

# 2) 从报告 JSON 提取 collection 段另存为 collection.json

# 3) 用 Python 把 collection 中 url.path 的 {taskId} 替换为实际值，url.variable 置空
#    参考脚本：docs/scripts/fix_collection_task.py

# 4) 用 apifox run 执行修正后的 collection
apifox run <collection.json> -r cli,json
```

**实测**：`PATCH /api/v1/tasks/T163` → 200 OK（481B，完整 Task 对象）。参考用例「更新任务-正向 (PATCH /tasks/{taskId})」id=404389855（endpoint 497109142），本地环境 47907998；PATCH body 用契约 `TaskStatusUpdate`（`status` 必填）。

### 2.10 汇总速查表

| 问题 | 一句话解决 |
|------|-----------|
| CLI 输出中文乱码 | `cmd /c "... > file"` 落盘原始字节，或 Python subprocess 捕获后 `decode("utf-8-sig")` |
| `pip install` 报 externally-managed | 脚本只用标准库，别装第三方包；或 `uv run --with <pkg> python ...` |
| 终端 python 与 IDE 不一致 | 用解释器绝对路径运行：`& "C:/Users/uio8k/.local/bin/python.exe" script.py` |
| PowerShell 拼 Markdown 转义混乱 | 改用 Python f-string 生成文档 |
| Python 调不到 apifox | 用 `.cmd` 完整路径，不用 `.ps1` |
| apifox 真实路径 | `C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd` |
| 后端启动报 UnsupportedClassVersionError | 用 JDK 17 绝对路径启动：`& "D:\devtools\jdk17\jdk-17.0.20+8\bin\java.exe" -jar ...` |
| Apifox 报 ECONNREFUSED 8080 | 后端没启动：先起 MySQL（3306）再起后端（8080），见 2.8 |
| `test-case run` URL 仍是 `{taskId}` → 400 | CLI 不替换 path 变量：报告取 collection 段 → 替换 path → `apifox run` 执行，见 2.9 |