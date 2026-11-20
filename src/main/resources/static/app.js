const navigationItems = [
  ["dashboard", "Overview", "⌂"],
  ["groceries", "Grocery", "◇"],
  ["bills", "Bills", "▤"],
  ["analytics", "Expenses & Budgets", "◔"],
  ["expenses", "Expense Records", "↗"],
  ["documents", "Documents", "▧"],
  ["medicines", "Medicines", "✚"],
  ["tasks", "Tasks", "✓"],
  ["calendar", "Calendar", "▦"],
  ["search", "Global Search", "⌕"],
  ["reminders", "Reminders", "◷"],
  ["backup", "Backup & Restore", "⇧"],
  ["notifications", "Notification Settings", "◉"],
  ["algorithms", "ADA Algorithm Lab", "⌘"],
  ["reports", "Reports & Activity", "▥"]
];
const moduleKeys = ["groceries", "bills", "expenses", "documents", "medicines", "tasks", "calendar"];
const content = document.querySelector("#content");
const pageTitle = document.querySelector("#page-title");
const recordDialog = document.querySelector("#record-dialog");
const recordForm = document.querySelector("#record-form");
const recordFields = document.querySelector("#record-fields");
const notice = document.querySelector("#notice");
const appShell = document.querySelector("#app-shell");
const authScreen = document.querySelector("#auth-screen");
const authForm = document.querySelector("#auth-form");
const authMessage = document.querySelector("#auth-message");
let bootstrap = null;
let activeView = "dashboard";
let calendarMonth = new Date();
let toastTimer;
let reminderTimer = null;
let authMode = "login";
const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, char => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
}[char]));
const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const nice = value => String(value ?? "").replaceAll("_", " ");

function readCookie(name) {
  const cookie = document.cookie.split("; ").find(item => item.startsWith(`${name}=`));
  return cookie ? decodeURIComponent(cookie.slice(name.length + 1)) : "";
}

async function api(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = { ...(options.body ? { "Content-Type": "application/json" } : {}), ...options.headers };
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    let token = readCookie("XSRF-TOKEN");
    if (!token) {
      const csrfResponse = await fetch("/api/auth/csrf", { credentials: "same-origin" });
      if (!csrfResponse.ok) throw new Error("Could not establish a secure session.");
      token = readCookie("XSRF-TOKEN");
    }
    if (!token) throw new Error("Could not establish a secure session.");
    headers["X-XSRF-TOKEN"] = token;
  }
  const response = await fetch(`/api${path}`, {
    ...options,
    method,
    credentials: "same-origin",
    headers
  });
  const type = response.headers.get("content-type") || "";
  const body = type.includes("application/json") ? await response.json() : await response.text();
  if (!response.ok) {
    const message = body?.error || body?.detail || body?.message || `Request failed (${response.status})`;
    throw Object.assign(new Error(message), { status: response.status });
  }
  return body;
}

function showAuth(message = "", isError = false) {
  if (reminderTimer) {
    clearInterval(reminderTimer);
    reminderTimer = null;
  }
  authScreen.hidden = false;
  appShell.hidden = true;
  authMessage.textContent = message;
  authMessage.classList.toggle("show", Boolean(message));
  authMessage.classList.toggle("error", isError);
}

function setAuthMode(mode) {
  authMode = mode;
  const registering = mode === "register";
  document.querySelector("#auth-title").textContent = registering ? "Create your account" : "Sign in";
  document.querySelector("#auth-description").textContent = registering
    ? "Create a private workspace for your personal records."
    : "Sign in to open your private workspace.";
  document.querySelector("#auth-name-field").hidden = !registering;
  document.querySelector("#auth-name").required = registering;
  document.querySelector("#auth-password").autocomplete = registering ? "new-password" : "current-password";
  document.querySelector("#auth-submit").textContent = registering ? "Create account" : "Sign in";
  document.querySelector("#auth-switch-text").textContent = registering ? "Already have an account?" : "New here?";
  document.querySelector("#auth-toggle").textContent = registering ? "Sign in" : "Create an account";
  authMessage.textContent = "";
  authMessage.classList.remove("show", "error");
}

function showNotice(message, isError = false) {
  notice.textContent = message;
  notice.classList.toggle("error", isError);
  notice.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => notice.classList.remove("show"), 4200);
}

function showToast(message, isError = false) {
  document.querySelector(".toast")?.remove();
  const toast = document.createElement("div");
  toast.className = `toast${isError ? " error" : ""}`;
  toast.textContent = message;
  document.body.append(toast);
  setTimeout(() => toast.remove(), 4000);
}

function setPage(view, title) {
  activeView = view;
  pageTitle.textContent = title;
  document.querySelectorAll(".nav-link").forEach(link => link.classList.toggle("active", link.dataset.view === view));
}

function renderNavigation() {
  const nav = document.querySelector("#navigation");
  nav.innerHTML = navigationItems.map(([key, title, icon]) =>
    `<button class="nav-link" data-view="${key}"><span class="nav-icon">${icon}</span>${escapeHtml(title)}</button>`
  ).join("");
  nav.addEventListener("click", event => {
    const button = event.target.closest("[data-view]");
    if (button) openView(button.dataset.view);
  });
  document.querySelector(".brand").addEventListener("click", event => {
    event.preventDefault();
    openView("dashboard");
  });
}

function intro(title, subtitle, action = "") {
  return `<div class="page-intro"><div><h2>${escapeHtml(title)}</h2><p>${escapeHtml(subtitle)}</p></div>${action}</div>`;
}

function emptyState(message) {
  return `<div class="empty-state">${escapeHtml(message)}</div>`;
}

