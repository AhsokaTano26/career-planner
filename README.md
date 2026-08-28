# 生涯规划系统·部署手册


## 一、前置依赖（本机已装）

- **JDK 25**：`D:\devtools\jdk25\jdk-25.0.2`（必须用 25，否则启动报 `class file version 69.0`）
- **Maven 3.9+**：`D:\devtools\maven\apache-maven-3.9.16`
- **MySQL**：`D:\devtools\mysql\mysql-26.7.0-winx64`
- **Python 3**：供 career-ai 与冒烟脚本使用

## 二、部署步骤（3 步）

> **一键启动**：双击项目根目录 `start-all.bat` 即可自动完成
> 各服务在独立窗口运行，关闭窗口即停止。

## 三、其他文档

- 详细文档：`docs/guides/使用手册.md`（完整安装/排坑）、`docs/openapi/career-core-apis-live.yaml`（接口定义，以 Apifox 线上为准）、`docs/openapi/career-core-apis-live-summary.md`（接口清单摘要）
- `deploy/docker-compose.yml` 为 Docker 部署**最小骨架**（尚未提供 Dockerfile，暂不可用）；当前按本手册本机部署

## 四、Apifox使用

- 部署完成后，Apifox中选择“开发环境”就可以开始调试