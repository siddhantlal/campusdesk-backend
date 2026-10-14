export const e = value => String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
export const titleCase = value => String(value || '').toLowerCase().replace(/(^|[ _])\w/g, text => text.replace('_', ' ').toUpperCase());
export const initials = name => String(name || '?').trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
export const ticketCode = id => `CD-${String(id).padStart(4, '0')}`;
export function date(value, full = false) {
  if (!value) return '—';
  return new Intl.DateTimeFormat(undefined, full ? { dateStyle: 'medium', timeStyle: 'short' } : { month: 'short', day: 'numeric' }).format(new Date(value));
}
const paths = {
  grid: '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
  ticket: '<path d="M4 4h16v5a3 3 0 0 0 0 6v5H4v-5a3 3 0 0 0 0-6Z"/><path d="M14 4v3m0 4v2m0 4v3"/>',
  layers: '<path d="m12 3 10 5-10 5L2 8Z"/><path d="m2 12 10 5 10-5M2 16l10 5 10-5"/>',
  users: '<circle cx="9" cy="8" r="3"/><path d="M3 21v-2a6 6 0 0 1 12 0v2m1-16a3 3 0 0 1 0 6m5 10v-2a6 6 0 0 0-4-5"/>',
  arrow: '<path d="M5 12h14m-6-6 6 6-6 6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  message: '<path d="M21 11a8 8 0 0 1-8 8H6l-4 3V11a9 9 0 0 1 19 0Z"/><path d="M7 10h10m-10 4h6"/>',
  logout: '<path d="M10 4H4v16h6m5-13 5 5-5 5m-7-5h12"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  back: '<path d="M19 12H5m6-6-6 6 6 6"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  help: '<circle cx="12" cy="12" r="9"/><path d="M9 9a3 3 0 0 1 6 0c0 2-3 2-3 4m0 3h.01"/>',
  leaf: '<path d="M20 3C9 3 3 8 5 15c3 8 15 4 15-12Z"/><path d="M4 21 15 10"/>',
  refresh: '<path d="M20 7v5h-5M4 17v-5h5"/><path d="M5 8a8 8 0 0 1 13-3l2 2M4 17l2 2a8 8 0 0 0 13-3"/>',
};
export function icon(name, cls = '') { return `<svg class="icon ${e(cls)}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.ticket}</svg>`; }
export const brand = '<span class="brand-mark" aria-hidden="true"><i></i><i></i><i></i><i></i></span>';
export const badge = (value, type = 'status') => `<span class="badge ${e(type)} ${e(String(value).toLowerCase())}"><span class="badge-dot"></span>${e(titleCase(value))}</span>`;
export function toast(message, bad = false) {
  const item = document.createElement('div');
  item.className = `toast${bad ? ' bad' : ''}`;
  item.innerHTML = `${icon(bad ? 'help' : 'check')}<span>${e(message)}</span><button aria-label="Dismiss notification">${icon('close')}</button>`;
  item.querySelector('button').onclick = () => item.remove();
  document.querySelector('#notifications').append(item);
  setTimeout(() => item.remove(), bad ? 10000 : 5500);
}
export function empty(title, message, action = '', glyph = 'ticket') {
  return `<div class="empty"><div class="empty-icon">${icon(glyph)}</div><h3>${e(title)}</h3><p>${e(message)}</p>${action}</div>`;
}
export function loading() { return '<div class="loading" role="status"><span class="spinner"></span>Getting things ready…</div>'; }
export function pager(page, change) {
  const el = document.createElement('div');
  el.className = 'pager';
  const start = page.totalElements ? page.page * page.size + 1 : 0;
  el.innerHTML = `<span>${start}–${Math.min((page.page + 1) * page.size, page.totalElements)} of ${page.totalElements.toLocaleString()}</span><div><button class="btn small" data-prev ${page.page === 0 ? 'disabled' : ''}>${icon('back')}<span>Previous</span></button><span class="page-count">${page.page + 1} / ${Math.max(1, page.totalPages)}</span><button class="btn small" data-next ${page.page + 1 >= page.totalPages ? 'disabled' : ''}><span>Next</span>${icon('arrow')}</button></div>`;
  el.querySelector('[data-prev]').onclick = () => change(page.page - 1);
  el.querySelector('[data-next]').onclick = () => change(page.page + 1);
  return el;
}
export function modal(title, content, description = '') {
  const el = document.createElement('dialog');
  el.className = 'modal';
  el.innerHTML = `<div class="modal-heading"><div><h2>${e(title)}</h2>${description ? `<p>${e(description)}</p>` : ''}</div><button class="icon-btn" type="button" data-close aria-label="Close dialog">${icon('close')}</button></div>${content}`;
  el.addEventListener('close', () => el.remove(), { once: true });
  el.addEventListener('click', event => {
    if (event.target === el) {
      const rect = el.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) el.close();
    }
  });
  el.querySelectorAll('[data-close]').forEach(button => { button.onclick = () => el.close(); });
  document.body.append(el);
  el.showModal();
  return { el, close: () => el.close() };
}
export function formError(form, error) {
  const box = form.querySelector('.form-error');
  if (!box) return;
  const fields = Object.entries(error.fields || {}).map(([key, value]) => `${titleCase(key.replace(/([A-Z])/g, ' $1'))}: ${value}`).join(' · ');
  box.textContent = [error.message, fields].filter(Boolean).join(' ');
  box.hidden = false;
  box.focus();
}
export function wireForm(form, handler) {
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!form.reportValidity() || form.dataset.pending) return;
    const submit = form.querySelector('[type=submit]');
    const original = submit.innerHTML;
    form.dataset.pending = 'true'; submit.disabled = true; submit.textContent = 'Please wait…';
    form.querySelector('.form-error')?.setAttribute('hidden', '');
    try { await handler(Object.fromEntries(new FormData(form)), form); }
    catch (error) { if (error.name !== 'AbortError') formError(form, error); }
    finally { delete form.dataset.pending; submit.disabled = false; submit.innerHTML = original; }
  });
}
export const formNotice = '<div class="form-error" role="alert" tabindex="-1" hidden></div>';
export const formButtons = label => `<div class="form-actions"><button type="button" class="btn" data-close>Cancel</button><button type="submit" class="btn primary">${e(label)}${icon('arrow')}</button></div>`;
export const passwordField = '<label>Password<input name="password" type="password" required minlength="12" maxlength="64" autocomplete="new-password" placeholder="At least 12 characters"><small>12–64 characters, at most 72 UTF-8 bytes.</small></label>';
export function checkPassword(value) {
  if (new TextEncoder().encode(value).length > 72) throw new Error('Password must be at most 72 UTF-8 bytes. Try fewer characters.');
}