async function openView(view) {
  const item = navigationItems.find(([key]) => key === view);
  setPage(view, item?.[1] || "Life Organizer");
  content.innerHTML = `<div class="panel">${emptyState("Loading your workspace…")}</div>`;
  try {
    if (view === "dashboard") return await renderDashboard();
    if (moduleKeys.includes(view) && view !== "calendar") return await renderModule(view);
    if (view === "analytics") return await renderAnalytics();
    if (view === "calendar") return await renderCalendar();
    if (view === "search") return renderSearch();
    if (view === "reminders") return await renderReminders();
    if (view === "backup") return renderBackup();
    if (view === "notifications") return renderNotifications();
    if (view === "algorithms") return renderAlgorithms();
    if (view === "reports") return await renderReports();
  } catch (error) {
    if (error.status === 401) {
      showAuth("Your session ended. Sign in again.");
      return;
    }
    content.innerHTML = `<div class="panel"><h3>Could not load this page</h3><p class="muted">${escapeHtml(error.message)}</p><p class="footer-note">Check MySQL settings in the terminal and confirm the database upgrade has been applied.</p></div>`;
  }
}

async function renderDashboard() {
  const [data, reminderData] = await Promise.all([api("/dashboard"), api("/reminders")]);
  const stats = [
    ["Grocery items", data.groceryCount, "in your list", "◇"],
    ["Purchased", data.purchasedCount, "grocery items", "✓"],
    ["Unpaid bills", data.unpaidBills, "need attention", "▤"],
    ["Total expenses", money(data.totalExpenses), "all recorded time", "↗"],
    ["Active tasks", data.activeTasks, "not completed", "☷"],
    ["High priority", data.highPriorityTasks, "priority 1–2", "↑"],
    ["Overdue tasks", data.overdueTasks, "past their deadline", "◷"],
    ["Upcoming events", data.upcomingEvents, "from today onward", "▦"]
  ];
  const quick = [["tasks", "Add a task", "✓"], ["bills", "Track a bill", "▤"], ["calendar", "Plan an event", "▦"], ["expenses", "Add an expense", "↗"]];
  const reminders = reminderData.upcoming.slice(0, 6);
  content.innerHTML = `
    <div class="page-intro"><div><h2>Welcome back${bootstrap?.user?.name ? `, ${escapeHtml(bootstrap.user.name)}` : ""}</h2><p>A calm, clear overview of your personal workspace.</p></div>
      <div class="toolbar-right"><button class="button secondary" data-action="refresh">↻ Refresh</button><button class="button primary" data-action="demo">＋ Add demo data</button></div></div>
    <div class="cards">${stats.map(([label, value, foot, icon]) => `<article class="stat-card"><div class="stat-card-top">${escapeHtml(label)}<span class="stat-icon">${icon}</span></div><div class="stat-value">${escapeHtml(value)}</div><div class="stat-foot">${escapeHtml(foot)}</div></article>`).join("")}</div>
    <div class="dashboard-grid">
      <section class="panel"><div class="panel-heading"><h3>Quick actions</h3><span class="muted">Pick up where you left off</span></div>
        <div class="quick-grid">${quick.map(([view, label, icon]) => `<a class="quick-link" href="#" data-open="${view}"><span class="nav-icon">${icon}</span>${label}<span class="muted" style="margin-left:auto">→</span></a>`).join("")}</div>
      </section>
      <section class="panel"><div class="panel-heading"><h3>Coming up</h3><button class="text-link" data-open="reminders">All reminders →</button></div>
        ${reminders.length ? reminders.map(item => `<div class="reminder-row"><div><strong>${escapeHtml(item.label)}</strong><small>${escapeHtml(nice(item.module))} · ${escapeHtml(item.when)}</small></div><span class="badge">${escapeHtml(item.when)}</span></div>`).join("") : emptyState("Nothing upcoming yet.")}
      </section>
    </div>`;
  content.querySelector('[data-action="refresh"]').addEventListener("click", renderDashboard);
  content.querySelector('[data-action="demo"]').addEventListener("click", async event => {
    const button = event.currentTarget;
    button.disabled = true;
    try {
      const result = await api("/demo-data", { method: "POST" });
      showToast(result.message);
      await renderDashboard();
    } catch (error) {
      button.disabled = false;
      showToast(error.message, true);
    }
  });
  content.querySelectorAll("[data-open]").forEach(link => link.addEventListener("click", event => {
    event.preventDefault();
    openView(link.dataset.open);
  }));
}

