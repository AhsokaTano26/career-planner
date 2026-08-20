---
name: apifox-cli
description: Use when working with Apifox CLI for API testing, importing/exporting, or troubleshooting CLI issues in this project. Covers path variable substitution, encoding, and environment setup.
---

# Apifox CLI Skill

This skill provides guidance for using Apifox CLI in the career-planner project.

## Key Points

- **CLI Path**: Use `C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd` for Python subprocess calls (`.ps1` cannot be executed by subprocess).
- **Output Encoding**: Write CLI output to file with `cmd /c "... > file"` to preserve UTF-8 bytes; decode in Python with `subprocess.run(...).stdout.decode("utf-8-sig")`.
- **Python Interpreter**: Managed by uv (PEP 668). Do not use `pip install`. Run scripts with `& "C:/Users/uio8k/.local/bin/python.exe" script.py`.
- **Markdown Generation**: Use Python f-strings for backticks in Markdown; avoid PowerShell string interpolation with backticks.
- **API List**: Full interface list at `docs/openapi/career-core-apis-live-summary.md`; reproducible script at `docs/scripts/organize_apifox_apis.py` (project ID 8662286, main branch 127 interfaces).
- **test-case run Issue**: CLI 2.2.9 does not replace path variables in single test case runs. Workaround:
  1. Run `apifox test-case run <id> -e <envId> -r json` to generate report.
  2. Extract `collection` from report JSON and save separately.
  3. Use Python to replace `{taskId}` in URL path with actual value (set `url.variable` to empty).
  4. Execute `apifox run <collection.json> -r cli,json` (real URL returns 200 OK).
  Reference script: `docs/scripts/fix_collection_task.py`. Example test case: "更新任务-正向 (PATCH /tasks/{taskId})" id=404389855, local env 47907998.

## When to Use

- When Apifox CLI commands fail or produce unexpected results.
- When dealing with encoding issues (Chinese乱码) in CLI output.
- When running Python scripts that interact with Apifox CLI.
- When test-case run does not substitute path variables.

## References

- `docs/Apifox-CLI与开发环境排坑记录.md` for detailed troubleshooting.
- `docs/scripts/fix_collection_task.py` for the path variable workaround.