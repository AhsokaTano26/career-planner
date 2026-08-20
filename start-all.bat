@echo off
rem ==================================================================
rem  career-planner 一键部署脚本
rem  依次启动：MySQL(3306) -> career-core(8080) -> career-ai(8000)
rem  每个服务在独立窗口中运行，关闭窗口即停止对应服务
rem ==================================================================
chcp 65001 >nul
setlocal

set "PROJECT=D:\Zht20241287\career-planner"
set "JDK_HOME=D:\devtools\jdk17\jdk-17.0.20+8"
set "MAVEN_HOME=D:\devtools\maven\apache-maven-3.9.16"
set "MYSQL_BIN=D:\devtools\mysql\mysql-26.7.0-winx64\bin"
set "MYSQL_INI=D:\devtools\mysql\my.ini"

echo.
echo ============================================================
echo    career-planner 一键部署
echo ============================================================
echo 项目目录 : %PROJECT%
echo.

rem ---------------- 第 1 步：MySQL ----------------
echo [1/3] 检查 MySQL（:3306）...
netstat -ano 2>nul | findstr /C:":3306" >nul
if not errorlevel 1 (
    echo        MySQL 已在运行，跳过启动。
    goto mysql_ready
)
echo        MySQL 未运行，正在启动...
start "MySQL" "%MYSQL_BIN%\mysqld.exe" --defaults-file="%MYSQL_INI%" --console
set /a wait=0
:wait_mysql
netstat -ano 2>nul | findstr /C:":3306" >nul
if not errorlevel 1 (
    echo        MySQL 启动成功（:3306）。
    goto mysql_ready
)
set /a wait+=1
if %wait% GEQ 30 (
    echo        [警告] 30 秒内未检测到 3306，请查看 MySQL 窗口中的报错。
    goto mysql_ready
)
timeout /t 1 /nobreak >nul
goto wait_mysql
:mysql_ready

rem ---------------- 第 2 步：career-core ----------------
echo.
echo [2/3] 构建并启动后端 career-core（:8080）...
rem 若 8080 被占用，先终止旧进程（同时避免旧进程锁住 jar 导致构建失败）
call :kill_port 8080
set "JAVA_HOME=%JDK_HOME%"
set "PATH=%JDK_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
cd /d "%PROJECT%\career-core"
call mvn -DskipTests package
if errorlevel 1 (
    echo        [错误] Maven 构建失败，请查看上方错误后重试。
    pause
    exit /b 1
)
echo        构建完成，正在启动后端服务（新窗口）...
start "career-core" "%JDK_HOME%\bin\java.exe" -jar "%PROJECT%\career-core\target\career-core-0.0.1-SNAPSHOT.jar"

rem ---------------- 第 3 步：career-ai ----------------
echo.
echo [3/3] 配置并启动 AI 服务 career-ai（:8000）...
cd /d "%PROJECT%\career-ai"
if not exist ".venv\Scripts\python.exe" (
    echo        首次运行：创建虚拟环境 .venv ...
    python -m venv .venv
    if errorlevel 1 (
        echo        [错误] 创建虚拟环境失败，请确认 Python 已安装且位于 PATH。
        pause
        exit /b 1
    )
    echo        安装依赖（requirements.txt）...
    ".venv\Scripts\python.exe" -m pip install -r requirements.txt
    if errorlevel 1 (
        echo        [错误] 依赖安装失败，请查看上方错误。
        pause
        exit /b 1
    )
)
if not exist ".env" (
    echo        [提示] 未找到 .env，正在生成模板...
    >".env" echo # career-ai 环境变量
    >>".env" echo LLM_API_KEY=
    >>".env" echo LLM_BASE_URL=https://api.deepseek.com
    >>".env" echo LLM_MODEL=deepseek-v4-flash
    echo        [提示] 请打开 %PROJECT%\career-ai\.env 填入 LLM_API_KEY 后重新运行本脚本。
)
echo        启动 AI 服务（新窗口）...
rem 若 8000 被占用，先终止旧进程
call :kill_port 8000
start "career-ai" "%PROJECT%\career-ai\.venv\Scripts\python.exe" -m uvicorn api.main:app --host 127.0.0.1 --port 8000

echo.
echo ============================================================
echo    部署完成，服务窗口已打开：
echo      MySQL       :3306
echo      career-core :8080    http://127.0.0.1:8080
echo      career-ai   :8000    http://127.0.0.1:8000/health
echo.
echo    冒烟验证：cd /d "%PROJECT%" ^&^& python tests\smoke_api.py
echo ============================================================
pause
exit /b 0

rem ============================================================
rem  辅助子程序：若端口被占用，终止监听进程并等待其释放
rem  用法：call :kill_port <端口号>
rem ============================================================
:kill_port
set "PORT=%~1"
echo       检查端口 %PORT% 占用...
netstat -ano 2>nul | findstr /C:":%PORT% " | findstr /C:"LISTENING" >nul
if errorlevel 1 (
    echo       端口 %PORT% 未被占用，无需终止。
    exit /b 0
)
for /f "tokens=5" %%p in ('netstat -ano 2^>nul ^| findstr /C:":%PORT% " ^| findstr /C:"LISTENING"') do (
    echo       端口 %PORT% 被进程 %%p 占用，正在终止...
    taskkill /F /PID %%p >nul 2>&1
)
set /a kwait=0
:wait_port_free
netstat -ano 2>nul | findstr /C:":%PORT% " | findstr /C:"LISTENING" >nul
if errorlevel 1 (
    echo       端口 %PORT% 已释放。
    exit /b 0
)
set /a kwait+=1
if %kwait% GEQ 5 (
    echo       [警告] 端口 %PORT% 5 秒内未释放，请手动检查（脚本将继续）。
    exit /b 0
)
timeout /t 1 /nobreak >nul
goto wait_port_free
