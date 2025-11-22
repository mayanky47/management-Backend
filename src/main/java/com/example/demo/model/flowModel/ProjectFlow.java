package com.example.demo.model.flowModel;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g. "User Registration Flow"
    
    @Column(length = 1000)
    private String description; // "Handles signup and email verification"

    private String projectName; // Link to the parent project

    @Lob // Large object for storing the big JSON graph data
    @Column(columnDefinition = "TEXT") 
    private String flowData; // JSON string of React Flow nodes/edges
}