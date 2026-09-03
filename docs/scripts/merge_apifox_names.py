# -*- coding: utf-8 -*-
"""
增量合并：以 Apifox（主分支 main）为准，单向把「接口名」同步到 OpenAPI yaml。

只处理「路径 + 方法」两边都存在的接口（交集），仅覆盖名称相关字段：
  - summary      （Apifox 接口名，改名的主要落点）
  - operationId  （Apifox 由接口名推导的操作 ID）

不增删任何路径 / 接口；不在交集内的路径（Apifox 新增 or yaml 独有）一律不动，
避免破坏 yaml 里 career-ai 等异源接口，也避免把 Apifox 新接口未经确认就引入。

用法:
  python docs/scripts/merge_apifox_names.py <apifox_export.json> [--yaml <path>]
前置:
  apifox export --project 8662286 --format openapi --oas-version 3.0 \
      --output docs/openapi/_apifox_main.json --branch main
"""
import io
import json
import sys

APIFOX_EXPORT = r"D:\Zht20241287\career-planner\docs\openapi\_apifox_main.json"
YAML_PATH = r"D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live.yaml"

HTTP_METHODS = ("get", "post", "put", "patch", "delete")
NAME_FIELDS = ("summary", "operationId")


def load_json(path):
    with io.open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def iter_ops(doc):
    """yield (path, method, operation) for each HTTP operation."""
    for p, methods in (doc.get("paths") or {}).items():
        for m, op in (methods or {}).items():
            if m.lower() in HTTP_METHODS:
                yield p, m.lower(), op


def build_lookup(doc):
    """path+method -> operation"""
    return {(p, m): op for p, m, op in iter_ops(doc)}


def json_str(s):
    """JSON-encode a string as it would appear in a compact literal."""
    return json.dumps(s, ensure_ascii=False)


def main():
    args = sys.argv[1:]
    apifox_path = args[0] if args else APIFOX_EXPORT
    yaml_path = None
    if "--yaml" in args:
        yaml_path = args[args.index("--yaml") + 1]
    if not yaml_path:
        yaml_path = YAML_PATH

    yaml_doc = load_json(yaml_path)
    apifox_doc = load_json(apifox_path)

    y_map = build_lookup(yaml_doc)
    a_map = build_lookup(apifox_doc)

    # compute changes
    changes = []  # (path, method, field, old, new)
    for key, y_op in y_map.items():
        a_op = a_map.get(key)
        if not a_op:
            continue  # not in Apifox -> leave yaml untouched
        for field in NAME_FIELDS:
            old = y_op.get(field)
            new = a_op.get(field)
            if field == "description":
                continue  # 只改「名」，不动接口说明
            if old != new:
                changes.append((key[0], key[1], field, old, new))

    if not changes:
        print("无名称差异，无需合并。")
        return

    # apply to raw text with precise string replacement (minimal diff)
    with io.open(yaml_path, "r", encoding="utf-8") as f:
        text = f.read()

    applied = 0
    missed = []
    for path, method, field, old, new in changes:
        old_lit = json_str(old) if old is not None else "null"
        new_lit = json_str(new) if new is not None else "null"
        # replacement target: "field": "<old>",
        target = '"%s": %s' % (field, old_lit)
        repl = '"%s": %s' % (field, new_lit)
        # count occurrences of field literal to detect ambiguity
        n = text.count(target)
        if n == 0:
            missed.append((path, method, field, old, new, "target not found"))
            continue
        text = text.replace(target, repl, 1)
        applied += 1

    with io.open(yaml_path, "w", encoding="utf-8") as f:
        f.write(text)

    # report
    print("应用替换：%d 处" % applied)
    print("字段差异明细：")
    seen = set()
    for path, method, field, old, new in changes:
        verbose = (path, method) not in seen
        seen.add((path, method))
        marker = "  * " if verbose else "    "
        print("%s%s %s [%s] -> [%s]" % (marker, method.upper(), path, old, new))
    if missed:
        print("\n未能定位的替换（需人工核对）：")
        for m in missed:
            print("  ", m)


if __name__ == "__main__":
    main()