async function renderModule(key) {
  const module = bootstrap.modules.find(item => item.key === key);
  if (!module) throw new Error("This module is not available.");
  const rows = await api(`/records/${key}`);
  const title = module.title;
  content.innerHTML = `
    ${intro(title, `${rows.length} record${rows.length === 1 ? "" : "s"} · Search, edit, export, and manage your records.`,
      `<button class="button primary" data-action="add">＋ Add record</button>`)}
    <div class="toolbar"><div class="toolbar-left"><input class="input search-input" id="record-search" type="search" placeholder="Search visible records…"><select class="select" id="record-sort"><option value="new">Newest first</option><option value="az">Name A–Z</option><option value="za">Name Z–A</option></select></div>
      <div class="toolbar-right"><button class="button secondary" data-action="export">Export CSV</button><button class="button secondary" data-action="refresh">↻ Refresh</button></div></div>
    <div id="record-table"></div>`;
  content.querySelector('[data-action="add"]').addEventListener("click", () => openRecordDialog(module));
  content.querySelector('[data-action="refresh"]').addEventListener("click", () => renderModule(key));
  content.querySelector('[data-action="export"]').addEventListener("click", () => exportCsv(module, rows));
  const search = content.querySelector("#record-search");
  const sort = content.querySelector("#record-sort");
  const renderRows = () => {
    let visible = rows.filter(row => JSON.stringify(row).toLowerCase().includes(search.value.toLowerCase()));
    if (sort.value !== "new") {
      const field = module.fields[0].name;
      visible = [...visible].sort((a, b) => String(a[field] || "").localeCompare(String(b[field] || "")));
      if (sort.value === "za") visible.reverse();
    }
    const heading = module.fields.map(field => `<th>${escapeHtml(field.label)}</th>`).join("");
    const body = visible.map(row => `<tr><td class="muted">#${escapeHtml(row.id)}</td>${module.fields.map(field => `<td>${formatCell(row[field.name], field.name)}</td>`).join("")}
      <td><div class="table-actions"><button class="button secondary small-button" data-edit="${escapeHtml(row.id)}">Edit</button><button class="button danger small-button" data-delete="${escapeHtml(row.id)}">Delete</button></div></td></tr>`).join("");
    content.querySelector("#record-table").innerHTML = `<div class="table-panel table-scroll"><table><thead><tr><th>ID</th>${heading}<th>Actions</th></tr></thead><tbody>${body || `<tr><td colspan="${module.fields.length + 2}">${emptyState("No records match. Add your first record to get started.")}</td></tr>`}</tbody></table></div>`;
    content.querySelectorAll("[data-edit]").forEach(button => button.addEventListener("click", () => openRecordDialog(module, rows.find(row => String(row.id) === button.dataset.edit))));
    content.querySelectorAll("[data-delete]").forEach(button => button.addEventListener("click", async () => {
      const row = rows.find(item => String(item.id) === button.dataset.delete);
      if (!row || !confirm(`Delete this ${module.title.toLowerCase()} record?`)) return;
      try {
        await api(`/records/${key}/${encodeURIComponent(row.id)}`, { method: "DELETE" });
        showToast("Record deleted.");
        await renderModule(key);
      } catch (error) { showToast(error.message, true); }
    }));
  };
  search.addEventListener("input", renderRows);
  sort.addEventListener("change", renderRows);
  renderRows();
}

function formatCell(value, field) {
  if (value === null || value === undefined || value === "") return `<span class="muted">—</span>`;
  const text = escapeHtml(value);
  if (field === "status" || field === "payment_status" || field === "purchased_status") {
    const klass = String(value).toLowerCase().includes("completed") || String(value).toLowerCase().includes("paid") || String(value).toLowerCase().includes("purchased") ? "good" : "";
    return `<span class="badge ${klass}">${text}</span>`;
  }
  if (field === "amount" || field === "budget_amount") return money(value);
  return text;
}

function openRecordDialog(module, row = null) {
  document.querySelector("#record-dialog-title").textContent = `${row ? "Edit" : "Add"} ${module.title.toLowerCase()} record`;
  recordFields.innerHTML = module.fields.map(field => {
    const value = row?.[field.name] ?? "";
    const type = field.type === "select" ? "select" : field.type;
    const fieldHtml = type === "select"
      ? `<select class="select" id="field-${field.name}" ${field.required ? "required" : ""}>${field.options.map(option => `<option ${String(value) === option ? "selected" : ""}>${escapeHtml(option)}</option>`).join("")}</select>`
      : field.name === "notes" || field.name === "description" || field.name === "file_path"
        ? `<textarea class="input" id="field-${field.name}" ${field.required ? "required" : ""}>${escapeHtml(value)}</textarea>`
        : `<input class="input" id="field-${field.name}" type="${type === "integer" ? "number" : type}" ${type === "integer" ? 'step="1"' : ""} ${field.name === "priority" ? 'min="1" max="5"' : ""} ${field.required ? "required" : ""} value="${escapeHtml(value)}">`;
    return `<div class="field ${field.name === "notes" || field.name === "description" ? "full" : ""}"><label for="field-${field.name}">${escapeHtml(field.label)}${field.required ? " *" : ""}</label>${fieldHtml}</div>`;
  }).join("");
  recordForm.onsubmit = async event => {
    event.preventDefault();
    if (event.submitter?.value !== "save") { recordDialog.close(); return; }
    const data = Object.fromEntries(module.fields.map(field => [field.name, recordFields.querySelector(`#field-${CSS.escape(field.name)}`).value]));
    if (row) data.id = row.id;
    try {
      await api(`/records/${module.key}`, { method: "POST", body: JSON.stringify(data) });
      recordDialog.close();
      showToast(row ? "Record updated." : "Record added.");
      await renderModule(module.key);
    } catch (error) { showToast(error.message, true); }
  };
  recordDialog.showModal();
}

function exportCsv(module, rows) {
  const columns = ["id", ...module.fields.map(field => field.name)];
  const cell = value => `"${String(value ?? "").replaceAll('"', '""')}"`;
  const csv = [columns.map(cell).join(","), ...rows.map(row => columns.map(column => cell(row[column])).join(","))].join("\r\n");
  download(`${module.key}.csv`, new Blob(["\ufeff", csv], { type: "text/csv;charset=utf-8" }));
}

function download(filename, blob) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

