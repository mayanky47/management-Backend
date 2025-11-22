package com.example.demo.controller.projectController;

import com.example.demo.config.AppConfig;
import com.example.demo.model.projectModel.Project;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.service.analyzeService.AnalysisService;
import com.example.demo.service.DependencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private DependencyService dependencyService;

    // --- NEW: Inject Config ---
    @Autowired
    private AppConfig appConfig;

    // List of common build/dependency folders that should NEVER be treated as projects
    private static final List<String> EXCLUDED_BUILD_DEPENDENCY_FOLDERS = List.of(
            "node_modules", ".git", "target", "build", "dist", "out", "bin", ".idea", ".vscode",
            "src", "main", "test", "resources", "lib", "vendor", "docs", "temp", "tmp", "logs"
    );

    private String determineProjectType(String path, String name) {
        String lowerCasePath = path.toLowerCase();
        String lowerCaseName = name.toLowerCase();

        if (lowerCasePath.contains("frontend") || lowerCaseName.contains("react") || lowerCaseName.contains("angular") || lowerCaseName.contains("vue")) {
            return "React";
        } else if (lowerCasePath.contains("backend") || lowerCaseName.contains("spring") || lowerCaseName.contains("java") || lowerCaseName.contains("kotlin")) {
            return "Spring";
        } else if (lowerCaseName.contains("python") || lowerCaseName.contains("django") || lowerCaseName.contains("flask")) {
            return "Python";
        } else if (lowerCaseName.contains("html") || lowerCaseName.contains("css") || lowerCaseName.contains("js") || lowerCaseName.contains("website") || lowerCaseName.contains("web")) {
            return "HTML/CSS/JS";
        }
        return "Other";
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects(@RequestParam(required = false) String directoryPath) {
        List<Project> allProjectsFromDb = projectRepository.findAll();
        ConcurrentHashMap<String, Project> finalProjectsMap = new ConcurrentHashMap<>();

        // --- Cleanup: Remove DB records for projects not found on file system ---
        List<Project> projectsToDeleteFromDb = new ArrayList<>();
        for (Project dbProject : allProjectsFromDb) {
            Path projectDirectoryPath = Paths.get(dbProject.getPath());
            if (!Files.exists(projectDirectoryPath) || !Files.isDirectory(projectDirectoryPath)) {
                System.out.println("Cleaning up DB: Project '" + dbProject.getName() + "' found in DB but its directory '" + dbProject.getPath() + "' is missing. Marking for deletion.");
                projectsToDeleteFromDb.add(dbProject);
            }
        }

        for (Project project : projectsToDeleteFromDb) {
            projectRepository.deleteById(project.getName());
            allProjectsFromDb.remove(project);
        }

        allProjectsFromDb.forEach(project -> finalProjectsMap.put(project.getName(), project));

        // Use Config Paths
        Path frontendDirPath = appConfig.getFrontendPath();
        Path backendDirPath = appConfig.getBackendPath();

        BiConsumer<Path, String> scanDirectory = (dirPath, projectType) -> {
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (Stream<Path> paths = Files.walk(dirPath, 1)) {
                    paths.filter(Files::isDirectory)
                            .filter(path -> !path.equals(dirPath))
                            .filter(path -> {
                                String folderName = path.getFileName().toString().toLowerCase();
                                return !EXCLUDED_BUILD_DEPENDENCY_FOLDERS.contains(folderName);
                            })
                            .forEach(path -> {
                                String folderName = path.getFileName().toString();
                                String fullPath = path.toString();
                                if (!finalProjectsMap.containsKey(folderName)) {
                                    Project discoveredProject = new Project(folderName, projectType, fullPath, "", "", "", null, null);
                                    finalProjectsMap.put(folderName, discoveredProject);
                                }
                            });
                } catch (IOException e) {
                    System.err.println("Error scanning " + projectType.toLowerCase() + " project directory: " + e.getMessage());
                }
            }
        };

        scanDirectory.accept(frontendDirPath, "React");
        scanDirectory.accept(backendDirPath, "Spring");

        List<Project> resultProjects = new ArrayList<>(finalProjectsMap.values());

        if (directoryPath != null && !directoryPath.isEmpty()) {
            resultProjects = resultProjects.stream()
                    .filter(project -> project.getPath() != null && project.getPath().startsWith(directoryPath))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(resultProjects);
    }

    @GetMapping("/projects/{name}")
    public ResponseEntity<Project> getProjectByName(@PathVariable String name) {
        Optional<Project> project = projectRepository.findById(name);
        return project.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/projects")
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        if (project.getName() == null || project.getName().trim().isEmpty() ||
                project.getType() == null || project.getType().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (projectRepository.existsById(project.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Path baseDir;
        if ("React".equals(project.getType())) {
            baseDir = appConfig.getFrontendPath(); // Use Config
        } else if ("Spring".equals(project.getType())) {
            baseDir = appConfig.getBackendPath(); // Use Config
        } else {
            // Fallback or default
            baseDir = appConfig.getRootPath();
        }

        // Construct path
        project.setPath(baseDir.resolve(project.getName()).toString());

        Project savedProject = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProject);
    }

    @PutMapping("/projects/{name}")
    public ResponseEntity<Project> updateProject(@PathVariable String name, @RequestBody Project project) {
        if (!projectRepository.existsById(name)) {
            return ResponseEntity.notFound().build();
        }
        if (project.getName() == null || project.getName().trim().isEmpty() ||
                project.getType() == null || project.getType().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        project.setName(name);

        Path baseDir;
        if ("React".equals(project.getType())) {
            baseDir = appConfig.getFrontendPath();
        } else if ("Spring".equals(project.getType())) {
            baseDir = appConfig.getBackendPath();
        } else {
            baseDir = appConfig.getRootPath();
        }

        project.setPath(baseDir.resolve(project.getName()).toString());

        Project updatedProject = projectRepository.save(project);
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/projects/{name}")
    public ResponseEntity<Void> deleteProject(@PathVariable String name) {
        if (projectRepository.existsById(name)) {
            projectRepository.deleteById(name);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/projects/{name}/analyze")
    public ResponseEntity<Project> analyzeProject(@PathVariable String name) {
        Optional<Project> projectOpt = projectRepository.findById(name);
        if (projectOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + name);
        }

        Project project = projectOpt.get();

        try {
            Project updatedProject = analysisService.analyzeProject(project);
            return ResponseEntity.ok(updatedProject);
        } catch (IOException e) {
            System.err.println("Analysis failed for project " + name + ": " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Analysis failed: " + e.getMessage());
        }
    }

    @PostMapping("/projects/{name}/dependencies/add-sqlite")
    public ResponseEntity<String> addSqliteSupport(@PathVariable String name) {
        Optional<Project> projectOpt = projectRepository.findById(name);
        if (projectOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + name);
        }

        Project project = projectOpt.get();

        try {
            dependencyService.addSQLiteSupport(project);
            return ResponseEntity.ok("Successfully added SQLite support to " + name);
        } catch (IOException e) {
            System.err.println("Failed to modify files for " + name + ": " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to modify project files: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.err.println("Dependency injection failed for " + name + ": " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}