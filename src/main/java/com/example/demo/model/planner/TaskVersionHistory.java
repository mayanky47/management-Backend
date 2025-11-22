package com.example.demo.model.planner;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Entity representing a snapshot of a Task's state at a specific point in time,
 * enabling history tracking and versioning of the idea/plan.
 */
@Entity
@Data
public class TaskVersionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We link back to the current Task's ID, not the whole Task object,
    // to simplify the data model and prevent unintended cascading/loading.
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    // When this historical snapshot was recorded.
    private Instant versionTimestamp = Instant.now();

    // The state of the Task before the change
    private String oldTitle;

    @Column(columnDefinition = "TEXT") // Use TEXT for potentially large description history
    private String oldDescription;

    private String oldStatus;

    // A summary of what changed (e.g., "Status moved to IN_PROGRESS", "Title corrected")
    private String changeSummary;
}