async function renderCalendar() {
  const month = calendarMonth.toISOString().slice(0, 7);
  const data = await api(`/calendar?month=${encodeURIComponent(month)}`);
  const first = new Date(`${month}-01T12:00:00`);
  const start = (first.getDay() + 6) % 7;
  const daysInMonth = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
  const byDate = new Map();
  for (const event of data.events) {
    const date = String(event.event_date).slice(0, 10);
    if (!byDate.has(date)) byDate.set(date, []);
    byDate.get(date).push(event);
  }
  const cells = [];
  for (let i = 0; i < start; i++) cells.push(`<div class="calendar-cell muted"></div>`);
  for (let day = 1; day <= daysInMonth; day++) {
    const key = `${month}-${String(day).padStart(2, "0")}`;
    const events = (byDate.get(key) || []).map(event => `<div class="calendar-event"><button data-event="${escapeHtml(event.id)}">${escapeHtml(event.event_title)}</button></div>`).join("");
    cells.push(`<div class="calendar-cell"><div class="calendar-date"><span>${day}</span>${events ? `<span class="badge good">${byDate.get(key).length}</span>` : ""}</div>${events}</div>`);
  }
  const module = bootstrap.modules.find(item => item.key === "calendar");
  content.innerHTML = `${intro("Calendar", "Plan events, dates, and reminders for the month.",
    `<button class="button primary" data-action="add">＋ Add event</button>`)}
    <section class="panel"><div class="toolbar"><div class="toolbar-left"><button class="button secondary" data-action="prev">← Previous</button><button class="button secondary" data-action="today">Today</button><button class="button secondary" data-action="next">Next →</button><strong>${escapeHtml(first.toLocaleString("en", { month: "long", year: "numeric" }))}</strong></div><span class="muted">${data.events.length} event(s)</span></div>
    <div class="calendar-head">${["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map(day => `<span>${day}</span>`).join("")}</div><div class="calendar-grid">${cells.join("")}</div></section>
    <div style="height:16px"></div><section class="panel"><div class="panel-heading"><h3>Events this month</h3></div>${data.events.length ? `<div class="table-scroll"><table><thead><tr><th>Date</th><th>Time</th><th>Event</th><th>Reminder</th><th></th></tr></thead><tbody>${data.events.map(event => `<tr><td>${escapeHtml(String(event.event_date).slice(0, 10))}</td><td>${escapeHtml(event.event_time || "All day")}</td><td>${escapeHtml(event.event_title)}</td><td>${escapeHtml(event.reminder_minutes)} min before</td><td><button class="button secondary small-button" data-event="${escapeHtml(event.id)}">Edit</button></td></tr>`).join("")}</tbody></table></div>` : emptyState("No events scheduled this month.")}</section>`;
  content.querySelector('[data-action="add"]').addEventListener("click", () => openRecordDialog(module));
  content.querySelector('[data-action="prev"]').addEventListener("click", () => { calendarMonth.setMonth(calendarMonth.getMonth() - 1); renderCalendar(); });
  content.querySelector('[data-action="next"]').addEventListener("click", () => { calendarMonth.setMonth(calendarMonth.getMonth() + 1); renderCalendar(); });
  content.querySelector('[data-action="today"]').addEventListener("click", () => { calendarMonth = new Date(); renderCalendar(); });
  content.querySelectorAll("[data-event]").forEach(button => button.addEventListener("click", () => {
    const event = data.events.find(item => String(item.id) === button.dataset.event);
    if (event) openRecordDialog(module, { ...event, event_date: String(event.event_date).slice(0, 10), event_time: String(event.event_time || "").slice(0, 5) });
  }));
}

function renderSearch() {
  content.innerHTML = `${intro("Global Search", "Search across tasks, bills, expenses, groceries, medicines, documents, and calendar events.")}
    <section class="panel"><form id="search-form" class="toolbar"><input id="global-query" class="input search-input" type="search" placeholder="Type a name, category, status, or date…" required><button class="button primary">Search</button></form><div id="search-results">${emptyState("Search your saved records.")}</div></section>`;
  const form = content.querySelector("#search-form");
  form.addEventListener("submit", async event => {
    event.preventDefault();
    const query = content.querySelector("#global-query").value.trim();
    try {
      const results = await api(`/search?q=${encodeURIComponent(query)}`);
      content.querySelector("#search-results").innerHTML = results.length
        ? `<div class="table-scroll"><table><thead><tr><th>Module</th><th>Record</th><th>ID</th><th></th></tr></thead><tbody>${results.map(item => `<tr><td>${escapeHtml(item.title)}</td><td>${escapeHtml(item.name)}</td><td>#${escapeHtml(item.id)}</td><td><button class="button secondary small-button" data-open-record="${escapeHtml(item.module)}">Open module</button></td></tr>`).join("")}</tbody></table></div>`
        : emptyState("No matching records found.");
      content.querySelectorAll("[data-open-record]").forEach(button => button.addEventListener("click", () => openView(button.dataset.openRecord)));
    } catch (error) { showToast(error.message, true); }
  });
}

async function renderAnalytics() {
  const data = await api("/analytics");
  const maxCategory = Math.max(1, ...data.categories.map(item => Number(item.total)));
  const maxMonth = Math.max(1, ...data.months.map(item => Number(item.total)));
  const barRows = (items, max, labelKey, valueKey) => items.map(item => `<div class="chart-row"><span>${escapeHtml(item[labelKey])}</span><div class="chart-track"><div class="chart-fill" style="width:${Math.max(1, Number(item[valueKey]) / max * 100)}%"></div></div><span class="chart-value">${money(item[valueKey])}</span></div>`).join("");
  content.innerHTML = `${intro("Expenses & Budgets", "Understand spending patterns and keep monthly categories on track.",
    `<button class="button primary" data-action="budget">＋ Set monthly budget</button>`)}
    <div class="two-column"><section class="chart-panel panel"><div class="panel-heading"><h3>Spending by category</h3><span class="badge">${escapeHtml(data.month)}</span></div>
      ${data.categories.length ? `<div class="chart-bars">${barRows(data.categories, maxCategory, "category", "total")}</div>` : emptyState("No expenses recorded this month.")}</section>
    <section class="chart-panel panel"><div class="panel-heading"><h3>Monthly spending</h3><span class="muted">Last six months</span></div><div class="chart-bars">${barRows(data.months, maxMonth, "month", "total")}</div></section></div>
    <div style="height:16px"></div><section class="panel"><div class="panel-heading"><h3>${escapeHtml(data.month)} budget status</h3><button class="text-link" data-open="expenses">View expense records →</button></div>
      ${data.budgets.length ? `<div class="table-scroll"><table><thead><tr><th>Category</th><th>Budget</th><th>Spent</th><th>Remaining</th><th>Status</th></tr></thead><tbody>${data.budgets.map(budget => {
        const remaining = Number(budget.budget) - Number(budget.spent);
        const over = remaining < 0;
        return `<tr><td>${escapeHtml(budget.category)}</td><td>${money(budget.budget)}</td><td>${money(budget.spent)}</td><td>${money(remaining)}</td><td><span class="badge ${over ? "bad" : "good"}">${over ? "Over budget" : "On track"}</span></td></tr>`;
      }).join("")}</tbody></table></div>` : emptyState("No budgets set for this month. Add a category budget to get started.")}</section>`;
  content.querySelector('[data-action="budget"]').addEventListener("click", () => openBudgetDialog());
  content.querySelector('[data-open="expenses"]').addEventListener("click", () => openView("expenses"));
}

