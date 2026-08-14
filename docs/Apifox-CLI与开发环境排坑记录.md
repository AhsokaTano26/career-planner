# Apifox CLI 与开发环境排坑记录

> 记录用 Apifox CLI 拉取/整理接口时的踩坑与解决方案，供后续高效复用。
> 整理日期：2026-08-15 ｜ 环境：Windows 10 企业版 + PowerShell（pwsh）

---

## 1. PowerShell 中文乱码问题（最高频）

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

---

## 2. Python 解释器被 uv 托管，`pip install` 不可用

**现象**：工作区 Python 是 uv 托管的（`C:\Users\uio8k\AppData\Roaming\uv\python\cpython-3.14-...`），直接 `pip install pyyaml` 报错：

```
error: externally-managed-environment
This environment is externally managed ... managed by uv ...
```

**根因**：uv 管理的解释器受 PEP 668 保护，不允许直接修改。

**解决方式**：

1. **优先写不依赖第三方库的脚本**（推荐）：
   - 需要解析 JSON → 用标准库 `json`（无需 pyyaml）。
   - 需要调 CLI → 用标准库 `subprocess`。
   本次 `organize_apifox_apis.py` 即全程只用标准库，零第三方依赖。

2. **临时注入依赖**（非交互有 `uv` 时）：
   ```bash
   uv run --with pyyaml python script.py
   ```
   ⚠️ 本机 `uv` 不在 PATH 时此命令不可用，需先定位 uv 或改走方案 1。

3. **务必用正确解释器路径运行**：终端 `python` 可能与 IDE 选中的解释器不是同一个。用 `get_python_environment_details` 拿到实际可执行路径（本机为 `C:/Users/uio8k/.local/bin/python.exe`）再运行：
   ```powershell
   & "C:/Users/uio8k/.local/bin/python.exe" script.py
   ```

**避坑要点**：不要对 uv 托管解释器执行 `pip install`；脚本优先纯标准库实现。

---

## 3. PowerShell 字符串插值转义错误导致输出异常

**现象**：在 PowerShell 里用双引号字符串拼接 Markdown 时，想输出反引号（Markdown 行内代码 `` ` ``），用了反引号转义 `$`，结果 `$($ep.method.ToUpper())` 没有被求值，而是把整个对象字面量（`@{id=...; name=...}`）拼进了文本，生成文档全是乱码表达式。

**根因**：PowerShell 中：
- `` ` `` 是转义字符，`` `$ `` 表示字面 `$`；
- 在双引号字符串里混用 `` `$ `` 和 `` $() `` 子表达式时极易出错——`$` 被转义成字面量后，后面的表达式不再按插值求值，或把对象整体序列化成 `@{key=value}` 字符串。

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

---

## 4. Apifox CLI 的完整路径

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

---

## 5. apifox 是 npm 的 `.ps1` 包装脚本，Python 无法直接调用 → 用 `apifox.cmd`

**现象**：Python `subprocess.run` 直接执行 `apifox` 失败（见第 4 节）。

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

---

## 6. 将脚本改为调用 `apifox.cmd` 完整路径（示例）

修复后的可复跑脚本片段（`docs/scripts/organize_apifox_apis.py`）：

```python
PROJECT_ID = "8662286"
APIFOX_CMD = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"

def run_apifox(args: list) -> dict:
    """调用 apifox 命令并解析 stdout 为 JSON（按 UTF-8 解码原始字节）"""
    cmd = [APIFOX_CMD, *args, "--project", PROJECT_ID]
    proc = subprocess.run(cmd, capture_output=True)
    text = proc.stdout.decode("utf-8-sig")   # 解决中文乱码（见第 1 节）
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

---

## 汇总速查表

| 问题 | 一句话解决 |
|------|-----------|
| CLI 输出中文乱码 | `cmd /c "... > file"` 落盘原始字节，或 Python subprocess 捕获后 `decode("utf-8-sig")` |
| `pip install` 报 externally-managed | 脚本只用标准库，别装第三方包；或 `uv run --with <pkg> python ...` |
| 终端 python 与 IDE 不一致 | 用解释器绝对路径运行：`& "C:/Users/uio8k/.local/bin/python.exe" script.py` |
| PowerShell 拼 Markdown 转义混乱 | 改用 Python f-string 生成文档 |
| Python 调不到 apifox | 用 `.cmd` 完整路径，不用 `.ps1` |
| apifox 真实路径 | `C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd` |
