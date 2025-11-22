package com.example.demo.model.planner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a high-level planning idea, study module, or project theme.
 * This is the parent entity for Tasks.
 */
@Entity
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProjectPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    // Status can be DRAFT, ACTIVE, COMPLETED, ABANDONED
    private String status;

    // This field now holds ONLY THE LATEST version of the strategy plan.
    @Column(columnDefinition = "TEXT")
    private String strategyPlan;

    @Column(columnDefinition = "TEXT")
    private String markdownPlan;

    private Instant createdAt = Instant.now();

    // Relationship: One Project can have many Tasks.
    @OneToMany(mappedBy = "project")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Task> tasks;

    // NEW: Relationship to track all historical versions of the strategy plan.
    // cascade = CascadeType.ALL: Operations (save, delete) on a Project cascade to its versions.
    // orphanRemoval = true: If a version is removed from this list, it's deleted from the DB.
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore // Exclude from API responses unless specifically requested
    private List<ProjectStrategyVersion> strategyVersions = new ArrayList<>();

    /**
     * The parent project of this project. It's lazy-loaded for performance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_project_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private ProjectPlan parentProject;

    /**
     * A list of child projects. Eagerly-loaded to be sent to the frontend.
     */
    @OneToMany(mappedBy = "parentProject", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProjectPlan> childProjects = new ArrayList<>();

    /**
     * Helper method to expose only the parent's ID in the JSON response.
     * This makes it easy for the frontend to know if a project has a parent.
     */
    @JsonProperty("parentProjectId")
    public Long getParentProjectId() {
        if (parentProject != null) {
            return parentProject.getId();
        }
        return null;
    }

}