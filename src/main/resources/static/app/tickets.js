import { api, post, patch, query } from './api.js';
import { e, icon, titleCase, badge, date, ticketCode, initials, empty, pager, modal, picker, wireForm, formNotice, formButtons, toast } from './ui.js';

export const statuses = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const chooseCategory = () => picker('Choose a service category', (page) => api(`/categories${query({ page, size: 20 })}`));
export const chooseStaff = () => picker('Assign a staff member', (page, search) => api(`/admin/users${query({ page, size: 20, role: 'STAFF', active: true, search })}`), { searchable: true });

export function ticketTable(tickets) {
  return `<div class="table-scroll"><table class="request-table"><thead><tr><th>Request</th><th>Status</th><th>Priority</th><th>Assigned to</th><th>Created</th><th><span class="sr-only">Open request</span></th></tr></thead><tbody>${tickets.map(t => `<tr><td><a class="request-link" href="#/tickets/${e(t.id)}"><small>${ticketCode(t.id)} <span>· ${e(t.categoryName)}</span></small><strong>${e(t.title)}</strong></a></td><td>${badge(t.status)}</td><td>${badge(t.priority, 'priority')}</td><td>${t.assignee ? `<span class="person-inline"><span class="avatar tiny">${e(initials(t.assignee.name))}</span>${e(t.assignee.name)}</span>` : '<span class="muted">Not assigned</span>'}</td><td class="nowrap muted">${date(t.createdAt)}</td><td><a class="icon-btn" aria-label="Open ${e(ticketCode(t.id))}" href="#/tickets/${e(t.id)}">${icon('arrow')}</a></td></tr>`).join('')}</tbody></table></div>`;
}

export function createTicket(ctx) {
  let category = null;
  const dialog = modal('A little help starts here.', `<form class="stack-form">${formNotice}<label>What needs attention?<input name="title" required maxlength="160" placeholder="A short, clear title"></label><label>Tell us a little more<textarea name="description" required maxlength="5000" rows="4" placeholder="Where is the issue? What happened? Any details that could help?"></textarea></label><label>Service category<button class="btn field-picker" type="button" data-category><span>Choose a category</span>${icon('layers')}</button></label><label>How urgent is it?<select name="priority"><option value="LOW">Low — can wait a little</option><option value="MEDIUM" selected>Medium — needs attention soon</option><option value="HIGH">High — needs prompt attention</option></select></label>${formButtons('Send request')}</form>`, 'Your service team will take it from here.');
  dialog.el.querySelector('[data-category]').onclick = async () => {
    const selected = await chooseCategory();
    if (selected && dialog.el.open) { category = selected; dialog.el.querySelector('[data-category] span').textContent = selected.name; }
  };
  wireForm(dialog.el.querySelector('form'), async values => {
    if (!category) throw new Error('Choose a service category before sending your request.');
    const ticket = await post('/tickets', { title: values.title.trim(), description: values.description.trim(), categoryId: category.id, priority: values.priority });
    dialog.close(); toast('Your request is with the team.'); ctx.go(`/tickets/${ticket.id}`);
  });
}

