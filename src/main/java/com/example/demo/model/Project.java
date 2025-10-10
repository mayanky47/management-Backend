package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Lombok annotations for boilerplate code (getters, setters, constructors)
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
@Entity // Marks this class as a JPA entity, mapping to a database table
public class Project {
    @Id // Marks 'name' as the primary key
    private String name; // Project name is now the ID
    private String type; // e.g., "React", "Spring", "Python"
    private String path; // Actual path on the file system
    private String purpose;
    private String pastActivities;
    private String futurePlans;
    // No 'id' field anymore
}
