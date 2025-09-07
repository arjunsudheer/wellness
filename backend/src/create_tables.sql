CREATE TABLE ToDoGroups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE ToDoLists (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    dueDate DATE,
    status VARCHAR(9) CHECK (status IN ('Active', 'Completed', 'Overdue')),
    belongsTo INTEGER REFERENCES ToDoGroups(id) NOT NULL ON DELETE CASCADE
);