// Picker pages stay bounded; searching is delegated to the authorized API.
export function picker(title, fetchPage, { describe = item => item.description || item.email || '', searchable = false } = {}) {
  return new Promise(resolve => {
    let page = 0, search = '', chosen = null, generation = 0;
    const dialog = modal(title, `${searchable ? '<form class="picker-search"><label class="sr-only" for="picker-search">Search</label><input id="picker-search" name="search" placeholder="Search by name or email" maxlength="100"><button class="btn" type="submit">Search</button></form>' : ''}<div class="picker-list"></div><div class="picker-pages"></div>`);
    dialog.el.addEventListener('close', () => { generation++; resolve(chosen); }, { once: true });
    const load = async () => {
      const current = ++generation;
      const list = dialog.el.querySelector('.picker-list');
      list.innerHTML = loading();
      try {
        const result = await fetchPage(page, search);
        if (current !== generation || !dialog.el.open) return;
        list.innerHTML = result.content.length ? result.content.map(item => `<button type="button" class="picker-item" data-id="${e(item.id)}"><span class="picker-symbol">${icon(searchable ? 'users' : 'layers')}</span><span><strong>${e(item.name)}</strong><small>${e(describe(item))}</small></span>${icon('arrow')}</button>`).join('') : empty('Nothing here yet', searchable ? 'Try a different search, or create an active staff account first.' : 'An administrator needs to add an active category first.', '', searchable ? 'users' : 'layers');
        list.querySelectorAll('[data-id]').forEach(button => { button.onclick = () => { chosen = result.content.find(item => String(item.id) === button.dataset.id); dialog.close(); }; });
        dialog.el.querySelector('.picker-pages').replaceChildren(pager(result, next => { page = next; load(); }));
      } catch (error) { if (current === generation) list.innerHTML = empty('Could not load options', error.message); }
    };
    dialog.el.querySelector('form')?.addEventListener('submit', event => { event.preventDefault(); search = new FormData(event.target).get('search'); page = 0; load(); });
    load();
  });
}
