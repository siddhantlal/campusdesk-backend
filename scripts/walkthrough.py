"""Executable curl walkthrough. Python 3 + curl; no packages. Requires a running app."""
import json
import os
import pathlib
import subprocess
import uuid

root = pathlib.Path(__file__).resolve().parent.parent
local = root / '.local' / 'local.env'
config = {}
if local.exists():
    config = dict(line.split('=', 1) for line in local.read_text().splitlines() if '=' in line)
config.update(os.environ)
base = config.get('API_URL', 'http://localhost:8080') + '/api/v1'

def api(method, path, body=None, token=None):
    cmd = ['curl', '--silent', '--show-error', '--fail-with-body', '--request', method, base + path]
    if token:
        cmd += ['--header', 'Authorization: Bearer ' + token]
    if body is not None:
        cmd += ['--header', 'Content-Type: application/json', '--data-binary', '@-']
    result = subprocess.run(cmd, input=json.dumps(body) if body is not None else None, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(f'{method} {path}: {result.stderr} {result.stdout}')
    return json.loads(result.stdout)

def login(email, password):
    return api('POST', '/auth/login', {'email': email, 'password': password})['accessToken']

suffix = uuid.uuid4().hex[:10]
password = uuid.uuid4().hex + 'aA!'
admin = login(config['ADMIN_EMAIL'], config['ADMIN_PASSWORD'])
email = f'student.{suffix}@campusdesk.test'
api('POST', '/auth/register', {'name': 'Walkthrough Student', 'email': email, 'password': password})
student = login(email, password)
category = api('POST', '/categories', {'name': 'Facilities ' + suffix, 'description': 'Campus maintenance'}, admin)
staff_email = f'staff.{suffix}@campusdesk.test'
staff = api('POST', '/admin/staff', {'name': 'Walkthrough Staff', 'email': staff_email, 'password': password}, admin)
staff_token = login(staff_email, password)
ticket = api('POST', '/tickets', {'title': 'Library fan stopped working', 'description': 'The ceiling fan near desk 12 is not turning.',
                                 'categoryId': category['id'], 'priority': 'HIGH'}, student)
path = '/tickets/' + str(ticket['id'])
ticket = api('PATCH', path + '/assignment', {'assigneeId': staff['id'], 'version': ticket['version']}, admin)
ticket = api('PATCH', path + '/status', {'status': 'IN_PROGRESS', 'version': ticket['version']}, staff_token)
ticket = api('PATCH', path + '/status', {'status': 'RESOLVED', 'resolutionSummary': 'Replaced the faulty capacitor and verified operation.',
                                      'version': ticket['version']}, staff_token)
ticket = api('PATCH', path + '/status', {'status': 'CLOSED', 'version': ticket['version']}, student)
events = api('GET', path + '/events', token=student)
assert ticket['status'] == 'CLOSED' and events['totalElements'] == 5
print(f"Walkthrough succeeded: ticket {ticket['id']} is CLOSED with {events['totalElements']} history events.")
for event in events['content']:
    print(f"  {event['eventType']}: {event['previousValue']} -> {event['newValue']}")
