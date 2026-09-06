import json
import sys

with open(r'D:\Zht20241287\career-planner\docs\openapi\career-core-apis-live.yaml', 'r', encoding='utf-8') as f:
    d = json.load(f)

# OpenAPI 3.0 basic validation
assert 'openapi' in d
assert d['openapi'].startswith('3.0')
assert 'info' in d
assert 'paths' in d
assert isinstance(d['paths'], dict)
assert d['paths']
assert 'components' in d
assert 'schemas' in d['components']

tag_names = {t['name'] for t in d['tags']}

print(f'OpenAPI version: {d["openapi"]}')
print(f'Paths: {len(d["paths"])}')
print(f'Tags: {len(d["tags"])}')
print(f'Schemas: {len(d["components"]["schemas"])}')
print(f'Servers: {len(d["servers"])}')

assert 'AI-Gateway' in tag_names, 'AI-Gateway tag missing'
print('AI-Gateway tag: OK')

assert '/api/v1/gateway/generate' in d['paths']
assert '/api/v1/gateway/chat/completions' in d['paths']
assert '/v1/chat/completions' not in d['paths'], 'Old /v1/chat/completions should be removed'
print('Gateway paths: OK')

# Check all $ref resolve
def collect_refs(obj, refs):
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k == '$ref' and isinstance(v, str):
                refs.add(v)
            else:
                collect_refs(v, refs)
    elif isinstance(obj, list):
        for item in obj:
            collect_refs(item, refs)

refs = set()
collect_refs(d, refs)
schema_refs = {r.replace('#/components/schemas/', '') for r in refs if r.startswith('#/components/schemas/')}
defined = set(d['components']['schemas'].keys())
missing = schema_refs - defined
assert not missing, f'Missing schemas: {missing}'
print(f'Schema refs: {len(schema_refs)}, missing: {len(missing)} - OK')

# Spot check 5 endpoints
checks = [
    ('/api/v1/auth/login', 'post', 'ApiResponseTokenVO'),
    ('/api/v1/students/me/plans/latest', 'get', 'ApiResponsePlanVO'),
    ('/api/v1/students/me/tasks/{taskId}', 'put', 'ApiResponseTaskVO'),
    ('/api/v1/reviews/drafts', 'post', 'ApiResponseReviewVO'),
    ('/api/v1/gateway/generate', 'post', 'GatewayGenerateResponse'),
    ('/api/v1/gateway/chat/completions', 'post', 'HTTPValidationError'),
]
def find_200_schema(op):
    for status in ('200', '201', 'default'):
        if status not in op.get('responses', {}):
            continue
        resp = op['responses'][status]
        content = resp.get('content', {})
        for ct, schema_wrapper in content.items():
            schema_obj = schema_wrapper.get('schema', {})
            if '$ref' in schema_obj:
                return schema_obj['$ref'].split('/')[-1], status, ct
            elif 'type' in schema_obj and schema_obj.get('type') == 'object' and '$ref' in schema_obj.get('properties', {}).get('data', {}):
                return schema_obj['properties']['data']['$ref'].split('/')[-1], status, ct + ' (inline)'
    return None, None, None

for path, method, expected_schema in checks:
    op = d['paths'][path][method]
    actual, status, ct = find_200_schema(op)
    if actual:
        ok = 'OK' if expected_schema in actual else 'MISMATCH'
        print(f'  {path} {method.upper()} ({ct}) [{status}] -> {actual} {ok}')
    else:
        print(f'  {path} {method.upper()} -> no 200 response with $ref')

print()
print('All validations passed')