export async function ticketsView(ctx) {
  const params = ctx.params;
  const page = Math.max(0, Number(params.get('page')) || 0);
  const sortChoice = params.get('order') || 'createdAt:desc';
  const [sort, direction] = sortChoice.split(':');
  const filters = { page, size: 15, status: params.get('status'), priority: params.get('priority'), categoryId: params.get('categoryId'), title: params.get('title'), sort, direction };
  if (ctx.user.role === 'ADMIN') filters.assigneeId = params.get('assigneeId');
  const result = await ctx.get(`/tickets${query(filters)}`);
  if (!ctx.current()) return;
  const route = changes => {
    const next = Object.fromEntries(params);
    Object.entries(changes).forEach(([key, value]) => { if (value === null || value === '') delete next[key]; else next[key] = value; });
    return `/tickets${query(next)}`;
  };
  const admin = ctx.user.role === 'ADMIN';
  ctx.content.innerHTML = `<div class="page-heading"><div><span class="eyebrow">${admin ? 'CAMPUS WORKSPACE' : ctx.user.role === 'STAFF' ? 'YOUR WORKSPACE' : 'YOUR CAMPUS, TAKEN CARE OF'}</span><h1>${admin ? 'Campus requests' : ctx.user.role === 'STAFF' ? 'Your assigned requests' : 'Your requests'}<span class="heading-count">${result.totalElements.toLocaleString()}</span></h1><p>${admin ? 'Keep every campus request moving in the right direction.' : ctx.user.role === 'STAFF' ? 'A clear view of the people who need your help.' : 'A little issue or a big question. Follow it all here.'}</p></div>${ctx.user.role === 'STUDENT' ? `<button class="btn primary" data-new-ticket>${icon('plus')}New request</button>` : ''}</div><section class="card request-card"><nav class="status-tabs" aria-label="Filter request status"><a class="${!filters.status ? 'selected' : ''}" href="#${e(route({ status: null, page: null }))}">All requests</a>${statuses.map(status => `<a class="${filters.status === status ? 'selected' : ''}" href="#${e(route({ status, page: null }))}">${titleCase(status)}</a>`).join('')}</nav><form class="filter-bar" id="ticket-filters"><label class="search-field">${icon('search')}<span class="sr-only">Search request titles</span><input name="title" placeholder="Search your requests…" maxlength="160" value="${e(filters.title)}"></label><label><span class="sr-only">Priority</span><select name="priority"><option value="">All priorities</option>${['LOW', 'MEDIUM', 'HIGH'].map(p => `<option value="${p}" ${filters.priority === p ? 'selected' : ''}>${titleCase(p)} priority</option>`).join('')}</select></label><label><span class="sr-only">Sort requests</span><select name="order">${[['createdAt:desc', 'Newest first'], ['createdAt:asc', 'Oldest first'], ['updatedAt:desc', 'Recently updated']].map(([value, label]) => `<option value="${value}" ${sortChoice === value ? 'selected' : ''}>${label}</option>`).join('')}</select></label><button class="btn" type="submit">Search</button></form><div class="filter-extras"><button class="filter-chip" data-category>${icon('layers')}${e(params.get('categoryName') || 'Category')}${filters.categoryId ? '<span>· selected</span>' : ''}</button>${admin ? `<button class="filter-chip" data-assignee>${icon('users')}${e(params.get('assigneeName') || 'Assignee')}${filters.assigneeId ? '<span>· selected</span>' : ''}</button>` : ''}${[filters.status, filters.priority, filters.title, filters.categoryId, filters.assigneeId].some(Boolean) ? '<a class="text-link" href="#/tickets">Clear filters</a>' : ''}<span class="muted">${result.totalElements === 1 ? '1 request' : `${result.totalElements.toLocaleString()} requests`}</span></div>${result.content.length ? ticketTable(result.content) : empty('A clear desk.', 'No requests match this view. Try different filters, or start a new request.', ctx.user.role === 'STUDENT' ? `<button class="btn primary" data-new-ticket>${icon('plus')}New request</button>` : '')}<div id="ticket-pager"></div></section>`;
  ctx.content.querySelector('#ticket-pager').append(pager(result, next => ctx.go(route({ page: next }))));
  const filterForm = ctx.content.querySelector('#ticket-filters');
  filterForm.onsubmit = event => { event.preventDefault(); ctx.go(route({ ...Object.fromEntries(new FormData(filterForm)), page: null })); };
  filterForm.querySelectorAll('select').forEach(select => { select.onchange = () => filterForm.requestSubmit(); });
  ctx.content.querySelector('[data-category]').onclick = async () => { const item = await chooseCategory(); if (item && ctx.current()) ctx.go(route({ categoryId: item.id, categoryName: item.name, page: null })); };
  const staffButton = ctx.content.querySelector('[data-assignee]');
  if (staffButton) staffButton.onclick = async () => { const item = await chooseStaff(); if (item && ctx.current()) ctx.go(route({ assigneeId: item.id, assigneeName: item.name, page: null })); };
}

function eventText(event) {
  switch (event.eventType) {
    case 'CREATED': return 'opened this request';
    case 'ASSIGNED': return `assigned this request to staff #${event.newValue}`;
    case 'REASSIGNED': return `changed the assignment from staff #${event.previousValue} to #${event.newValue}`;
    case 'STATUS_CHANGED': return `moved this request to ${titleCase(event.newValue).toLowerCase()}`;
    case 'REOPENED': return 'reopened this request';
    case 'COMMENT_CREATED': return 'added a comment';
    default: return 'updated this request';
  }
}

