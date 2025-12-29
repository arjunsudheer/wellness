package com.arjunsudheer.backend;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ToDoListController {
    @Value("${datasource.url}")
    private String dbUrl;

    @Value("${datasource.username}")
    private String dbUsername;

    @Value("${datasource.password}")
    private String dbPassword;
    
    private Connection conn;

    @Autowired
    public ToDoListController(
        @Value("${datasource.url}") String dbUrl,
        @Value("${datasource.username}") String dbUsername,
        @Value("${datasource.password}") String dbPassword
    ) {
        try {
            this.conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    @GetMapping("/")
    public String index() {
        return "Backend is running and ToDoListController is active!";
    }

    @GetMapping("/getToDoLists")
    public ResponseEntity<Map<Integer, String>> getToDoLists() {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        String query = "SELECT id, name FROM todogroups";
        Map<Integer, String> groups = new HashMap<>();

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                groups.put(rs.getInt("id"), rs.getString("name"));
            }
            return ResponseEntity.ok(groups);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/getToDoItems")
    public ResponseEntity<List<Map<String, Object>>> getToDoItemsByGroup(@RequestParam int id) {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        String query = "SELECT * FROM todolists WHERE belongsto = ? ORDER BY dueDate ASC";

        List<Map<String, Object>> items = new java.util.ArrayList<>();

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getInt("id"));
                item.put("name", rs.getString("name"));
                item.put("date", rs.getDate("dueDate") != null ? rs.getDate("dueDate").toString() : "");
                item.put("completed", "Completed".equals(rs.getString("status")));
                item.put("belongsTo", String.valueOf(rs.getInt("belongsTo")));
                items.add(item);
            }
            return ResponseEntity.ok(items);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/addToDoList")
    public ResponseEntity<String> addToDoList(@RequestBody Map<String, String> payload) {
        String idStr = payload.get("id");
        String name = payload.get("name");

        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String query = "INSERT INTO todogroups VALUES (?, ?)";

        // Set default values if not provided
        if (name == null || name.trim().isEmpty()) {
            name = "Untitled List";
        }

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setInt(1, Integer.parseInt(idStr));
            pstmt.setString(2, name);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Creating to-do list failed.");
            }

            return ResponseEntity.status(HttpStatus.CREATED).body("To-Do Group created");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing query.");
        }
    }

    @PostMapping("/addToDoItem")
    public ResponseEntity<String> addToDoItem(@RequestBody Map<String, String> payload) {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String name = payload.get("name");
        String dueDate = payload.get("dueDate");
        String status = payload.get("status");
        String belongsTo = payload.get("belongsTo");

        // Validate required fields
        if (name == null || name.trim().isEmpty() ||
                dueDate == null || dueDate.trim().isEmpty() ||
                status == null || status.trim().isEmpty() ||
                belongsTo == null || belongsTo.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing required fields.");
        }

        // Validate date format
        try {
            Date.valueOf(dueDate);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid date format. Use YYYY-MM-DD.");
        }

        if (!status.equals("Active") && !status.equals("Completed") && !status.equals("Overdue")) {
            status = "Active";
        }

        String query = "INSERT INTO todolists (name, dueDate, status, belongsto) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setDate(2, Date.valueOf(dueDate));
            pstmt.setString(3, status);
            pstmt.setInt(4, Integer.parseInt(belongsTo));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Creating to-do item failed.");
            }

            return ResponseEntity.status(HttpStatus.CREATED).body("To-Do Item created");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing query.");
        }
    }

    @PatchMapping("/updateToDoItem")
    public ResponseEntity<String> updateStatus(@RequestBody Map<String, String> payload) {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String idStr = payload.get("id");
        String name = payload.get("name");
        String dueDate = payload.get("dueDate");
        String status = payload.get("status");

        String query = "UPDATE todolists SET name = ?, dueDate = ?, status = ? WHERE id = ?";

        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Task name is required.");
        }
        if (dueDate == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Due date is required.");
        }
        // Validate date format
        try {
            Date.valueOf(dueDate);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid date format. Use YYYY-MM-DD.");
        }
        // Validate status
        if (status == null || (!status.equals("Active") && !status.equals("Completed") && !status.equals("Overdue"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status value.");
        }

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setDate(2, Date.valueOf(dueDate));
            pstmt.setString(3, status);
            pstmt.setInt(4, Integer.parseInt(idStr));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("To-Do List item not found.");
            }


            return ResponseEntity.ok("To-Do List item updated successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing query.");
        }
    }

    @DeleteMapping("/deleteCompletedToDoItems")
    public ResponseEntity<String> deleteToDoListItem() {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String query = "DELETE FROM ToDoLists WHERE status = 'Completed'";

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No completed To-Do List items were found.");
            }

            return ResponseEntity.ok("Completed To-Do List items deleted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing query.");
        }
    }

    @DeleteMapping("/deleteToDoList")
    public ResponseEntity<String> deleteToDoGroup(@RequestParam int id) {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        // Do not delete the default group "My Tasks" with id 0
        if (id == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot delete the default To-Do Group.");
        }

        // First delete all to-do items associated with this group
        String deleteItemsQuery = "DELETE FROM ToDoLists WHERE belongsto = ?";
        try (PreparedStatement pstmt = this.conn.prepareStatement(deleteItemsQuery)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting associated to-do items.");
        }

        // Then delete the to-do group itself
        String query = "DELETE FROM ToDoGroups WHERE id = ?";
        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setInt(1, id);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("To-Do Group not found.");
            }

            return ResponseEntity.ok("To-Do Group deleted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing query.");
        }
    }
}