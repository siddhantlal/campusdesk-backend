// Optional local-only smoke check. Reuses installed Chrome/Edge; never touches Render.
import { readFile, mkdir, access } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import assert from 'node:assert/strict';
import { chromium } from '../.tools/ui-check/node_modules/playwright/index.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const config = Object.fromEntries((await readFile(path.join(root, '.local/local.env'), 'utf8')).split(/\r?\n/).filter(line => line.includes('=')).map(line => {
  const split = line.indexOf('='); return [line.slice(0, split), line.slice(split + 1)];
}));
const base = process.env.FRONTEND_CHECK_URL || 'http://localhost:8080';
assert(['localhost', '127.0.0.1', '[::1]'].includes(new URL(base).hostname), 'Use a local development server only');
let ready = false;
for (let attempt = 0; attempt < 60; attempt++) {
  try { if ((await fetch(`${base}/actuator/health`)).ok) { ready = true; break; } } catch {}
  await new Promise(resolve => setTimeout(resolve, 500));
}
assert(ready, 'Start the local CampusDesk application before running the browser check');
const candidates = [process.env.BROWSER_EXECUTABLE, 'C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].filter(Boolean);
let executablePath;
for (const candidate of candidates) { try { await access(candidate); executablePath = candidate; break; } catch {} }
const browser = await chromium.launch({ headless: true, ...(executablePath ? { executablePath } : {}) });
const errors = [];
const screenshotDir = path.join(root, '.local/frontend-check');
await mkdir(screenshotDir, { recursive: true });
async function newPage(viewport = { width: 1440, height: 1000 }) {
  const context = await browser.newContext({ viewport });
  const page = await context.newPage();
  page.on('pageerror', error => errors.push(error.message));
  page.setDefaultTimeout(15000);
  return page;
}
async function login(page, email, password) {
  await page.goto(`${base}/#/signin`);
  await page.getByLabel('Email address', { exact: true }).fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in to CampusDesk' }).click();
  await page.getByRole('heading', { name: /Good to see you/ }).waitFor();
}
const activeDialog = page => page.getByRole('dialog').last();
async function resolve(page, summary) {
  await page.getByRole('button', { name: 'Resolve request', exact: true }).click();
  await activeDialog(page).getByRole('textbox').fill(summary);
  await activeDialog(page).getByRole('button', { name: 'Resolve request', exact: true }).click();
  await page.locator('.ticket-kicker .resolved').waitFor();
}
const suffix = randomUUID().slice(0, 8);
const password = randomUUID();
const studentEmail = `ui.student.${suffix}@campusdesk.test`;
const staffEmail = `ui.staff.${suffix}@campusdesk.test`;
const staffName = `UI Staff ${suffix}`;
const categoryName = `UI Facilities ${suffix}`;
const requestTitle = `Library fan check ${suffix}`;
let admin, student, staff;
try {
  admin = await newPage();
  await admin.goto(base);
  await admin.getByRole('heading', { name: /Let’s pick up/ }).waitFor();
  await admin.screenshot({ path: path.join(screenshotDir, 'sign-in.png'), fullPage: true });
  await login(admin, config.ADMIN_EMAIL, config.ADMIN_PASSWORD);
  await admin.screenshot({ path: path.join(screenshotDir, 'admin-overview.png'), fullPage: true });
  await admin.locator('.sidebar').getByRole('link', { name: 'Service categories', exact: true }).click();
  await admin.getByRole('button', { name: 'Add category', exact: true }).first().click();
  await activeDialog(admin).getByLabel('Category name').fill(categoryName);
  await activeDialog(admin).getByLabel('Description').fill('Synthetic browser workflow category');
  await activeDialog(admin).getByRole('button', { name: 'Create category' }).click();
  await admin.getByRole('heading', { name: categoryName, exact: true }).waitFor();
  await admin.locator('.sidebar').getByRole('link', { name: 'People & access' }).click();
  await admin.getByRole('button', { name: 'Add staff member' }).click();
  await activeDialog(admin).getByLabel('Full name').fill(staffName);
  await activeDialog(admin).getByLabel('Email address').fill(staffEmail);
  await activeDialog(admin).getByLabel(/^Password/).fill(password);
  await activeDialog(admin).getByRole('button', { name: 'Create staff account' }).click();
  await admin.getByRole('dialog').waitFor({ state: 'hidden' });
  await admin.getByPlaceholder('Search by name or email…').fill(staffEmail);
  await admin.locator('#people-filters').getByRole('button', { name: 'Search', exact: true }).click();
  await admin.getByText(staffEmail, { exact: true }).waitFor();

  student = await newPage();
  await student.goto(`${base}/#/register`);
  await student.getByLabel('Full name').fill(`UI Student ${suffix}`);
  await student.getByLabel('Email address').fill(studentEmail);
  await student.getByLabel(/^Password/).fill(password);
  await student.getByRole('button', { name: 'Create student account' }).click();
  await student.getByRole('heading', { name: /Good to see you/ }).waitFor();
  assert.equal(await student.locator('.sidebar').getByRole('link', { name: 'People & access' }).count(), 0);
  await student.locator('.dashboard-hero [data-new-ticket]').click();
  await activeDialog(student).getByLabel('What needs attention?').fill(requestTitle);
  await activeDialog(student).getByLabel('Tell us a little more').fill('Fan near desk 12. Literal text: <img src=x onerror=window.__xss=1>');
  await activeDialog(student).locator('[data-category]').click();
  await activeDialog(student).getByRole('button', { name: new RegExp(categoryName) }).click();
  await activeDialog(student).getByLabel('How urgent is it?').selectOption('HIGH');
  await activeDialog(student).getByRole('button', { name: 'Send request' }).click();
  await student.getByRole('heading', { name: requestTitle, exact: true }).waitFor();
  const id = student.url().match(/#\/tickets\/(\d+)/)?.[1];
  assert(id, 'Ticket detail route should contain an ID');
  assert.equal(await student.locator('.description-card img').count(), 0);
  assert.equal(await student.evaluate(() => window.__xss), undefined);

  await admin.goto(`${base}/#/tickets/${id}`);
  await admin.getByRole('button', { name: 'Assign staff', exact: true }).click();
  await activeDialog(admin).getByPlaceholder('Search by name or email').fill(staffName);
  await activeDialog(admin).getByRole('button', { name: 'Search', exact: true }).click();
  await activeDialog(admin).getByRole('button', { name: new RegExp(staffName) }).click();
  await admin.locator('.details-card .person-inline').filter({ hasText: staffName }).waitFor();

  staff = await newPage();
  await login(staff, staffEmail, password);
  assert.equal(await staff.locator('.sidebar [data-new-ticket]').count(), 0);
  await staff.goto(`${base}/#/tickets/${id}`);
  await staff.getByRole('button', { name: 'Start working', exact: true }).click();
  await staff.locator('.ticket-kicker .in_progress').waitFor();
  await staff.getByLabel('Add to the conversation').fill('Checking the capacitor and power supply.');
  await staff.getByRole('button', { name: 'Send comment' }).click();
  await staff.locator('.comment').getByText('Checking the capacitor and power supply.', { exact: true }).waitFor();
  await resolve(staff, 'Replaced the capacitor and checked the fan.');

  await student.reload();
  await student.getByRole('button', { name: 'Reopen', exact: true }).click();
  await activeDialog(student).getByRole('textbox').fill('It still stops after a few minutes.');
  await activeDialog(student).getByRole('button', { name: 'Reopen request', exact: true }).click();
  await student.locator('.ticket-kicker .open').waitFor();
  await staff.reload();
  await staff.getByRole('button', { name: 'Start working', exact: true }).click();
  await staff.locator('.ticket-kicker .in_progress').waitFor();
  await resolve(staff, 'Replaced the worn connection and tested continuously.');
  await student.reload();
  await student.getByRole('button', { name: 'Close request', exact: true }).click();
  await activeDialog(student).getByRole('button', { name: 'Close request', exact: true }).click();
  await student.locator('.ticket-kicker .closed').waitFor();
  assert.equal(await student.locator('#comment-form').count(), 0);
  await student.getByRole('button', { name: 'History', exact: true }).click();
  await student.locator('.timeline').getByText(/reopened this request/).waitFor();
  await student.screenshot({ path: path.join(screenshotDir, 'student-request.png'), fullPage: true });

  await student.setViewportSize({ width: 390, height: 844 });
  await student.reload();
  await student.getByRole('heading', { name: requestTitle, exact: true }).waitFor();
  assert.equal(await student.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true, 'Mobile page should not overflow horizontally');
  await student.getByRole('button', { name: 'Open navigation' }).click();
  assert.equal(await student.locator('.app-shell.nav-open').count(), 1);
  await student.locator('.sidebar').getByRole('link', { name: 'Overview', exact: true }).click();
  await student.getByRole('heading', { name: /Good to see you/ }).waitFor();
  await student.screenshot({ path: path.join(screenshotDir, 'mobile-overview.png'), fullPage: true });
  await student.reload();
  await student.getByRole('heading', { name: /Good to see you/ }).waitFor();
  assert.equal(await student.evaluate(() => Boolean(sessionStorage.getItem('campusdesk.access-token'))), true);
  await student.getByRole('button', { name: 'Open navigation' }).click();
  await student.getByRole('button', { name: 'Sign out', exact: true }).click();
  await student.getByRole('heading', { name: /Let’s pick up/ }).waitFor();
  assert.equal(await student.evaluate(() => sessionStorage.getItem('campusdesk.access-token')), null);
  assert.deepEqual(errors, [], 'Browser should not raise unhandled JavaScript errors');
  console.log(`Frontend workflow passed: registration, category/staff creation, ticket ${id}, assignment, comment, resolve, reopen, close, history, mobile layout/menu and tab session.`);
  console.log('Screenshots: .local/frontend-check/ (local synthetic development data retained).');
} catch (error) {
  for (const [name, page] of [['admin', admin], ['student', student], ['staff', staff]]) {
    if (page) await page.screenshot({ path: path.join(screenshotDir, `failure-${name}.png`), fullPage: true }).catch(() => {});
  }
  throw error;
} finally { await browser.close(); }
