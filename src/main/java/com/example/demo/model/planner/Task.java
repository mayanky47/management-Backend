package com.example.demo.model.planner;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Entity representing a specific planning task or item within a Project.
 * This entity has relationships with Project (parent) and TaskVersionHistory (versions).
 */
@Entity
@Data
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // Rich text/Markdown content for detailed planning notes
    private String description;

    private LocalDate dueDate;

    // Priority can be LOW, MEDIUM, HIGH
    private String priority;

    // Status for Kanban: TO_DO, IN_PROGRESS, REVIEW, COMPLETED
    private String status;

    private Instant createdAt = Instant.now();

    // This timestamp is key to triggering the history logging mechanism in the service layer
    private Instant updatedAt = Instant.now();

    // Relationship: Many Tasks belong to one Project.
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false) // Defines the foreign key column
    private ProjectPlan project;

    // Relationship: One Task can have many historical versions.
    @OneToMany(mappedBy = "taskId")
    @ToString.Exclude // Exclude from toString to prevent infinite recursion
    @EqualsAndHashCode.Exclude // Exclude from equals/hashCode
    private List<TaskVersionHistory> versions;
}
