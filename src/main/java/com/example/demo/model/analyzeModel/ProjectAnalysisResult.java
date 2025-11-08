// src/main/java/com/example/demo/analyzer/model/ProjectAnalysisResult.java
package com.example.demo.model.analyzeModel;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class ProjectAnalysisResult {

    // --- Basic Info ---
    private String projectName;
    private String type;        // e.g. "spring", "react", "unsupported"
    private LocalDateTime analyzedAt;

    // --- ⭐️ ADD THIS FIELD ---
    private String projectUrl;  // URL of the running project (e.g. "http://localhost:8081")

    // --- Collected Data ---
    private List<ApiEndpoint> apiEndpoints = new ArrayList<>();
    private List<EntityInfo> entities = new ArrayList<>();
    private List<DependencyInfo> dependencies = new ArrayList<>();
    private Map<String, String> configuration = new LinkedHashMap<>();

    // --- Metadata / Status ---
    private String error;       // in case analysis fails
    private String summary;     // optional — e.g., “2 controllers, 4 entities detected”
}