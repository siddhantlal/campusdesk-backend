import { api, post, patch, query } from './api.js';
import { e, icon, initials, titleCase, date, empty, pager, modal, wireForm, formNotice, formButtons, passwordField, checkPassword, toast } from './ui.js';

export function categoryForm(ctx, category = null) {
  const dialog = modal(category ? 'Edit service category' : 'A new place for support.', `<form class="stack-form">${formNotice}<label>Category name<input name="name" required maxlength="80" placeholder="For example, Library services" value="${e(category?.name)}"></label><label>Description<textarea name="description" required maxlength="500" rows="3" placeholder="What can students get help with here?">${e(category?.description)}</textarea></label>${category ? `<label class="checkbox-field"><input type="checkbox" name="active" ${category.active ? 'checked' : ''}><span>Available for new requests</span></label><p class="field-hint">Existing requests keep this category even when it’s inactive.</p>` : ''}${formButtons(category ? 'Save category' : 'Create category')}</form>`);
  let conflicted = false;
  dialog.el.addEventListener('close', () => { if (conflicted) ctx.refresh(); });
  wireForm(dialog.el.querySelector('form'), async values => {
    if (conflicted) throw new Error('Close this dialog to reload the latest category, then edit it again.');
    try {
      const body = { name: values.name.trim(), description: values.description.trim() };
      if (category) await patch(`/categories/${category.id}`, { ...body, active: values.active === 'on', version: category.version });
      else await post('/categories', body);
    } catch (error) { if (error.status === 409 && error.code === 'VERSION_CONFLICT') { conflicted = true; error.message += ' Close this dialog to reload.'; } throw error; }
    dialog.close(); toast(category ? 'Category updated.' : 'Your new service category is ready.'); ctx.refresh();
  });
}

export async function categoriesView(ctx) {
  const admin = ctx.user.role === 'ADMIN';
  const page = Math.max(0, Number(ctx.params.get('page')) || 0);
  const result = await ctx.get(`/categories${query({ page, size: 12, includeInactive: admin })}`);
  if (!ctx.current()) return;
  ctx.content.innerHTML = `<div class="page-heading"><div><span class="eyebrow">A PLACE FOR EVERY REQUEST</span><h1>Service categories<span class="heading-count">${result.totalElements}</span></h1><p>${admin ? 'Help students find the right people. Keep your service desks up to date.' : 'Find the right desk for your campus question or issue.'}</p></div>${admin ? `<button class="btn primary" data-create-category>${icon('plus')}Add category</button>` : ''}</div>${result.content.length ? `<section class="category-grid">${result.content.map((c, i) => `<article class="card category-card ${c.active ? '' : 'inactive-card'}"><div class="category-top"><span class="category-symbol tone-${i % 3}">${icon('layers')}</span><span class="badge ${c.active ? 'open' : 'inactive'}">${c.active ? 'Active' : 'Inactive'}</span></div><h2>${e(c.name)}</h2><p>${e(c.description)}</p><div class="category-bottom">${admin ? `<button class="text-link" data-edit="${c.id}">Edit category${icon('arrow')}</button>` : `<a class="text-link" href="#/tickets${e(query({ categoryId: c.id, categoryName: c.name }))}">View requests${icon('arrow')}</a>`}<small>Service desk</small></div></article>`).join('')}</section>` : `<section class="card">${empty('Your service desks start here.', admin ? 'Add a category so students can direct their requests to the right team.' : 'Your campus administrator hasn’t added an active category yet.', admin ? '<button class="btn primary" data-create-category>Add category</button>' : '', 'layers')}</section>`}<div id="category-pager"></div>`;
  ctx.content.querySelector('#category-pager').append(pager(result, next => ctx.go(`/categories${query({ page: next })}`)));
  ctx.content.querySelectorAll('[data-edit]').forEach(button => { button.onclick = () => categoryForm(ctx, result.content.find(c => String(c.id) === button.dataset.edit)); });
}

export function staffForm(ctx) {
  const dialog = modal('Someone new on the team.', `<form class="stack-form">${formNotice}<label>Full name<input name="name" autocomplete="name" required maxlength="100" placeholder="Staff member’s full name"></label><label>Email address<input name="email" type="email" autocomplete="email" required maxlength="254" placeholder="staff@yourcampus.edu"></label>${passwordField}<div class="info-note">${icon('users')}<span>This creates an active staff account. Share the login details privately with the staff member.</span></div>${formButtons('Create staff account')}</form>`);
  wireForm(dialog.el.querySelector('form'), async values => {
    checkPassword(values.password);
    await post('/admin/staff', { name: values.name.trim(), email: values.email.trim(), password: values.password });
    dialog.close(); toast('Staff account created.'); ctx.refresh();
  });
}

