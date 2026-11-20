-- Life Organizer database. Run this complete script in MySQL Workbench.
CREATE DATABASE IF NOT EXISTS life_organizer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE life_organizer;

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
 task_name VARCHAR(180) NOT NULL, deadline DATE NOT NULL, status ENUM('Pending','In Progress','Completed') NOT NULL DEFAULT 'Pending',
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

CREATE OR REPLACE VIEW v_user_task_summary AS
 SELECT user_id, COUNT(*) total_tasks,
 SUM(status='Completed') completed_tasks,
 SUM(status<>'Completed') active_tasks
 FROM tasks GROUP BY user_id;
CREATE OR REPLACE VIEW v_user_expense_summary AS
 SELECT user_id, COALESCE(SUM(amount),0) total_expenses
 FROM expenses GROUP BY user_id;
CREATE OR REPLACE VIEW v_user_bill_summary AS
 SELECT user_id, COUNT(*) total_bills,
 SUM(payment_status='Paid') paid_bills,
 SUM(payment_status='Unpaid') unpaid_bills,
 COALESCE(SUM(CASE WHEN payment_status='Unpaid' THEN amount ELSE 0 END),0) unpaid_amount
 FROM bills GROUP BY user_id;

DROP PROCEDURE IF EXISTS sp_user_dashboard;
DELIMITER //
CREATE PROCEDURE sp_user_dashboard(IN p_user_id INT)
BEGIN
 SELECT
  (SELECT COUNT(*) FROM groceries WHERE user_id=p_user_id) grocery_count,
  (SELECT COUNT(*) FROM groceries WHERE user_id=p_user_id AND purchased_status='Purchased') purchased_count,
  (SELECT COUNT(*) FROM bills WHERE user_id=p_user_id AND payment_status='Unpaid') unpaid_bill_count,
  (SELECT COALESCE(SUM(amount),0) FROM expenses WHERE user_id=p_user_id) expense_total,
  (SELECT COUNT(*) FROM tasks WHERE user_id=p_user_id AND status<>'Completed') active_tasks,
  (SELECT COUNT(*) FROM tasks WHERE user_id=p_user_id AND priority<=2 AND status<>'Completed') high_priority_tasks;
END//
DELIMITER ;

-- Triggers provide database-level audit events when the application session variable is set.
DROP TRIGGER IF EXISTS trg_task_update_audit;
DELIMITER //
CREATE TRIGGER trg_task_update_audit AFTER UPDATE ON tasks FOR EACH ROW
BEGIN
 IF @lifeorganizer_user_id IS NOT NULL THEN
  INSERT INTO audit_logs(user_id,entity_name,record_id,action_name,details)
  VALUES(@lifeorganizer_user_id,'tasks',NEW.task_id,'UPDATE',CONCAT('Task updated: ',NEW.task_name));
 END IF;
END//
DELIMITER ;
