package com.example.demo.controller;

import com.example.demo.model.Project;
import com.example.demo.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController // Marks this class as a REST controller
@RequestMapping("/api") // Base path for all endpoints in this controller
@CrossOrigin(origins = "*") // Allow requests from your React frontend
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    // Define the fixed base directory for project discovery and creation
    private static final String DEFAULT_PROJECT_BASE_DIR = "D:\\project\\projects";

    // List of common build/dependency folders that should NEVER be treated as projects
    private static final List<String> EXCLUDED_BUILD_DEPENDENCY_FOLDERS = List.of(
            "node_modules", ".git", "target", "build", "dist", "out", "bin", ".idea", ".vscode",
            "src", "main", "test", "resources", "lib", "vendor", "docs", "temp", "tmp", "logs"
    );

    /**
     * Helper method to determine project type based on path/name heuristics.
     * This is primarily used for new project creation or if a type isn't explicitly set.
     * @param path The full path of the project folder.
     * @param name The name of the project folder.
     * @return A string representing the determined project type.
     */
    private String determineProjectType(String path, String name) {
        String lowerCasePath = path.toLowerCase();
        String lowerCaseName = name.toLowerCase();

        if (lowerCasePath.contains("frontend") || lowerCaseName.contains("react") || lowerCaseName.contains("angular") || lowerCaseName.contains("vue")) {
            return "React"; // Broaden to include other frontend types
        } else if (lowerCasePath.contains("backend") || lowerCaseName.contains("spring") || lowerCaseName.contains("java") || lowerCaseName.contains("kotlin")) {
            return "Spring"; // Broaden to include other backend types
        } else if (lowerCaseName.contains("python") || lowerCaseName.contains("django") || lowerCaseName.contains("flask")) {
            return "Python";
        } else if (lowerCaseName.contains("html") || lowerCaseName.contains("css") || lowerCaseName.contains("js") || lowerCaseName.contains("website") || lowerCaseName.contains("web")) {
            return "HTML/CSS/JS";
        }
        return "Other";
    }

    /**
     * Retrieves all projects by specifically scanning the 'frontend' and 'backend'
     * subdirectories of the base path. It merges discovered folders with information
     * from the database. Organizational folders and common build/dependency folders
     * are excluded.
     */
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects(@RequestParam(required = false) String directoryPath) {
        // Fetch all projects currently stored in the database
        List<Project> allProjectsFromDb = projectRepository.findAll();
        // Use a map to store final projects, using name as key to handle duplicates (DB data takes precedence)
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
            // Remove from the initial list so it's not re-added to finalProjectsMap
            allProjectsFromDb.remove(project);
        }
        // --- End Cleanup ---

        // Add remaining projects from DB to the map first. These are the authoritative versions.
        allProjectsFromDb.forEach(project -> finalProjectsMap.put(project.getName(), project));

        Path baseDirPath = Paths.get(DEFAULT_PROJECT_BASE_DIR);
        Path frontendDirPath = baseDirPath.resolve("frontend");
        Path backendDirPath = baseDirPath.resolve("backend");

        // Helper: Use BiConsumer for lambda that accepts two arguments
        BiConsumer<Path, String> scanDirectory = (dirPath, projectType) -> {
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (Stream<Path> paths = Files.walk(dirPath, 1)) { // Depth 1 to get direct children
                    paths.filter(Files::isDirectory) // Only consider directories
                            .filter(path -> !path.equals(dirPath)) // Exclude the directory itself
                            .filter(path -> {
                                String folderName = path.getFileName().toString().toLowerCase();
                                return !EXCLUDED_BUILD_DEPENDENCY_FOLDERS.contains(folderName);
                            })
                            .forEach(path -> {
                                String folderName = path.getFileName().toString();
                                String fullPath = path.toString();
                                // If the project is not already in the map (from DB or prior scan), add it
                                // DB entries would have been added already and take precedence
                                if (!finalProjectsMap.containsKey(folderName)) {
                                    Project discoveredProject = new Project(folderName, projectType, fullPath, "", "", "");
                                    finalProjectsMap.put(folderName, discoveredProject);
                                }
                            });
                } catch (IOException e) {
                    System.err.println("Error scanning " + projectType.toLowerCase() + " project directory: " + e.getMessage());
                }
            }
        };

        // 1. Scan frontend directory for React projects
        scanDirectory.accept(frontendDirPath, "React");

        // 2. Scan backend directory for Spring projects
        scanDirectory.accept(backendDirPath, "Spring");

        List<Project> resultProjects = new ArrayList<>(finalProjectsMap.values());

        // The directoryPath parameter from frontend is no longer used for filtering here,
        // as the scanning is now targeted.
        if (directoryPath != null && !directoryPath.isEmpty()) {
            resultProjects = resultProjects.stream()
                    .filter(project -> project.getPath() != null && project.getPath().startsWith(directoryPath))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(resultProjects);
    }
    /**
     * Retrieves a single project by its Name (which is now the ID).
     */
    @GetMapping("/projects/{name}")
    public ResponseEntity<Project> getProjectByName(@PathVariable String name) {
        Optional<Project> project = projectRepository.findById(name); // Use repository to find by Name
        return project.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Creates a new project.
     * The project's name is used as its ID. The path is constructed based on type and name
     * using the fixed default base directory. The project is then saved to the database.
     * Returns 409 Conflict if a project with the same name already exists.
     */
    @PostMapping("/projects")
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        if (project.getName() == null || project.getName().trim().isEmpty() ||
                project.getType() == null || project.getType().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Check if a project with this name already exists
        if (projectRepository.existsById(project.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        }

        String subfolder = "";
        if ("React".equals(project.getType())) {
            subfolder = "frontend\\"; // Use double backslash for Windows paths in Java strings
        } else if ("Spring".equals(project.getType())) {
            subfolder = "backend\\"; // Use double backslash for Windows paths in Java strings
        }
        // Construct path using the fixed DEFAULT_PROJECT_BASE_DIR
        project.setPath(DEFAULT_PROJECT_BASE_DIR + "\\" + subfolder + project.getName()); // Use double backslash

        Project savedProject = projectRepository.save(project); // Save the new project to the database
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProject);
    }

    /**
     * Updates an existing project.
     * The path is re-calculated based on the new type and name using a fixed default base directory.
     * The updated project is then saved to the database.
     */
    @PutMapping("/projects/{name}")
    public ResponseEntity<Project> updateProject(@PathVariable String name, @RequestBody Project project) {
        if (!projectRepository.existsById(name)) { // Check if project exists in DB by name
            return ResponseEntity.notFound().build();
        }
        if (project.getName() == null || project.getName().trim().isEmpty() ||
                project.getType() == null || project.getType().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Ensure the name in the request body matches the path variable
        project.setName(name);

        String subfolder = "";
        if ("React".equals(project.getType())) {
            subfolder = "frontend\\"; // Use double backslash for Windows paths in Java strings
        } else if ("Spring".equals(project.getType())) {
            subfolder = "backend\\"; // Use double backslash for Windows paths in Java strings
        }
        // Re-calculate path using the fixed DEFAULT_PROJECT_BASE_DIR
        project.setPath(DEFAULT_PROJECT_BASE_DIR + "\\" + subfolder + project.getName()); // Use double backslash

        Project updatedProject = projectRepository.save(project); // Save the updated project to the database
        return ResponseEntity.ok(updatedProject);
    }

    /**
     * Deletes a project by its Name (ID) from the database.
     */
    @DeleteMapping("/projects/{name}")
    public ResponseEntity<Void> deleteProject(@PathVariable String name) {
        if (projectRepository.existsById(name)) { // Check if project exists before deleting
            projectRepository.deleteById(name); // Delete from the database
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
