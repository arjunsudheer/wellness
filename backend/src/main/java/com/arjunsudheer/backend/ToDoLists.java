package com.arjunsudheer.backend;

public class ToDoLists {
    private int id;
    private String name;
    private String dueDate;
    private String status;
    private Integer belongsTo;

    public ToDoLists(int id, String name, String dueDate, String status, Integer belongsTo) {
        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.status = status;
        this.belongsTo = belongsTo;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(Integer belongsTo) {
        this.belongsTo = belongsTo;
    }
}