function openBudgetDialog() {
  recordFields.innerHTML = `<div class="field"><label for="budget-category">Category *</label><input id="budget-category" class="input" required></div>
    <div class="field"><label for="budget-amount">Monthly amount (₹) *</label><input id="budget-amount" class="input" type="number" min="0" step="0.01" required></div>
    <div class="field"><label for="budget-month">Month *</label><input id="budget-month" class="input" type="month" value="${new Date().toISOString().slice(0, 7)}" required></div>`;
  document.querySelector("#record-dialog-title").textContent = "Set monthly budget";
  recordForm.onsubmit = async event => {
    event.preventDefault();
    if (event.submitter?.value !== "save") { recordDialog.close(); return; }
    try {
      await api("/budgets", { method: "POST", body: JSON.stringify({
        category: document.querySelector("#budget-category").value,
        amount: document.querySelector("#budget-amount").value,
        month: document.querySelector("#budget-month").value
      }) });
      recordDialog.close();
      showToast("Budget saved.");
      await renderAnalytics();
    } catch (error) { showToast(error.message, true); }
  };
  recordDialog.showModal();
}

async function renderReminders() {
  const data = await api("/reminders");
  content.innerHTML = `${intro("Reminders", "Upcoming dates plus snooze and completion history. Notifications run while this website is open.",
    `<button class="button secondary" data-action="refresh">↻ Refresh</button>`)}
    <div class="two-column"><section class="panel"><div class="panel-heading"><h3>Upcoming items</h3></div><div id="upcoming-list"></div></section>
    <section class="panel"><div class="panel-heading"><h3>Snooze & completion history</h3></div><div id="history-list"></div></section></div>
    <p class="footer-note">Keep this page open for timed browser notifications. Task and bill reminders are checked at 9:00 AM; medicine and event reminders use their saved times.</p>`;
  const upcoming = content.querySelector("#upcoming-list");
  upcoming.innerHTML = data.upcoming.length ? data.upcoming.map(item => `<div class="reminder-row"><div><strong>${escapeHtml(item.label)}</strong><small>${escapeHtml(nice(item.module))} · ${escapeHtml(item.when)}</small><small class="muted" style="display:block">${escapeHtml(item.key)}</small></div>
    <div class="reminder-actions"><button class="button secondary small-button" data-snooze="${escapeHtml(item.key)}">Snooze</button><button class="button secondary small-button" data-complete="${escapeHtml(item.key)}">Done</button><button class="button danger small-button" data-reset="${escapeHtml(item.key)}" aria-label="Reset">↺</button></div></div>`).join("") : emptyState("Nothing scheduled yet.");
  const history = content.querySelector("#history-list");
  history.innerHTML = data.history.length ? data.history.map(item => `<div class="reminder-row"><div><strong>${escapeHtml(item.reminder_key)}</strong><small>${escapeHtml(item.action_type)} · ${escapeHtml(item.updated_at)}</small></div><button class="button secondary small-button" data-reset="${escapeHtml(item.reminder_key)}">Reset</button></div>`).join("") : emptyState("No snoozed or completed reminders.");
  content.querySelector('[data-action="refresh"]').addEventListener("click", renderReminders);
  content.querySelectorAll("[data-snooze], [data-complete]").forEach(button => button.addEventListener("click", async () => {
    const key = button.dataset.snooze || button.dataset.complete;
    const action = button.dataset.snooze ? "SNOOZE" : "COMPLETED";
    try {
      const result = await api("/reminders/action", { method: "POST", body: JSON.stringify({ key, action }) });
      showToast(result.message);
      await renderReminders();
    } catch (error) { showToast(error.message, true); }
  }));
  content.querySelectorAll("[data-reset]").forEach(button => button.addEventListener("click", async () => {
    try {
      await api(`/reminders/action?key=${encodeURIComponent(button.dataset.reset)}`, { method: "DELETE" });
      showToast("Reminder state reset.");
      await renderReminders();
    } catch (error) { showToast(error.message, true); }
  }));
}

