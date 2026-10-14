const key = 'campusdesk.access-token';
let token = '';
try { token = sessionStorage.getItem(key) || ''; } catch { /* Private browsing may restrict storage. */ }

export function authenticated() { return Boolean(token); }
export function saveToken(value) {
  token = value || '';
  try { token ? sessionStorage.setItem(key, token) : sessionStorage.removeItem(key); } catch { /* Keep the in-memory session. */ }
}
export class ApiError extends Error {
  constructor(response, data) {
    super(data.detail || `Request failed (${response.status})`);
    this.status = response.status;
    this.code = data.code;
    this.fields = data.fieldErrors || {};
    this.reference = data.correlationId;
  }
}
export async function api(path, { method = 'GET', body, signal, publicRequest = false } = {}) {
  const headers = { Accept: 'application/json' };
  if (token && !publicRequest) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(`/api/v1${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal, credentials: 'omit' });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error('Could not connect. Check your connection and try again.');
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401 && !publicRequest && token) {
      saveToken('');
      window.dispatchEvent(new CustomEvent('session-expired'));
    }
    throw new ApiError(response, data);
  }
  return data;
}
export const post = (path, body, options = {}) => api(path, { ...options, method: 'POST', body });
export const patch = (path, body) => api(path, { method: 'PATCH', body });
export function query(values = {}) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => { if (value !== '' && value !== null && value !== undefined) params.set(key, String(value)); });
  const text = params.toString();
  return text ? `?${text}` : '';
}
