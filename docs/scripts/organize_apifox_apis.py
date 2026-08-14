# -*- coding: utf-8 -*-
"""
从 Apifox CLI 拉取全部接口并按目录分组整理成 Markdown 清单。
用法: python docs/scripts/organize_apifox_apis.py
前置: 已安装 apifox-cli 并登录，projectId 见 .apifox/settings.json
输出: docs/openapi/career-core-apis-live-summary.md
"""
import json
import subprocess
import sys
from collections import OrderedDict

PROJECT_ID = "8662286"
OUT = r"D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live-summary.md"
APIFOX_CMD = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"


def run_apifox(args: list) -> dict:
    """调用 apifox 命令并解析 stdout 为 JSON（按 UTF-8 解码原始字节）"""
    cmd = [APIFOX_CMD, *args, "--project", PROJECT_ID]
    proc = subprocess.run(cmd, capture_output=True)
    text = proc.stdout.decode("utf-8-sig")
    obj = json.loads(text)
    if not obj.get("success", True):
        raise RuntimeError(f"apifox 命令失败: {obj}")
    return obj


def main():
    eps_obj = run_apifox(["endpoint", "list"])
    fld_obj = run_apifox(["folder", "list", "--type", "endpoint"])

    eps = eps_obj["data"]
    folders = fld_obj["data"]
    fmap = {f["id"]: f["name"] for f in folders}

    # 统计
    by_method = {}
    for e in eps:
        m = e["method"].upper()
        by_method[m] = by_method.get(m, 0) + 1

    # 按目录分组（保持 Apifox 目录顺序）
    grouped = OrderedDict()
    for f in folders:
        grouped[f["id"]] = []
    for e in eps:
        grouped.setdefault(e["folderId"], []).append(e)

    lines = []
    lines.append("# 生涯规划系统 · 全量 API 接口清单（Apifox 线上）")
    lines.append("")
    lines.append(
        f"> 数据来源：Apifox 项目「生涯规划系统」(ID {PROJECT_ID}) 主分支 ｜ "
        f"接口总数：**{len(eps)}** ｜ 目录：{len(folders)} 个"
    )
    lines.append("")
    lines.append("## 一、统计概览")
    lines.append("")
    lines.append("| HTTP 方法 | 数量 |")
    lines.append("|-----------|------|")
    for m in ["GET", "POST", "PUT", "PATCH", "DELETE"]:
        lines.append(f"| {m} | {by_method.get(m, 0)} |")
    lines.append("")

    sec = 0
    for fid, items in grouped.items():
        if not items:
            continue
        sec += 1
        fname = fmap.get(fid, "未命名")
        lines.append(f"## 二.{sec} {fname}（{len(items)} 个）")
        lines.append("")
        lines.append("| # | 方法 | 路径 | 说明 |")
        lines.append("|---|------|------|------|")
        for i, e in enumerate(sorted(items, key=lambda x: x["path"]), 1):
            lines.append(
                f"| {i} | `{e['method'].upper()}` | `{e['path']}` | {e['name']} |"
            )
        lines.append("")

    content = "\n".join(lines)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"✅ 完成：{len(eps)} 个接口 / {sec} 个目录 → {OUT}")


if __name__ == "__main__":
    sys.exit(main())
