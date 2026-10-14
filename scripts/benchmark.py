"""Opt-in benchmark: inserts 10k synthetic tickets into the LOCAL DEV database."""
import concurrent.futures
import json
import os
import pathlib
import platform
import statistics
import subprocess
import time
import urllib.request
import uuid

root = pathlib.Path(__file__).resolve().parent.parent
config = dict(line.split('=', 1) for line in (root / '.local/local.env').read_text().splitlines() if '=' in line)
config.update(os.environ)
os.environ.update(config)
base = config.get('API_URL', 'http://localhost:8080') + '/api/v1'
if not config['DB_URL'].startswith('jdbc:postgresql://localhost:'):
    raise RuntimeError('This benchmark is intended for the local development database only')

def api(method, path, body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body is not None else None, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)

admin = api('POST', '/auth/login', {'email': config['ADMIN_EMAIL'], 'password': config['ADMIN_PASSWORD']})['accessToken']
suffix = uuid.uuid4().hex[:10]
password = uuid.uuid4().hex
email = 'benchmark.' + suffix + '@campusdesk.test'
student = api('POST', '/auth/register', {'name': 'Benchmark Student', 'email': email, 'password': password})
token = api('POST', '/auth/login', {'email': email, 'password': password})['accessToken']
category = api('POST', '/categories', {'name': 'Benchmark ' + suffix, 'description': 'Synthetic data'}, admin)
output = root / '.local' / 'benchmark'
output.mkdir(exist_ok=True)
subprocess.run(['java', '--class-path', str(root / '.tools/jdbc/postgresql-42.7.7.jar'),
                str(root / 'scripts/DatabaseBenchmark.java'), str(student['id']), str(category['id']), str(output)], check=True)

path = '/tickets?size=20&sort=createdAt&direction=desc'
for _ in range(20):
    assert api('GET', path, token=token)['totalElements'] == 10000

def sample(_):
    start = time.perf_counter()
    try:
        result = api('GET', path, token=token)
        ok = result['totalElements'] == 10000 and len(result['content']) == 20
    except Exception:
        ok = False
    return (time.perf_counter() - start) * 1000, ok

start = time.perf_counter()
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    results = list(pool.map(sample, range(100)))
duration = time.perf_counter() - start
latencies = sorted(latency for latency, ok in results if ok)
errors = sum(not ok for _, ok in results)
report = {'measurement': 'application HTTP API including authentication lookup, JPA, DTO JSON and count query',
          'datasetTicketsForStudent': 10000, 'warmup': 20, 'samples': 100, 'concurrency': 4,
          'durationSeconds': duration, 'medianMs': statistics.median(latencies) if latencies else None,
          'p95Ms': latencies[max(0, int(len(latencies) * .95 + .9999) - 1)] if latencies else None,
          'errorRate': errors / 100, 'successfulThroughputPerSecond': (100 - errors) / duration,
          'platform': platform.platform(), 'logicalCpus': os.cpu_count()}
(output / 'api.json').write_text(json.dumps(report, indent=2))
print(json.dumps(report, indent=2))
print('Plans and database measurements saved under .local/benchmark/. Synthetic data is retained.')