function renderNotifications() {
  const preferences = readPreferences();
  content.innerHTML = `${intro("Notification Settings", "Choose browser popups and sound separately for each reminder group.")}
    <section class="panel"><div class="toolbar"><div><h3 style="margin:0 0 5px">Browser notification permission</h3><span class="muted">Permission is requested only when you press the button.</span></div><button class="button primary" data-action="permission">Enable browser notifications</button></div>
    <div id="permission-status" class="footer-note"></div></section>
    <div style="height:15px"></div><section class="panel"><div class="panel-heading"><h3>Module preferences</h3><button class="button secondary small-button" data-action="test-sound">Test sound</button></div>
    <div class="toggle-list">${[
      ["medicines", "Medicines"], ["calendar", "Calendar"], ["tasks", "Tasks"], ["bills", "Bills"],
      ["expenses", "Expenses"], ["groceries", "Groceries"], ["documents", "Documents"]
    ].map(([key, label]) => `<div class="toggle-row"><strong>${label}</strong><div class="toggle-controls">
      <label><input type="checkbox" data-pref="${key}.popup" ${preferences[`${key}.popup`] !== false ? "checked" : ""}> Popup</label>
      <label><input type="checkbox" data-pref="${key}.sound" ${preferences[`${key}.sound`] !== false ? "checked" : ""}> Sound</label>
    </div></div>`).join("")}</div></section>
    <p class="footer-note">Preferences are saved in this browser on this computer. The website must be open for scheduled checks.</p>`;
  const permissionStatus = content.querySelector("#permission-status");
  const updatePermission = () => {
    permissionStatus.textContent = "Notification status: " + (("Notification" in window) ? Notification.permission : "not supported by this browser");
  };
  updatePermission();
  content.querySelector('[data-action="permission"]').addEventListener("click", async () => {
    if (!("Notification" in window)) return showToast("This browser does not support desktop notifications.", true);
    const result = await Notification.requestPermission();
    updatePermission();
    showToast(`Browser notification permission: ${result}`);
  });
  content.querySelectorAll("[data-pref]").forEach(input => input.addEventListener("change", () => {
    const current = readPreferences();
    current[input.dataset.pref] = input.checked;
    localStorage.setItem("lifeorganizer.notifications", JSON.stringify(current));
  }));
  content.querySelector('[data-action="test-sound"]').addEventListener("click", playSound);
}

function readPreferences() {
  try { return JSON.parse(localStorage.getItem("lifeorganizer.notifications") || "{}"); }
  catch { return {}; }
}

function playSound() {
  try {
    const context = new AudioContext();
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    oscillator.frequency.value = 740;
    gain.gain.value = 0.06;
    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.start();
    oscillator.stop(context.currentTime + 0.18);
    oscillator.onended = () => context.close();
  } catch { showToast("Sound could not be played by this browser.", true); }
}

async function pollReminders() {
  if (document.hidden) return;
  try {
    const due = await api("/reminders/due");
    const prefs = readPreferences();
    for (const item of due) {
      const signature = `${item.key}:${new Date().toISOString().slice(0, 16)}`;
      if (sessionStorage.getItem(signature)) continue;
      sessionStorage.setItem(signature, "shown");
      if (prefs[`${item.module}.sound`] !== false) playSound();
      if (prefs[`${item.module}.popup`] !== false) {
        if ("Notification" in window && Notification.permission === "granted") {
          new Notification("Life Organizer Reminder", { body: item.message });
        } else {
          showToast(item.message);
        }
      }
    }
  } catch (error) {
    console.warn("Reminder polling failed:", error.message);
    if (error.status === 401) showAuth("Your session ended. Sign in again.");
  }
}

function renderBackup() {
  content.innerHTML = `${intro("Backup & Restore", "Download a JSON backup of this account or restore its records from a previous backup.")}
    <div class="two-column"><section class="panel"><div class="panel-heading"><h3>Create a backup</h3></div><p class="muted">Includes personal records, budgets, reminders, and activity history. Passwords are never exported.</p><button class="button primary" data-action="create">Download backup JSON</button></section>
    <section class="panel"><div class="panel-heading"><h3>Restore a backup</h3></div><p class="muted">Restoring replaces this account's current application records. Keep a separate copy before continuing.</p>
    <div class="file-drop"><input id="backup-file" class="input" type="file" accept=".json,application/json"><div style="height:12px"></div><button class="button danger" data-action="restore">Restore selected backup</button></div></section></div>
    <p class="footer-note">Backups are scoped to the currently selected local account. Do not share the downloaded file; it may contain personal records.</p>`;
  content.querySelector('[data-action="create"]').addEventListener("click", async () => {
    try {
      const backup = await api("/backup");
      download(`life-organizer-backup-${new Date().toISOString().slice(0, 10)}.json`,
        new Blob([JSON.stringify(backup, null, 2)], { type: "application/json" }));
      showToast("Backup downloaded.");
    } catch (error) { showToast(error.message, true); }
  });
  content.querySelector('[data-action="restore"]').addEventListener("click", async () => {
    const file = content.querySelector("#backup-file").files[0];
    if (!file) return showToast("Choose a backup JSON file first.", true);
    if (!confirm("Restore will replace this account's application records. Continue?")) return;
    try {
      const backup = JSON.parse(await file.text());
      const result = await api("/backup/restore", { method: "POST", body: JSON.stringify(backup) });
      showToast(`${result.message} ${result.records} record(s) processed.`);
      await openView("dashboard");
    } catch (error) { showToast(error.message, true); }
  });
}

