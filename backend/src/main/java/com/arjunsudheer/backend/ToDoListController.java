package com.arjunsudheer.backend;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class ToDoListController {
    private Connection conn;
    private Statement st;

    public ToDoListController() {
        try {
            // Initialize PostgreSQL JDBC driver to connect to the courseplanner database
            this.conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/motivaitor", "worker",
                    "pass");

            this.st = this.conn.createStatement();
        } catch (SQLException e) {
            System.out.println("Unable to load the driver class.");
            return;
        }
    }

    @GetMapping("/getToDoLists")
    public ResponseEntity<Map<Integer, String>> getToDoLists() {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        String query = "SELECT id, name FROM ToDoGroups";
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
    public ResponseEntity<String> getToDoItemsByGroup(@RequestAttribute String group) {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String query;
        if (group == null || group.trim().isEmpty()) {
            query = "SELECT * FROM ToDoLists WHERE belongsTo IS NULL";
        } else {
            query = "SELECT * FROM ToDoLists WHERE belongsTo = (SELECT id FROM ToDoGroups WHERE name = ?)";
        }

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            // Set the group_id parameter in the query
            pstmt.setString(1, group);

            ResultSet rs = pstmt.executeQuery();

            Map<String, Object> response = new HashMap<>();
            while (rs.next()) {
                response.put("id", rs.getInt("id"));
                response.put("name", rs.getString("name"));
                response.put("dueDate", rs.getDate("dueDate"));
                response.put("status", rs.getString("status"));
                response.put("belongsTo", rs.getInt("belongsTo"));
            }
            return ResponseEntity.ok(response.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing query.");
        }
    }

    @PostMapping("/addToDoList")
    public ResponseEntity<String> addToDoList(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");

        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String query = "INSERT INTO ToDoGroups (name) VALUES (?)";

        // Set default values if not provided
        if (name == null || name.trim().isEmpty()) {
            name = "Untitled List";
        }

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setString(1, name);

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
    public ResponseEntity<String> addToDoItem(@RequestAttribute ToDoLists toDoList) {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String query = "INSERT INTO ToDoLists (name, dueDate, status, belongsTo) VALUES (?, ?, ?, ?)";

        // Set default values if not provided
        if (toDoList.getBelongsTo() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Task name is required.");
        }

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setString(0, toDoList.getName());
            pstmt.setDate(1, Date.valueOf(toDoList.getDueDate()));
            pstmt.setString(2, toDoList.getStatus());
            pstmt.setInt(3, toDoList.getBelongsTo());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Creating to-do list failed.");
            }

            return ResponseEntity.status(HttpStatus.CREATED).body("To-Do Item created");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing query.");
        }
    }

    @PatchMapping("/updateToDoItem")
    public ResponseEntity<String> updateStatus(@RequestAttribute int id, @RequestAttribute String name,
            @RequestAttribute String dueDate, @RequestAttribute String status) {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String query = "UPDATE ToDoLists SET name = ?, dueDate = ?, status = ? WHERE id = ?";

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
            pstmt.setInt(4, id);

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

    @DeleteMapping("/deleteToDoItem")
    public ResponseEntity<String> deleteToDoListItem(@RequestAttribute int id) {
        if (this.conn == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error.");
        }

        String query = "DELETE FROM ToDoLists WHERE id = ?";

        try (PreparedStatement pstmt = this.conn.prepareStatement(query)) {
            pstmt.setInt(1, id);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("To-Do List item not found.");
            }

            return ResponseEntity.ok("To-Do List item deleted successfully.");
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