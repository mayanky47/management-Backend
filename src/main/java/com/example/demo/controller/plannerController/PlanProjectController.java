package com.example.demo.controller.plannerController;


import com.example.demo.model.planner.ProjectPlan;
import com.example.demo.model.planner.ProjectStrategyVersion;
import com.example.demo.service.planService.ProjectPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * REST Controller for managing Projects (Idea Themes).
 */
@RestController
@RequestMapping("/api/plan-projects")
public class PlanProjectController {

    @Autowired
    private ProjectPlanService projectService;

    // GET /api/projects
    @GetMapping
    public List<ProjectPlan> getAllPlanProjects() {
        return projectService.findAllProjects();
    }

    // GET /api/projects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProjectPlan> getPlanProjectById(@PathVariable Long id) {
        ProjectPlan project = projectService.findProjectById(id)
                .orElseThrow(() -> new RuntimeException("Project not found for id: " + id));
        return ResponseEntity.ok(project);
    }

    // POST /api/projects
    @PostMapping
    public ProjectPlan createPlanProject(@RequestBody ProjectPlan project) {
        System.out.println("Creating project: " + project);
        return projectService.saveProject(project);
    }

    // PUT /api/projects/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ProjectPlan> updatePlanProject(@PathVariable Long id, @RequestBody ProjectPlan projectDetails) {
        final ProjectPlan updatedProject = projectService.updateProject(id, projectDetails);
        return ResponseEntity.ok(updatedProject);
    }

    // DELETE /api/projects/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlanProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------------------------------------
    // NEW HISTORY API ENDPOINT
    // --------------------------------------------------------------------------------

    /**
     * GET /api/projects/{id}/strategy-history
     * Retrieves the version history of the project's strategic plan.
     * The response should contain a List of ProjectStrategyVersion objects.
     */
    @GetMapping("/{id}/strategy-history")
    public ResponseEntity<List<ProjectStrategyVersion>> getProjectStrategyHistory(@PathVariable Long id) {
        // Call the service method to fetch the history for the given project ID.
        List<ProjectStrategyVersion> history = projectService.findProjectStrategyHistory(id);
        return ResponseEntity.ok(history);
    }
}