async function renderReports() {
  const data = await api("/reports");
  const section = (title, rows, columns) => `<section class="panel"><div class="panel-heading"><h3>${escapeHtml(title)}</h3></div>
    ${rows.length ? `<div class="table-scroll"><table><thead><tr>${columns.map(([, label]) => `<th>${escapeHtml(label)}</th>`).join("")}</tr></thead><tbody>${rows.map(row => `<tr>${columns.map(([key]) => `<td>${escapeHtml(row[key] ?? "—")}</td>`).join("")}</tr>`).join("")}</tbody></table></div>` : emptyState("No data to report yet.")}</section>`;
  content.innerHTML = `${intro("Reports & Activity", "A database-driven summary of spending, bills, tasks, and recent changes.",
    `<button class="button secondary" data-action="export">Export activity CSV</button><button class="button secondary" data-action="refresh">↻ Refresh</button>`)}
    <div class="two-column">${section("Expenses by category", data.expenses, [["category", "Category"], ["total", "Total"]])}
    ${section("Bills by status", data.bills, [["payment_status", "Status"], ["count", "Bills"], ["total", "Total"]])}
    ${section("Tasks by status", data.tasks, [["status", "Status"], ["count", "Tasks"]])}
    ${section("Recent activity", data.activity, [["created_at", "Time"], ["entity_name", "Module"], ["action_name", "Action"], ["details", "Details"]])}</div>`;
  content.querySelector('[data-action="refresh"]').addEventListener("click", renderReports);
  content.querySelector('[data-action="export"]').addEventListener("click", () => {
    const columns = ["created_at", "entity_name", "record_id", "action_name", "details"];
    const cell = value => `"${String(value ?? "").replaceAll('"', '""')}"`;
    const csv = [columns.map(cell).join(","), ...data.activity.map(row => columns.map(column => cell(row[column])).join(","))].join("\r\n");
    download("life-organizer-audit.csv", new Blob(["\ufeff", csv], { type: "text/csv;charset=utf-8" }));
  });
}

function renderAlgorithms() {
  const choices = ["Linear Search", "Binary Search", "Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort", "Priority Queue", "Task Scheduling", "0/1 Knapsack (Dynamic Programming)", "Fractional Knapsack (Greedy)", "Activity Selection (Greedy)"];
  content.innerHTML = `${intro("ADA Algorithm Lab", "Explore search, sorting, scheduling, and greedy/dynamic-programming examples.")}
    <section class="panel"><p class="muted">Enter a comma-separated list of whole numbers. For search algorithms, set the target. Knapsack and activity-selection examples use the built-in sample data.</p>
    <div class="toolbar"><input class="input" id="algorithm-input" style="flex:1;min-width:220px" value="8, 3, 6, 1, 9, 2" aria-label="Comma-separated integer input"><select class="select" id="algorithm-choice">${choices.map(item => `<option>${escapeHtml(item)}</option>`).join("")}</select>
    <input class="input" id="algorithm-target" type="number" value="6" style="width:100px" aria-label="Search target"><button class="button primary" data-action="run">Run algorithm</button></div>
    <div class="panel" style="background:#f8fafb"><pre class="algorithm-output" id="algorithm-output">Choose an algorithm and run it to see the result and complexity.</pre></div></section>`;
  content.querySelector('[data-action="run"]').addEventListener("click", runAlgorithm);
}

