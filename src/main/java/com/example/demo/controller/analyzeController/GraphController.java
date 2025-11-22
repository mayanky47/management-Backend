package com.example.demo.controller.analyzeController;

import com.example.demo.model.graphModel.ArchitectureGraph;
import com.example.demo.service.analyzeService.ArchitectureGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyze")
public class GraphController {

    @Autowired
    private ArchitectureGraphService graphService;

    @GetMapping("/{projectName}/graph")
    public ResponseEntity<ArchitectureGraph> getProjectGraph(@PathVariable String projectName) {
        try {
            ArchitectureGraph graph = graphService.generateGraph(projectName);
            return ResponseEntity.ok(graph);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}