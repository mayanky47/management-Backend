package com.example.demo.controller.autoSaveController;

import com.example.demo.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class AutoSaveController {

    @Autowired
    private AppConfig appConfig;

    /**
     * Endpoint to get the list of Spring projects by scanning the backend directory.
     */
    @GetMapping("/projects/spring")
    public ResponseEntity<?> getSpringProjects() {
        return listSubdirectories(appConfig.getBackendPath());
    }

    /**
     * Endpoint to get the list of React projects by scanning the frontend directory.
     */
    @GetMapping("/projects/react")
    public ResponseEntity<?> getReactProjects() {
        return listSubdirectories(appConfig.getFrontendPath());
    }

    /**
     * Endpoint to fetch the main class content for the AI prompt.
     */
    @GetMapping("/main-class")
    public ResponseEntity<String> getMainClass(@RequestParam String project) {
        if (project == null || project.isBlank()) {
            return ResponseEntity.badRequest().body("// Project name is required.");
        }

        // Use Config Path
        Path projectDir = appConfig.getBackendPath().resolve(project);

        if (!Files.isDirectory(projectDir)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("// Error: Project directory not found: " + projectDir);
        }

        Path srcMainJava = projectDir.resolve(Paths.get("src", "main", "java"));
        if (!Files.isDirectory(srcMainJava)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("// Error: 'src/main/java' not found in project: " + project);
        }

        try (Stream<Path> javaFiles = Files.walk(srcMainJava)
                .filter(path -> path.toString().endsWith(".java"))) {

            Optional<String> mainClassContent = javaFiles
                    .map(path -> {
                        try {
                            String content = Files.readString(path);
                            if (content.contains("@SpringBootApplication")) {
                                return content;
                            }
                        } catch (IOException e) {
                            // Ignore files that can't be read
                        }
                        return null;
                    })
                    .filter(content -> content != null)
                    .findFirst();

            return mainClassContent
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("// Error: No @SpringBootApplication class found in: " + project));

        } catch (IOException e) {
            System.err.println("Error scanning for main class: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("// Error: Failed to scan project files: " + e.getMessage());
        }
    }

    /**
     * DTO for the save request.
     */
    public static class FileSaveRequest {
        private String content;
        private String springProject;
        private String reactProject;

        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSpringProject() { return springProject; }
        public void setSpringProject(String springProject) { this.springProject = springProject; }
        public String getReactProject() { return reactProject; }
        public void setReactProject(String reactProject) { this.reactProject = reactProject; }
    }

    /**
     * Endpoint to analyze and save the file.
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveFile(@RequestBody FileSaveRequest request) {
        String content = request.getContent();
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Content cannot be empty."));
        }

        String[] lines = content.split("\n", 2);
        String firstLine = lines[0].trim();
        String contentToSave = (lines.length > 1) ? lines[1] : "";

        // --- Step 1: Find relative path in the first line comment ---
        String relativePathStr = null;
        if (firstLine.startsWith("//")) {
            relativePathStr = firstLine.substring(2).trim();
        } else if (firstLine.startsWith("/*") && firstLine.endsWith("*/")) {
            relativePathStr = firstLine.substring(2, firstLine.length() - 2).trim();
        }

        if (relativePathStr == null || !isValidPath(relativePathStr)) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "No valid file path comment found on the first line. " +
                            "Path must be relative (e.g., 'src/main/java/...') and must not contain '..' or drive letters."
            ));
        }

        Path relativePath = Paths.get(relativePathStr);

        // --- Step 2: Decide which project this file belongs to ---
        Path targetBaseDir;
        String targetProjectName;

        if (relativePathStr.contains("src/main/java") || relativePathStr.contains("src/main/resources")) {
            targetBaseDir = appConfig.getBackendPath(); // Use Config
            targetProjectName = request.getSpringProject();
        } else if (relativePathStr.contains("src/") && (relativePathStr.endsWith(".tsx") || relativePathStr.endsWith(".jsx") || relativePathStr.endsWith(".ts") || relativePathStr.endsWith(".css"))) {
            targetBaseDir = appConfig.getFrontendPath(); // Use Config
            targetProjectName = request.getReactProject();
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Could not determine project type from path: " + relativePathStr));
        }

        if (targetProjectName == null || targetProjectName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No target project was selected for this file type."));
        }

        // --- Step 3: Save the file ---
        try {
            Path finalPath = targetBaseDir.resolve(targetProjectName).resolve(relativePath);

            // Ensure parent directories exist
            Path parentDir = finalPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            // Write the file (the content *without* the first line)
            Files.writeString(finalPath, contentToSave);

            System.out.println("File saved: " + finalPath.toAbsolutePath());

            return ResponseEntity.ok(Map.of(
                    "message", "File saved successfully.",
                    "path", finalPath.toString()
            ));

        } catch (IOException e) {
            System.err.println("Failed to save file: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Failed to write file: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * NEW Endpoint: Generates a full context prompt by reading all files
     */
    @GetMapping("/full-context")
    public ResponseEntity<String> getFullContext(
            @RequestParam String springProject,
            @RequestParam String reactProject) {

        if (springProject == null || springProject.isBlank() || reactProject == null || reactProject.isBlank()) {
            return ResponseEntity.badRequest().body("// Error: Both Spring and React projects must be selected.");
        }

        StringBuilder fullContext = new StringBuilder();
        fullContext.append("Here is the full project context for BOTH projects.\n");
        fullContext.append("You will decide which project any new files belong to.\n\n");

        // --- 1. Get Spring Project Context ---
        Path springProjectDir = appConfig.getBackendPath().resolve(springProject); // Use Config
        if (!Files.isDirectory(springProjectDir)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("// Error: Spring project not found: " + springProject);
        }
        fullContext.append("// --- Start of Spring Project: ").append(springProject).append(" ---\n\n");
        try {
            appendProjectFiles(springProjectDir, springProjectDir, fullContext, ".java", ".xml", ".properties");
        } catch (IOException e) {
            System.err.println("Error scanning Spring project: " + e.getMessage());
            return ResponseEntity.status(500).body("// Error scanning Spring project: " + e.getMessage());
        }
        fullContext.append("\n// --- End of Spring Project: ").append(springProject).append(" ---\n\n");


        // --- 2. Get React Project Context ---
        Path reactProjectDir = appConfig.getFrontendPath().resolve(reactProject); // Use Config
        if (!Files.isDirectory(reactProjectDir)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("// Error: React project not found: " + reactProject);
        }
        fullContext.append("// --- Start of React Project: ").append(reactProject).append(" ---\n\n");
        try {
            appendProjectFiles(reactProjectDir, reactProjectDir, fullContext, ".tsx", ".ts", ".jsx", ".js", ".css");
        } catch (IOException e) {
            System.err.println("Error scanning React project: " + e.getMessage());
            return ResponseEntity.status(500).body("// Error scanning React project: " + e.getMessage());
        }
        fullContext.append("\n// --- End of React Project: ").append(reactProject).append(" ---\n\n");


        // --- 3. Add Final Instructions ---
        fullContext.append("When generating code, YOU MUST include the full intended file path as a comment on the very first line, using the correct project's structure as a reference.\n\n");
        fullContext.append("Example (Spring): // src/main/java/com/example/demo/service/MyService.java\n");
        fullContext.append("Example (React): // src/components/ui/Button.tsx\n\n");
        fullContext.append("After this first line, provide only the raw code.\n");

        return ResponseEntity.ok(fullContext.toString());
    }

    /**
     * Helper to recursively scan a directory and append file content to the context.
     */
    private void appendProjectFiles(Path projectRoot, Path currentDir, StringBuilder context, String... fileExtensions) throws IOException {
        try (Stream<Path> stream = Files.list(currentDir)) {
            stream.forEach(path -> {
                String fileName = path.getFileName().toString();
                // Skip common directories
                if (Files.isDirectory(path)) {
                    if (fileName.equals("node_modules") || fileName.equals(".git") || fileName.equals("build") || fileName.equals("target") || fileName.equals(".idea") || fileName.equals(".vscode")) {
                        return; // Skip this directory
                    }
                    try {
                        // Recurse into subdirectory
                        appendProjectFiles(projectRoot, path, context, fileExtensions);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to scan subdirectory: " + path, e); // Wrap in RuntimeException to use in lambda
                    }
                } else {
                    // Check if it's one of the file types we want
                    boolean matchesExtension = false;
                    for (String ext : fileExtensions) {
                        if (fileName.endsWith(ext)) {
                            matchesExtension = true;
                            break;
                        }
                    }

                    if (matchesExtension) {
                        try {
                            // Use relative path for the file comment
                            Path relativePath = projectRoot.relativize(path);
                            String content = Files.readString(path);

                            context.append("// ").append(relativePath.toString().replace("\\", "/")).append("\n");
                            context.append(content).append("\n\n");

                        } catch (IOException e) {
                            // Squelch read errors for individual files (e.g., locked files)
                            System.err.println("Could not read file, skipping: " + path + " - " + e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) { // Catch wrapper RuntimeException
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause(); // Re-throw original IOException
            } else {
                throw new IOException("Failed to list files in directory:  currentDir,"+ e);
            }
        }
    }

    private ResponseEntity<?> listSubdirectories(Path basePath) {
        if (!Files.isDirectory(basePath)) {
            String errorMsg = "Configuration error: Base path not found: " + basePath;
            System.err.println(errorMsg);
            return ResponseEntity.status(500).body(List.of(errorMsg));
        }

        try (Stream<Path> stream = Files.list(basePath)) {
            List<String> directories = stream
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted()
                    .toList();
            return ResponseEntity.ok(directories);
        } catch (IOException e) {
            String errorMsg = "Failed to read project directory: " + basePath;
            System.err.println(errorMsg + "\n" + e.getMessage());
            return ResponseEntity.status(500).body(List.of(errorMsg));
        }
    }

    private boolean isValidPath(String path) {
        if (path.contains("..")) {
            return false;
        }
        if (Paths.get(path).isAbsolute()) {
            return false;
        }
        return (path.contains("/") || path.contains("\\")) && path.contains(".");
    }
}