export async function ticketDetail(ctx, id) {
  const ticket = await ctx.get(`/tickets/${id}`);
  if (!ctx.current()) return;
  const admin = ctx.user.role === 'ADMIN';
  const staff = ctx.user.role === 'STAFF' && ticket.assignee?.id === ctx.user.id;
  const owner = ctx.user.role === 'STUDENT' && ticket.creator.id === ctx.user.id;
  const closed = ticket.status === 'CLOSED';
  const action = ticket.status === 'OPEN' && (admin || staff) ? '<button class="btn primary" data-progress>Start working</button>' : ticket.status === 'IN_PROGRESS' && (admin || staff) ? `<button class="btn primary" data-resolve>${icon('check')}Resolve request</button>` : ticket.status === 'RESOLVED' && (admin || owner) ? `<button class="btn" data-reopen>${icon('refresh')}Reopen</button><button class="btn primary" data-close-ticket>${icon('check')}Close request</button>` : '';
  ctx.content.innerHTML = `<a class="back-link" href="#/tickets">${icon('back')}Back to requests</a><div class="detail-heading"><div><div class="ticket-kicker"><span>${ticketCode(ticket.id)}</span>${badge(ticket.status)}</div><h1>${e(ticket.title)}</h1><p>Raised by <strong>${e(ticket.creator.name)}</strong> · ${date(ticket.createdAt, true)}</p></div><div class="heading-actions">${action}</div></div><div class="detail-grid"><div><section class="card description-card"><h2>The request</h2><p class="body-text">${e(ticket.description)}</p>${ticket.resolutionSummary ? `<div class="resolution-box"><span>${icon('check')}Resolution</span><p class="body-text">${e(ticket.resolutionSummary)}</p><small>Resolved ${date(ticket.resolvedAt, true)}</small></div>` : ''}</section><section class="card activity-card"><nav class="activity-tabs" aria-label="Request activity"><button class="selected" data-tab="comments">${icon('message')}Conversation</button><button data-tab="events">${icon('clock')}History</button></nav><div id="activity-list"></div><div id="activity-pager"></div>${closed ? `<div class="closed-note">${icon('check')}This request is closed. Its conversation and history are kept here.</div>` : `<form class="comment-form" id="comment-form">${formNotice}<label for="comment-body">Add to the conversation</label><textarea name="body" id="comment-body" required maxlength="2000" rows="3" placeholder="Share an update or ask a question…"></textarea><div class="comment-actions"><span class="muted">Visible to this request’s participants</span><button class="btn primary small" type="submit">Send comment${icon('arrow')}</button></div></form>`}</section></div><aside><section class="card details-card"><h2>At a glance</h2><dl><div><dt>Service category</dt><dd><a href="#/tickets${e(query({ categoryId: ticket.categoryId, categoryName: ticket.categoryName }))}">${icon('layers')}${e(ticket.categoryName)}</a></dd></div><div><dt>Priority</dt><dd>${badge(ticket.priority, 'priority')}</dd></div><div><dt>Assigned to</dt><dd>${ticket.assignee ? `<span class="person-inline"><span class="avatar small">${e(initials(ticket.assignee.name))}</span>${e(ticket.assignee.name)}</span>` : '<span class="muted">Awaiting assignment</span>'}</dd>${admin && !closed ? `<button class="btn small wide" data-assign>${icon('users')}${ticket.assignee ? 'Change assignee' : 'Assign staff'}</button>` : ''}</div><div><dt>Last updated</dt><dd>${date(ticket.updatedAt, true)}</dd></div>${ticket.closedAt ? `<div><dt>Closed</dt><dd>${date(ticket.closedAt, true)}</dd></div>` : ''}</dl></section><div class="detail-tip">${icon('leaf')}<p>${closed ? 'One less thing to worry about. Thanks for helping make campus better.' : 'A clear description and a helpful update can make all the difference.'}</p></div></aside></div>`;
  let activityGeneration = 0, tab = 'comments';
  const loadActivity = async (page = 0) => {
    const generation = ++activityGeneration;
    const list = ctx.content.querySelector('#activity-list');
    list.innerHTML = '<div class="loading"><span class="spinner"></span>Loading activity…</div>';
    try {
      const result = await ctx.get(`/tickets/${id}/${tab}${query({ page, size: 10 })}`);
      if (generation !== activityGeneration || !ctx.current()) return;
      list.innerHTML = result.content.length ? (tab === 'comments' ? `<div class="comments-list">${result.content.map(c => `<article class="comment"><span class="avatar">${e(initials(c.author.name))}</span><div><div class="comment-meta"><strong>${e(c.author.name)}</strong><time>${date(c.createdAt, true)}</time></div><p class="body-text">${e(c.body)}</p></div></article>`).join('')}</div>` : `<ol class="timeline">${result.content.map(ev => `<li><span class="timeline-dot">${icon(ev.eventType === 'COMMENT_CREATED' ? 'message' : ev.eventType === 'CREATED' ? 'plus' : 'check')}</span><div><p><strong>${e(ev.actor.name)}</strong> ${e(eventText(ev))}</p><time>${date(ev.createdAt, true)}</time>${ev.reason ? `<blockquote>${e(ev.reason)}</blockquote>` : ''}</div></li>`).join('')}</ol>`) : empty(tab === 'comments' ? 'The conversation starts with you.' : 'No history to display.', tab === 'comments' ? 'Share an update or a little more detail below.' : 'Activity will appear as this request progresses.', '', tab === 'comments' ? 'message' : 'clock');
      ctx.content.querySelector('#activity-pager').replaceChildren(pager(result, loadActivity));
    } catch (error) { if (error.name !== 'AbortError' && ctx.current()) list.innerHTML = empty('Could not load activity', error.message); }
  };
  ctx.content.querySelectorAll('[data-tab]').forEach(button => { button.onclick = () => { tab = button.dataset.tab; ctx.content.querySelectorAll('[data-tab]').forEach(b => b.classList.toggle('selected', b === button)); loadActivity(); }; });
  const runChange = async (body, message) => {
    await patch(`/tickets/${id}/status`, { ...body, version: ticket.version });
    toast(message); ctx.refresh();
  };
  const immediate = (selector, body, message) => {
    const button = ctx.content.querySelector(selector);
    if (button) button.onclick = async () => {
      button.disabled = true;
      try { await runChange(body, message); }
      catch (error) { toast(error.status === 409 ? 'This request changed. Its latest details have been loaded; review them and try again.' : error.message, true); if (error.status === 409) ctx.refresh(); else button.disabled = false; }
    };
  };
  immediate('[data-progress]', { status: 'IN_PROGRESS' }, 'The request is now in progress.');
  const statusDialog = (kind) => {
    const resolve = kind === 'RESOLVED', reopen = kind === 'OPEN';
    const label = resolve ? 'Resolve request' : reopen ? 'Reopen request' : 'Close request';
    const field = resolve ? 'resolutionSummary' : 'reason';
    const dialog = modal(label, `<form class="stack-form">${formNotice}${kind === 'CLOSED' ? '<p>The work is complete and the issue is resolved. Closing keeps the history and ends further updates.</p>' : `<label>${resolve ? 'What was done to resolve the issue?' : 'What still needs attention?'}<textarea name="${field}" rows="4" required maxlength="2000" placeholder="${resolve ? 'Describe the fix and how you checked it.' : 'Explain why the request needs another look.'}"></textarea></label>`}${formButtons(label)}</form>`);
    let conflict = false;
    dialog.el.addEventListener('close', () => { if (conflict) ctx.refresh(); });
    wireForm(dialog.el.querySelector('form'), async values => {
      if (conflict) throw new Error('Close this dialog to reload the request before trying again.');
      try { await patch(`/tickets/${id}/status`, { status: kind, version: ticket.version, ...(kind === 'CLOSED' ? {} : { [field]: values[field].trim() }) }); }
      catch (error) { if (error.status === 409) { conflict = true; error.message += ' Close this dialog to reload the latest details.'; } throw error; }
      dialog.close(); toast(resolve ? 'Resolution shared. The owner can now confirm it.' : reopen ? 'Request reopened. The team will take another look.' : 'Request closed. Thanks for confirming.'); ctx.refresh();
    });
  };
  for (const [selector, kind] of [['[data-resolve]', 'RESOLVED'], ['[data-reopen]', 'OPEN'], ['[data-close-ticket]', 'CLOSED']]) {
    const button = ctx.content.querySelector(selector); if (button) button.onclick = () => statusDialog(kind);
  }
  const assign = ctx.content.querySelector('[data-assign]');
  if (assign) assign.onclick = async () => {
    const selected = await chooseStaff();
    if (!selected || !ctx.current()) return;
    if (selected.id === ticket.assignee?.id) { toast('This staff member is already assigned.'); return; }
    assign.disabled = true;
    try { await patch(`/tickets/${id}/assignment`, { assigneeId: selected.id, version: ticket.version }); toast(`Assigned to ${selected.name}.`); ctx.refresh(); }
    catch (error) { toast(error.message, true); if (error.status === 409) ctx.refresh(); else assign.disabled = false; }
  };
  const comment = ctx.content.querySelector('#comment-form');
  if (comment) wireForm(comment, async (values) => {
    try {
      await post(`/tickets/${id}/comments`, { body: values.body.trim(), version: ticket.version });
    } catch (error) {
      if (error.status === 409) {
        const latest = await api(`/tickets/${id}`);
        Object.assign(ticket, latest);
        if (latest.status === 'CLOSED') { comment.querySelector('textarea').disabled = true; toast('This request was closed. Your draft has been kept below.', true); }
        error.message += ' Latest details retrieved. Review the request before retrying.';
      }
      throw error;
    }
    comment.reset();
    Object.assign(ticket, await api(`/tickets/${id}`));
    toast('Comment shared.'); tab = 'comments';
    ctx.content.querySelectorAll('[data-tab]').forEach(b => b.classList.toggle('selected', b.dataset.tab === tab));
    loadActivity();
  });
  loadActivity();
}
