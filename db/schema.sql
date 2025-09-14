-- schema.sql
CREATE TABLE IF NOT EXISTS books (
  isbn TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  author TEXT,
  total_copies INTEGER NOT NULL,
  available_copies INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS loans (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  isbn TEXT NOT NULL,
  user_id TEXT NOT NULL,
  loan_date TEXT NOT NULL,   -- ISO date
  due_date TEXT NOT NULL,    -- ISO date
  returned INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(isbn) REFERENCES books(isbn)
);
