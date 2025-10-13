package com.arjunsudheer.backend;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;

public class ToDoPlugin {

    private Connection conn;

    public ToDoPlugin() {
        try {
            this.conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/motivaitor", "worker", "pass");
        } catch (SQLException e) {
            System.out.println("Unable to load the driver class.");
        }
    }

    // Use snake_case as suggested by Semantic Kernel documentation since most LLMs
    // are trained on Python function calling
    @DefineKernelFunction(name = "add_to_do_item", description = "Add a new to-do item.")
    public String add_to_do_item(
            @KernelFunctionParameter(name = "name", description = "Name of the to-do item", required = true) String name,
            @KernelFunctionParameter(name = "dueDate", description = "Due date of the to-do item", defaultValue = "") String dueDate,
            @KernelFunctionParameter(name = "belongsTo", description = "Name of the group the to-do item belongs to", defaultValue = "My Tasks") String belongsTo) {
        if (this.conn == null) {
            return "Database connection error.";
        }

        if (dueDate == null || dueDate.trim().isEmpty()) {
            dueDate = LocalDate.now().toString();
        }

        if (belongsTo == null || belongsTo.trim().isEmpty()) {
            belongsTo = "My Tasks";
        }

        // Find the group id for the given group name
        Integer belongsToID = 0;
        String groupIdQuery = "SELECT id FROM todogroups WHERE name = ?";
        try (PreparedStatement groupStmt = this.conn.prepareStatement(groupIdQuery)) {
            groupStmt.setString(1, belongsTo);
            var rs = groupStmt.executeQuery();
            if (rs.next()) {
                belongsToID = rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error executing query.";
        }

        String query = "INSERT INTO todolists (name, dueDate, status, belongsto) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setDate(2, Date.valueOf(dueDate));
            pstmt.setString(3, "Active");
            pstmt.setInt(4, belongsToID);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return "Creating to-do item failed.";
            }

            return "To-Do Item created";
        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
            return "Error executing query. Make sure the date is in the format YYYY-MM-DD.";
        }
    }

    // Use snake_case as suggested by Semantic Kernel documentation since most LLMs
    // are trained on Python function calling
    @DefineKernelFunction(name = "add_to_do_group", description = "Add a new to-do group.")
    public String add_to_do_group(
            @KernelFunctionParameter(name = "name", description = "Name of the to-do group", required = true) String name) {
        if (this.conn == null) {
            return "Database connection error.";
        }

        if (name == null || name.trim().isEmpty()) {
            name = "Untitled List";
        }

        String query = "INSERT INTO todogroups (name) VALUES (?)";

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setString(1, name);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return "Creating to-do list failed.";
            }

            return "To-Do Group created";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error executing query.";
        }
    }
}
