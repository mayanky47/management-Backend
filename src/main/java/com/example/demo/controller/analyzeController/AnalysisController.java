package com.example.demo.controller.analyzeController;

import com.example.demo.model.projectModel.Project;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.service.analyzeService.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/analyze")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProjectRepository projectRepository;

    /**
     * Triggers an analysis of a specific project by its name.
     * @param name The name of the project (which is its ID).
     * @return The updated Project object with analysis metadata.
     */
    @PostMapping("/projects/{name}")
    public ResponseEntity<Project> analyzeProject(@PathVariable String name) {
        // 1. Find the project
        Optional<Project> projectOpt = projectRepository.findById(name);
        if (projectOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + name);
        }

        Project project = projectOpt.get();

        // 2. Run the analysis
        try {
            Project updatedProject = analysisService.analyzeProject(project);
            return ResponseEntity.ok(updatedProject);
        } catch (IOException e) {
            System.err.println("Analysis failed for project " + name + ": " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Analysis failed: " + e.getMessage());
        }
    }
}
