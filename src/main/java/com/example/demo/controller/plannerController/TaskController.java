package com.example.demo.controller.plannerController;




import com.example.demo.model.planner.Task;
import com.example.demo.model.planner.TaskVersionHistory;
import com.example.demo.service.planService.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing Tasks and accessing Task Version History.
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // GET /api/projects/{projectId}/tasks
    @GetMapping("/projects/{projectId}/tasks")
    public List<Task> getTasksByProject(@PathVariable Long projectId) {
        return taskService.findTasksByProjectId(projectId);
    }

    // GET /api/tasks/{id}
    @GetMapping("/tasks/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Task task = taskService.findTaskById(id)
                .orElseThrow(() -> new RuntimeException("Task not found for id: " + id));
        return ResponseEntity.ok(task);
    }

    // POST /api/tasks
    @PostMapping("/tasks")
    public Task createTask(@RequestBody Task task) {
        // Ensure the project link is valid before saving
        // (assuming the Project entity in the Task object is populated by the frontend/serializer)
        if (task.getProject() == null || task.getProject().getId() == null) {
            throw new IllegalArgumentException("Task must be linked to a valid Project ID.");
        }
        return taskService.saveTask(task);
    }

    // PUT /api/tasks/{id} - This endpoint triggers the versioning logic in the service layer
    @PutMapping("/tasks/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task taskDetails) {
        if (!id.equals(taskDetails.getId())) {
            throw new IllegalArgumentException("ID in path does not match ID in body.");
        }
        final Task updatedTask = taskService.saveTask(taskDetails);
        return ResponseEntity.ok(updatedTask);
    }

    // DELETE /api/tasks/{id}
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // HISTORY: GET /api/tasks/{id}/history
    @GetMapping("/tasks/{id}/history")
    public List<TaskVersionHistory> getTaskHistory(@PathVariable Long id) {
        return taskService.getTaskHistory(id);
    }
}
