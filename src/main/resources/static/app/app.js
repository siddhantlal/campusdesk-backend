import { api, authenticated, saveToken } from './api.js';
import { e, brand, icon, initials, titleCase, loading, empty, modal, toast } from './ui.js';
import { authView } from './auth.js';
import { dashboard } from './dashboard.js';
import { ticketsView, ticketDetail, createTicket, clearDrafts } from './tickets.js';
import { categoriesView, categoryForm, peopleView, staffForm } from './admin.js';

const root = document.querySelector('#app');
let user = null, activeContext = null, navigation = 0, controller;
function go(path) {
  const hash = `#${path}`;
  if (location.hash === hash) render(); else location.hash = hash;
}
function logout(message = '') {
  saveToken(''); clearDrafts(); user = null; go('/signin');
  if (message) toast(message, true);
}
function help() {
  const student = user.role === 'STUDENT', staff = user.role === 'STAFF';
  const steps = student ? ['Raise a request with a clear title, details, and service category.', 'Follow updates in your request’s conversation and history.', 'When resolved, close it to confirm the fix, or reopen it with a reason.'] : staff ? ['Open a request assigned to you and start working.', 'Share updates in the conversation as you investigate.', 'Resolve the request with a clear summary of the work completed.'] : ['Create service categories and staff accounts to set up your campus.', 'Assign requests to active staff and keep an eye on progress.', 'Manage account access and follow the same request lifecycle as your team.'];
  modal('A little guidance.', `<ol class="help-steps">${steps.map((step, i) => `<li><span>${i + 1}</span><p>${e(step)}</p></li>`).join('')}</ol><p class="field-hint">Need a fresh view? Use the refresh button. Your session is short-lived; sign in again if it ends.</p>`, 'Here’s how to get the most from your workspace.');
}
function profile() {
  modal('Your campus account', `<div class="profile-heading"><span class="avatar large">${e(initials(user.name))}</span><div><h3>${e(user.name)}</h3><p>${e(user.email)}</p><span class="role-tag">${titleCase(user.role)}</span></div></div><p class="field-hint">Your access is managed by your campus administrator.</p>`);
}
function shell(section) {
  const nav = [['overview', 'grid', 'Overview'], ['tickets', 'ticket', user.role === 'STAFF' ? 'Assigned requests' : user.role === 'ADMIN' ? 'All requests' : 'My requests'], ['categories', 'layers', 'Service categories'], ...(user.role === 'ADMIN' ? [['people', 'users', 'People & access']] : [])];
  root.innerHTML = `<div class="app-shell"><aside class="sidebar" id="sidebar"><a class="brand" href="#/overview">${brand}<span>CampusDesk<small>CAMPUS SERVICE DESK</small></span></a><div class="workspace-label">${titleCase(user.role)} workspace <span></span></div><span class="nav-heading">YOUR DESK</span><nav aria-label="Main navigation">${nav.map(([path, glyph, label]) => `<a href="#/${path}" ${section === path ? 'aria-current="page"' : ''} class="nav-link ${section === path ? 'active' : ''}">${icon(glyph)}<span>${label}</span>${section === path ? '<i></i>' : ''}</a>`).join('')}</nav>${user.role === 'STUDENT' ? `<button class="sidebar-new" data-new-ticket>${icon('plus')}Raise a request</button>` : ''}<div class="sidebar-bottom"><div class="support-card">${icon('leaf')}<h3>A little support.<br>A better day.</h3><p>Your campus service team is here to help.</p><button data-help>How it works${icon('arrow')}</button></div><button class="sidebar-help" data-help>${icon('help')}Need a little guidance?</button><div class="sidebar-person"><button data-profile class="person-inline"><span class="avatar">${e(initials(user.name))}</span><span><strong>${e(user.name)}</strong><small>${titleCase(user.role)} account</small></span></button><button class="icon-btn" data-logout aria-label="Sign out">${icon('logout')}</button></div></div></aside><div class="sidebar-shade" data-sidebar-close></div><div class="workspace"><header class="topbar"><div><button class="icon-btn menu-toggle" data-menu aria-label="Open navigation" aria-expanded="false">${icon('menu')}</button><span class="topbar-brand">CampusDesk</span><span class="topbar-divider">/</span><span>${nav.find(([path]) => path === section)?.[2] || 'Workspace'}</span></div><div class="topbar-actions"><span class="workspace-pill"><i></i>Campus workspace</span><button class="icon-btn" data-refresh aria-label="Refresh current page">${icon('refresh')}</button><button class="avatar small" data-profile aria-label="View your account">${e(initials(user.name))}</button></div></header><main id="content" tabindex="-1">${loading()}</main></div></div>`;
}
async function render() {
  const turn = ++navigation;
  controller?.abort(); controller = new AbortController();
  document.querySelectorAll('dialog').forEach(dialog => dialog.close());
  const [path, rawQuery = ''] = (location.hash.slice(1) || '/overview').split('?');
  if (!user) {
    activeContext = null;
    authView(root, path === '/register', loggedIn => { user = loggedIn; go('/overview'); });
    document.title = `${path === '/register' ? 'Create an account' : 'Sign in'} · CampusDesk`;
    return;
  }
  if (path === '/signin' || path === '/register') { go('/overview'); return; }
  const section = path.split('/')[1] || 'overview';
  shell(section);
  document.title = `${({ overview: 'Overview', tickets: 'Requests', categories: 'Service categories', people: 'People & access' })[section] || 'Workspace'} · CampusDesk`;
  const signal = controller.signal;
  const ctx = {
    user, content: root.querySelector('#content'), params: new URLSearchParams(rawQuery), go,
    get: endpoint => api(endpoint, { signal }),
    current: () => navigation === turn && !signal.aborted,
    refresh: () => { if (navigation === turn) render(); },
  };
  activeContext = ctx;
  try {
    if (section === 'overview' && path === '/overview') await dashboard(ctx);
    else if (path === '/tickets') await ticketsView(ctx);
    else if (/^\/tickets\/[1-9]\d*$/.test(path)) await ticketDetail(ctx, path.split('/')[2]);
    else if (path === '/categories') await categoriesView(ctx);
    else if (path === '/people') await peopleView(ctx);
    else ctx.content.innerHTML = empty('A wrong turn.', 'This page isn’t part of your workspace.', '<a class="btn primary" href="#/overview">Back to overview</a>');
  } catch (error) {
    if (error.name === 'AbortError' || !ctx.current()) return;
    ctx.content.innerHTML = `<section class="card page-error">${empty(error.status === 404 ? 'This request isn’t available.' : 'Let’s try that again.', error.message, `<button class="btn primary" data-refresh>${icon('refresh')}Try again</button><a class="btn" href="#/overview">Back to overview</a>`, 'help')}</section>`;
  }
}
document.addEventListener('click', event => {
  const button = event.target.closest('button, a');
  if (!button) return;
  if (button.matches('[data-logout]')) logout();
  else if (button.matches('[data-help]') && user) help();
  else if (button.matches('[data-profile]') && user) profile();
  else if (button.matches('[data-refresh]')) activeContext?.refresh();
  else if (button.matches('[data-new-ticket]') && user?.role === 'STUDENT' && activeContext) createTicket(activeContext);
  else if (button.matches('[data-create-category]') && user?.role === 'ADMIN' && activeContext) categoryForm(activeContext);
  else if (button.matches('[data-create-staff]') && user?.role === 'ADMIN' && activeContext) staffForm(activeContext);
  else if (button.matches('[data-menu]')) {
    root.querySelector('.app-shell')?.classList.toggle('nav-open');
    button.setAttribute('aria-expanded', String(root.querySelector('.app-shell')?.classList.contains('nav-open')));
  } else if (button.matches('[data-sidebar-close]')) root.querySelector('.app-shell')?.classList.remove('nav-open');
});
document.addEventListener('click', event => {
  if (event.target.closest('[data-sidebar-close]')) root.querySelector('.app-shell')?.classList.remove('nav-open');
});
document.addEventListener('keydown', event => { if (event.key === 'Escape') root.querySelector('.app-shell')?.classList.remove('nav-open'); });
window.addEventListener('hashchange', render);
window.addEventListener('session-expired', () => logout('Your session ended. Please sign in again.'));

async function start() {
  if (authenticated()) {
    try { user = await api('/users/me'); }
    catch (error) {
      if (error.status === 401) saveToken('');
      else { root.innerHTML = `<main class="boot-error">${empty('Could not open your workspace.', error.message, '<button class="btn primary" id="retry-start">Try again</button>', 'help')}</main>`; root.querySelector('#retry-start').onclick = start; return; }
    }
  }
  render();
}
start();
