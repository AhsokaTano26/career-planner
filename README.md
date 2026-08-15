# 生涯规划系统·部署手册


## 一、前置依赖（本机已装）

- **JDK 25**：`D:\devtools\jdk25\jdk-25.0.2`（必须用 25，否则启动报 `class file version 69.0`）
- **Maven 3.9+**：`D:\devtools\maven\apache-maven-3.9.16`
- **MySQL**：`D:\devtools\mysql\mysql-26.7.0-winx64`
- **Python 3**：供 career-ai 与冒烟脚本使用

## 二、部署步骤（3 步）

### 第 1 步：启动 MySQL（:3306）

```powershell
& D:\devtools\mysql\mysql-26.7.0-winx64\bin\mysqld.exe --defaults-file=D:\devtools\mysql\my.ini --console
```

确认监听正常：`Test-NetConnection 127.0.0.1 -Port 3306`

> 建表与种子数据由 career-core 启动时自动执行（幂等），**无需手动建表**。

### 第 2 步：启动后端 career-core（:8080）

```powershell
$env:JAVA_HOME='D:\devtools\jdk25\jdk-25.0.2'
$env:Path='D:\devtools\jdk25\jdk-25.0.2\bin;D:\devtools\maven\apache-maven-3.9.16\bin;'+$env:Path
cd D:\Zht20241287\career-planner\career-core
mvn -DskipTests package
& "D:\devtools\jdk25\jdk-25.0.2\bin\java.exe" -jar "D:\Zht20241287\career-planner\career-core\target\career-core-0.0.1-SNAPSHOT.jar"
```

- 数据库账号缺省 `career` / `career123`，可用环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖
- 启动成功标志：日志出现 `Started CareerCoreApplication`

### 第 3 步：启动 AI 服务 career-ai（:8000）

```powershell
cd D:\Zht20241287\career-planner\career-ai
# 首次：准备虚拟环境与依赖
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
# 首次：复制 .env.example 为 .env 并填入大模型 Key
#   LLM_API_KEY=<你的Key>  LLM_BASE_URL=<模型地址>  LLM_MODEL=deepseek-v4-pro
# 启动
.\.venv\Scripts\python.exe -m uvicorn api.main:app --host 127.0.0.1 --port 8000
```

## 三、其他文档

- 详细文档：`docs/使用手册.md`（完整安装/排坑）、`docs/接口设计.md`、`docs/openapi/career-core-apis.yaml`
- `deploy/docker-compose.yml` 为 Docker 部署**最小骨架**（尚未提供 Dockerfile，暂不可用）；当前按本手册本机部署

## 四、Apifox使用

- 部署完成后，Apifox中选择“开发环境”就可以开始调试