export async function peopleView(ctx) {
  if (ctx.user.role !== 'ADMIN') throw new Error('Only administrators can manage campus accounts.');
  const params = ctx.params;
  const filters = { page: Math.max(0, Number(params.get('page')) || 0), size: 15, search: params.get('search'), role: params.get('role'), active: params.get('active') };
  const result = await ctx.get(`/admin/users${query(filters)}`);
  if (!ctx.current()) return;
  const route = changes => `/people${query({ ...Object.fromEntries(params), ...changes })}`;
  ctx.content.innerHTML = `<div class="page-heading"><div><span class="eyebrow">PEOPLE MAKE THE CAMPUS</span><h1>Campus accounts<span class="heading-count">${result.totalElements}</span></h1><p>Welcome staff to the team and manage account access.</p></div><button class="btn primary" data-create-staff>${icon('plus')}Add staff member</button></div><section class="card"><form class="filter-bar" id="people-filters"><label class="search-field">${icon('search')}<span class="sr-only">Search accounts</span><input name="search" placeholder="Search by name or email…" maxlength="100" value="${e(filters.search)}"></label><label><span class="sr-only">Account role</span><select name="role"><option value="">All roles</option>${['STUDENT', 'STAFF', 'ADMIN'].map(role => `<option value="${role}" ${role === filters.role ? 'selected' : ''}>${titleCase(role)}</option>`).join('')}</select></label><label><span class="sr-only">Account status</span><select name="active"><option value="">All accounts</option><option value="true" ${filters.active === 'true' ? 'selected' : ''}>Active</option><option value="false" ${filters.active === 'false' ? 'selected' : ''}>Inactive</option></select></label><button type="submit" class="btn">Search</button>${[filters.search, filters.role, filters.active].some(Boolean) ? '<a class="text-link" href="#/people">Reset</a>' : ''}</form>${result.content.length ? `<div class="table-scroll"><table class="people-table"><thead><tr><th>Person</th><th>Role</th><th>Access</th><th>Joined</th><th>Manage</th></tr></thead><tbody>${result.content.map(user => `<tr><td><div class="person-inline"><span class="avatar">${e(initials(user.name))}</span><span class="person-name"><strong>${e(user.name)}${user.id === ctx.user.id ? '<small class="you-label">You</small>' : ''}</strong><small>${e(user.email)}</small></span></div></td><td><span class="role-tag">${titleCase(user.role)}</span></td><td><span class="badge ${user.active ? 'open' : 'inactive'}"><span class="badge-dot"></span>${user.active ? 'Active' : 'Inactive'}</span></td><td class="muted nowrap">${date(user.createdAt)}</td><td><button class="btn small ${user.active ? '' : 'primary'}" data-toggle-user="${user.id}" ${user.id === ctx.user.id ? 'disabled title="You cannot deactivate your own account"' : ''}>${user.active ? 'Deactivate' : 'Activate'}</button></td></tr>`).join('')}</tbody></table></div>` : empty('No matching accounts.', 'Try a different name, role, or access filter.', '', 'users')}<div id="people-pager"></div></section><div class="info-note people-note">${icon('help')}<span>Deactivated accounts lose access. Existing requests and history are preserved; reassign a staff member’s requests if needed.</span></div>`;
  const form = ctx.content.querySelector('#people-filters');
  form.onsubmit = event => { event.preventDefault(); ctx.go(route({ ...Object.fromEntries(new FormData(form)), page: 0 })); };
  form.querySelectorAll('select').forEach(select => { select.onchange = () => form.requestSubmit(); });
  ctx.content.querySelector('#people-pager').append(pager(result, next => ctx.go(route({ page: next }))));
  ctx.content.querySelectorAll('[data-toggle-user]').forEach(button => { button.onclick = () => {
    const user = result.content.find(u => String(u.id) === button.dataset.toggleUser);
    const action = user.active ? 'Deactivate' : 'Activate';
    const dialog = modal(`${action} ${user.name}?`, `<form class="stack-form">${formNotice}<p>${user.active ? 'This account will no longer be able to sign in or use CampusDesk. Their existing requests and history will be kept.' : 'This account will be able to sign in and use CampusDesk again.'}</p>${formButtons(`${action} account`)}</form>`);
    let conflict = false;
    dialog.el.addEventListener('close', () => { if (conflict) ctx.refresh(); });
    wireForm(dialog.el.querySelector('form'), async () => {
      if (conflict) throw new Error('Close this dialog to reload the current account status.');
      try { await patch(`/admin/users/${user.id}/status`, { active: !user.active, version: user.version }); }
      catch (error) { if (error.status === 409) { conflict = true; error.message += ' Close this dialog to reload.'; } throw error; }
      dialog.close(); toast(`Account ${user.active ? 'deactivated' : 'activated'}.`); ctx.refresh();
    });
  }; });
}
