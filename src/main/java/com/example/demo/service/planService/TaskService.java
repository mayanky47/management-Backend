package com.example.demo.service.planService;


import com.example.demo.model.planner.Task;
import com.example.demo.model.planner.TaskVersionHistory;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.TaskVersionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for handling Task business logic, including automatic versioning.
 */
@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskVersionHistoryRepository historyRepository;

    public List<Task> findTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectIdOrderByDueDateAsc(projectId);
    }

    public Optional<Task> findTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public Task saveTask(Task newTask) {
        if (newTask.getId() != null) {
            // This is an update operation, check for versioning
            return updateAndVersionTask(newTask);
        } else {
            // This is a new task creation
            return taskRepository.save(newTask);
        }
    }

    private Task updateAndVersionTask(Task newTask) {
        Task oldTask = taskRepository.findById(newTask.getId())
                .orElseThrow(() -> new RuntimeException("Task not found for ID: " + newTask.getId()));

        boolean hasChanges = false;
        StringBuilder summary = new StringBuilder();

        // 1. Check Title Change
        if (!oldTask.getTitle().equals(newTask.getTitle())) {
            summary.append("Title changed from '").append(oldTask.getTitle()).append("' to '").append(newTask.getTitle()).append("'. ");
            hasChanges = true;
        }

        // 2. Check Description Change
        if (!oldTask.getDescription().equals(newTask.getDescription())) {
            summary.append("Description content modified. ");
            hasChanges = true;
        }

        // 3. Check Status Change (Crucial for Kanban)
        if (!oldTask.getStatus().equals(newTask.getStatus())) {
            summary.append("Status moved from '").append(oldTask.getStatus()).append("' to '").append(newTask.getStatus()).append("'. ");
            hasChanges = true;
        }

        if (hasChanges) {
            // Create a historical record before saving the updated task
            TaskVersionHistory history = new TaskVersionHistory();
            history.setTaskId(oldTask.getId());
            history.setOldTitle(oldTask.getTitle());
            history.setOldDescription(oldTask.getDescription());
            history.setOldStatus(oldTask.getStatus());
            history.setChangeSummary(summary.toString().trim());

            // Save the history record
            historyRepository.save(history);

            // Update the main task's modification timestamp
            newTask.setUpdatedAt(Instant.now());
        }

        // Copy over unchangeable fields (like Project link) and save the new task state
        newTask.setProject(oldTask.getProject());
        return taskRepository.save(newTask);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
        // Note: History records are typically left behind for auditing purposes,
        // even if the main task is deleted.
    }

    public List<TaskVersionHistory> getTaskHistory(Long taskId) {
        return historyRepository.findByTaskIdOrderByVersionTimestampAsc(taskId);
    }
}
