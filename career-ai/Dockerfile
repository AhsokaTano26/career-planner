# career-ai 服务镜像（与 docker-compose.yml 的 build: ./career-ai 对齐）
# 入口：uvicorn api.main:app（见 api/main.py 顶部启动说明）
FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# 编译依赖：litellm/tiktoken/cryptography 可能需要从源码编译
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc g++ libffi-dev libssl-dev rustc cargo \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

# 监听 0.0.0.0 以便容器外（compose 网络 / 宿主）访问；端口与 compose 的 8000 一致
CMD ["uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "8000"]
