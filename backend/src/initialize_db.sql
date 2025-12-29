CREATE TABLE ToDoGroups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO ToDoGroups VALUES (0, 'Default');

CREATE TABLE ToDoLists (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    dueDate DATE,
    status VARCHAR(9) CHECK (status IN ('Active', 'Completed', 'Overdue')),
    belongsTo INTEGER NOT NULL REFERENCES ToDoGroups(id) ON DELETE CASCADE
);