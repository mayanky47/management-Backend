package com.example.demo.controller.analyzeController;

import com.example.demo.service.analyzeService.SpringAnalyzerService;
import com.example.demo.model.analyzeModel.ProjectAnalysisResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/projects")
public class SpringAnalyzerController {

    @Autowired
    private SpringAnalyzerService analyzerService;

    // Run analysis
    @PostMapping("/{name}/analyze/spring")
    public ProjectAnalysisResult analyzeProject(@PathVariable String name) {
        return analyzerService.analyzeProject(name);
    }

    // Get last stored analysis result
    @GetMapping("/{name}/analysis")
    public ProjectAnalysisResult getAnalysis(@PathVariable String name) {
        return analyzerService.getAnalysis(name);
    }
}
