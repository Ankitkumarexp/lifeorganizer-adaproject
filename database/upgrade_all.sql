-- SAFE ADDITIVE UPGRADE: run against the selected database; keeps existing tables and records intact.

CREATE TABLE IF NOT EXISTS users (
 user_id INT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 email VARCHAR(190) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS groceries (
 grocery_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 item_name VARCHAR(120) NOT NULL, category VARCHAR(80) NOT NULL DEFAULT 'Other',
 quantity VARCHAR(50) NOT NULL DEFAULT '1', purchased_status ENUM('Pending','Purchased') NOT NULL DEFAULT 'Pending',
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_grocery_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 INDEX idx_grocery_user(user_id), INDEX idx_grocery_name(item_name)
);
CREATE TABLE IF NOT EXISTS bills (
 bill_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 bill_name VARCHAR(120) NOT NULL, amount DECIMAL(12,2) NOT NULL,
 due_date DATE NOT NULL, payment_status ENUM('Unpaid','Paid') NOT NULL DEFAULT 'Unpaid',
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_bill_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 CONSTRAINT chk_bill_amount CHECK(amount >= 0), INDEX idx_bill_user_due(user_id,due_date)
);
CREATE TABLE IF NOT EXISTS expenses (
 expense_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 category VARCHAR(80) NOT NULL, amount DECIMAL(12,2) NOT NULL,
 expense_date DATE NOT NULL, notes VARCHAR(255),
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_expense_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 CONSTRAINT chk_expense_amount CHECK(amount >= 0), INDEX idx_expense_user_date(user_id,expense_date)
);
CREATE TABLE IF NOT EXISTS documents (
 document_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 document_name VARCHAR(180) NOT NULL, category VARCHAR(80) NOT NULL DEFAULT 'Other',
 file_path VARCHAR(500), notes VARCHAR(255),
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_document_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 INDEX idx_document_user(user_id)
);
CREATE TABLE IF NOT EXISTS medicines (
 medicine_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 medicine_name VARCHAR(120) NOT NULL, dosage VARCHAR(80), reminder_time TIME NOT NULL,
 notes VARCHAR(255),
 CONSTRAINT fk_medicine_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 INDEX idx_medicine_user_time(user_id,reminder_time)
);
CREATE TABLE IF NOT EXISTS tasks (
 task_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 task_name VARCHAR(180) NOT NULL, deadline DATE NOT NULL,
 status ENUM('Pending','In Progress','Completed') NOT NULL DEFAULT 'Pending',
 priority TINYINT NOT NULL DEFAULT 3,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_task_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 CONSTRAINT chk_task_priority CHECK(priority BETWEEN 1 AND 5),
 INDEX idx_task_user_priority(user_id,priority), INDEX idx_task_deadline(user_id,deadline)
);
CREATE TABLE IF NOT EXISTS audit_logs (
 audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 entity_name VARCHAR(50) NOT NULL, record_id BIGINT NULL,
 action_name VARCHAR(30) NOT NULL, details VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_audit_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 INDEX idx_audit_user_time(user_id,created_at)
);

CREATE TABLE IF NOT EXISTS calendar_events (
 event_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 event_title VARCHAR(180) NOT NULL, event_date DATE NOT NULL,
 event_time TIME NULL, description VARCHAR(500), reminder_minutes INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_calendar_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 INDEX idx_calendar_user_date(user_id,event_date)
);
CREATE TABLE IF NOT EXISTS expense_budgets (
 budget_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 category VARCHAR(80) NOT NULL, month_start DATE NOT NULL,
 budget_amount DECIMAL(12,2) NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uq_budget_user_category_month(user_id,category,month_start),
 CONSTRAINT fk_budget_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 CONSTRAINT chk_budget_amount CHECK(budget_amount >= 0)
);
CREATE TABLE IF NOT EXISTS reminder_actions (
 action_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 reminder_key VARCHAR(190) NOT NULL, action_type ENUM('SNOOZE','COMPLETED') NOT NULL,
 snooze_until DATETIME NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_reminder_action(user_id,reminder_key),
 CONSTRAINT fk_reminder_action_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
