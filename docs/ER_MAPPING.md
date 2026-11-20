# ER Diagram Mapping

The application preserves the seven entities and relationships from the supplied Life Organizer ER diagram.

- USER(user_id PK, name, email, password_hash)
- GROCERY(grocery_id PK, user_id FK, item_name, category, quantity, purchased_status)
- BILL(bill_id PK, user_id FK, bill_name, amount, due_date, payment_status)
- EXPENSE(expense_id PK, user_id FK, category, amount, expense_date)
- DOCUMENT(document_id PK, user_id FK, document_name, category)
- MEDICINE(medicine_id PK, user_id FK, medicine_name, dosage, reminder_time)
- TASK(task_id PK, user_id FK, task_name, deadline, status, priority)

Relationships: USER Manages GROCERY; USER Tracks BILL; USER Records EXPENSE; USER Stores DOCUMENT; USER Maintains MEDICINE; USER Creates TASK. Each is 1:N.

Implementation detail: `password` is stored as `password_hash` using PBKDF2 rather than storing plaintext credentials. Additional optional notes/file_path/created_at columns support application functionality without changing the seven main entities.
