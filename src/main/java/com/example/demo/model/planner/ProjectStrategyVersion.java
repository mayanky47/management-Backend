package com.example.demo.model.planner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor // Lombok constructor for JPA
public class ProjectStrategyVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long versionId;

    // Many-to-One relationship: Many versions belong to one project.
    // This side owns the foreign key (`project_id`).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore // Important to prevent circular serialization in APIs
    private ProjectPlan project;

    // The actual content of the plan at this point in history.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String oldStrategyPlan;


    private Instant versionTimestamp = Instant.now();

    // Constructor to easily create a new version
    public ProjectStrategyVersion(ProjectPlan project, String oldStrategyPlan) {
        this.project = project;
        this.oldStrategyPlan = oldStrategyPlan;
    }
}