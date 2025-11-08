package com.example.demo.controller.projectController;

import com.example.demo.model.projectModel.Project;
import com.example.demo.service.ProjectCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/create")
public class CreateProjectController {

    @Autowired
    private ProjectCreationService projectCreationService;

    @PostMapping("/projects")
    public ResponseEntity<Object> createProject(@RequestBody Project project) {
        try {
            Project createdProject = projectCreationService.createProject(project);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