function runAlgorithm() {
  const output = content.querySelector("#algorithm-output");
  try {
    const name = content.querySelector("#algorithm-choice").value;
    let values = content.querySelector("#algorithm-input").value.split(",").map(value => value.trim()).filter(Boolean).map(Number);
    if (!values.length || values.some(value => !Number.isSafeInteger(value)) || values.length > 10000) throw new Error("Enter 1–10,000 whole numbers.");
    const before = [...values];
    const target = Number(content.querySelector("#algorithm-target").value);
    let result = "";
    let comparisons = 0;
    let complexity = "";
    const start = performance.now();
    if (name === "Linear Search") {
      const index = values.findIndex(value => { comparisons++; return value === target; });
      result = `Found index: ${index}`;
      complexity = "Best O(1), average/worst O(n); space O(1).";
    } else if (name === "Binary Search") {
      values.sort((a, b) => a - b);
      let low = 0, high = values.length - 1, index = -1;
      while (low <= high) {
        const middle = Math.floor((low + high) / 2);
        comparisons++;
        if (values[middle] === target) { index = middle; break; }
        if (values[middle] < target) low = middle + 1; else high = middle - 1;
      }
      result = `Sorted data: ${values}\nFound index: ${index}`;
      complexity = "Best O(1), average/worst O(log n); space O(1).";
    } else if (name.endsWith("Sort")) {
      if (name === "Bubble Sort") {
        for (let i = 0; i < values.length - 1; i++) {
          let changed = false;
          for (let j = 0; j < values.length - i - 1; j++) {
            comparisons++;
            if (values[j] > values[j + 1]) { [values[j], values[j + 1]] = [values[j + 1], values[j]]; changed = true; }
          }
          if (!changed) break;
        }
        complexity = "Best O(n) with early exit; average/worst O(n²); space O(1).";
      } else if (name === "Selection Sort") {
        for (let i = 0; i < values.length; i++) {
          let min = i;
          for (let j = i + 1; j < values.length; j++) { comparisons++; if (values[j] < values[min]) min = j; }
          [values[i], values[min]] = [values[min], values[i]];
        }
        complexity = "Best/average/worst O(n²); space O(1).";
      } else if (name === "Insertion Sort") {
        for (let i = 1; i < values.length; i++) {
          const current = values[i];
          let j = i - 1;
          while (j >= 0) { comparisons++; if (values[j] <= current) break; values[j + 1] = values[j]; j--; }
          values[j + 1] = current;
        }
        complexity = "Best O(n); average/worst O(n²); space O(1).";
      } else if (name === "Merge Sort") {
        const mergeSort = list => {
          if (list.length < 2) return list;
          const middle = Math.floor(list.length / 2);
          const left = mergeSort(list.slice(0, middle)), right = mergeSort(list.slice(middle));
          const merged = [];
          while (left.length && right.length) { comparisons++; merged.push(left[0] <= right[0] ? left.shift() : right.shift()); }
          return merged.concat(left, right);
        };
        values = mergeSort(values);
        complexity = "Best/average/worst O(n log n); extra space O(n).";
      } else {
        const quickSort = (list, low, high) => {
          if (low >= high) return;
          const pivot = list[Math.floor((low + high) / 2)];
          let i = low, j = high;
          while (i <= j) {
            while (list[i] < pivot) { i++; comparisons++; }
            while (list[j] > pivot) { j--; comparisons++; }
            if (i <= j) { [list[i], list[j]] = [list[j], list[i]]; i++; j--; comparisons++; }
          }
          quickSort(list, low, j); quickSort(list, i, high);
        };
        quickSort(values, 0, values.length - 1);
        complexity = "Average O(n log n), worst O(n²); average recursion space O(log n).";
      }
      result = values.toString();
    } else if (name === "Priority Queue") {
      result = `Ascending priority removal order: ${[...values].sort((a, b) => a - b)}`;
      complexity = "Insert/remove O(log n), peek O(1); storage O(n).";
    } else if (name === "Task Scheduling") {
      result = `Task order by priority (1 is highest): ${values.map((priority, index) => ({ priority, index: index + 1 })).sort((a, b) => a.priority - b.priority).map(task => `task#${task.index}[priority ${task.priority}]`).join(", ")}`;
      complexity = "Sort-based scheduling O(n log n); output space O(n).";
    } else if (name.startsWith("0/1 Knapsack")) {
      const weights = [2, 3, 4, 5], items = [3, 4, 5, 8], capacity = 8;
      const dp = Array.from({ length: weights.length + 1 }, () => Array(capacity + 1).fill(0));
      for (let i = 1; i <= weights.length; i++) for (let weight = 0; weight <= capacity; weight++) {
        dp[i][weight] = dp[i - 1][weight];
        if (weights[i - 1] <= weight) dp[i][weight] = Math.max(dp[i][weight], items[i - 1] + dp[i - 1][weight - weights[i - 1]]);
      }
      result = `Sample weights=${weights}, values=${items}, capacity=${capacity}\nMaximum value=${dp[weights.length][capacity]}\nDP table:\n${dp.map(row => row.join(" ")).join("\n")}`;
      complexity = "Time O(nW), space O(nW); pseudo-polynomial in capacity W.";
    } else if (name.startsWith("Fractional Knapsack")) {
      const weights = [10, 20, 30], items = [60, 100, 120];
      let capacity = 50, total = 0;
      const selected = [0, 1, 2].sort((a, b) => items[b] / weights[b] - items[a] / weights[a]).map(index => {
        const fraction = Math.min(1, capacity / weights[index]);
        capacity -= fraction * weights[index];
        total += fraction * items[index];
        return `item ${index + 1} fraction ${fraction}`;
      });
      result = `Selected: ${selected.join("; ")}\nMaximum value=${total}`;
      complexity = "Time O(n log n); space O(n). Greedy is optimal for fractional, not 0/1, knapsack.";
    } else {
      const starts = [1, 3, 0, 5, 8, 5], ends = [2, 4, 6, 7, 9, 9];
      const activities = starts.map((start, index) => ({ start, end: ends[index], id: index + 1 })).sort((a, b) => a.end - b.end);
      let last = -1;
      const chosen = activities.filter(activity => {
        if (activity.start < last) return false;
        last = activity.end;
        return true;
      });
      result = `Activities (start,end): ${chosen.map(activity => `A${activity.id}(${activity.start},${activity.end})`).join(", ")}`;
      complexity = "Time O(n log n) with sorting; O(n) if already sorted by finish time; space O(n).";
    }
    const elapsed = ((performance.now() - start) * 1e6).toFixed(0);
    output.textContent = `ALGORITHM: ${name}\nINPUT: ${before}\nOUTPUT: ${result}\nCOMPARISONS (where counted): ${comparisons}\nEXECUTION TIME: ${elapsed} ns\n\nCOMPLEXITY: ${complexity}\n\nNote: timings depend on this browser and input size.`;
  } catch (error) {
    output.textContent = error.message || "Invalid input.";
  }
}

async function loadWorkspace() {
  bootstrap = await api("/bootstrap");
  authScreen.hidden = true;
  appShell.hidden = false;
  document.querySelector("#user-name").textContent = bootstrap.user.name || bootstrap.user.email;
  await openView("dashboard");
  if (!reminderTimer) {
    pollReminders();
    reminderTimer = setInterval(pollReminders, 30000);
  }
}

async function initialize() {
  renderNavigation();
  const now = new Date();
  document.querySelector("#today-label").textContent = now.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
  document.querySelector("#auth-toggle").addEventListener("click", () =>
    setAuthMode(authMode === "login" ? "register" : "login"));
  authForm.addEventListener("submit", async event => {
    event.preventDefault();
    const email = document.querySelector("#auth-email").value.trim();
    const password = document.querySelector("#auth-password").value;
    const submit = document.querySelector("#auth-submit");
    submit.disabled = true;
    try {
      if (authMode === "register") {
        const name = document.querySelector("#auth-name").value.trim();
        await api("/auth/register", {
          method: "POST",
          body: JSON.stringify({ name, email, password })
        });
        document.querySelector("#auth-email").value = email;
        document.querySelector("#auth-password").value = "";
        setAuthMode("login");
        showAuth("Account created. Sign in to continue.");
      } else {
        await api("/auth/login", {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({ email, password })
        });
        await loadWorkspace();
      }
    } catch (error) {
      showAuth(error.message, true);
    } finally {
      submit.disabled = false;
    }
  });
  document.querySelector("#logout").addEventListener("click", async () => {
    try {
      await api("/auth/logout", { method: "POST" });
      bootstrap = null;
      showAuth("You have signed out.");
    } catch (error) {
      showToast(error.message, true);
    }
  });
  try {
    await loadWorkspace();
  } catch (error) {
    if (error.status === 401) {
      showAuth();
    } else {
      showAuth(error.message, true);
    }
  }
}

initialize();
