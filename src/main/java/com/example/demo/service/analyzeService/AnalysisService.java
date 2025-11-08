package com.example.demo.service.analyzeService;

import com.example.demo.model.projectModel.Project;
import com.example.demo.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class AnalysisService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ObjectMapper objectMapper; // Spring Boot provides this

    // --- DTO for API Endpoints ---
    @Data
    private static class ApiEndpoint {
        private String method;
        private String path;
        private String javaMethodName;

        public ApiEndpoint(String method, String path, String javaMethodName) {
            this.method = method;
            this.path = path;
            this.javaMethodName = javaMethodName;
        }
    }

    // --- DTO for React Components ---
    @Data
    private static class ReactComponent {
        private String componentName;
        private String fileName;

        public ReactComponent(String componentName, String fileName) {
            this.componentName = componentName;
            this.fileName = fileName;
        }
    }

    // --- Regex Patterns ---
    // Simple regex for Spring request mappings (e.g., @GetMapping("/path"))
    private static final Pattern MAPPING_PATTERN = Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping\\(\"?(.*?)\"?\\)");
    // Simple regex for Java method names (e.g., public String getThing(...))
    private static final Pattern METHOD_PATTERN = Pattern.compile("public\\s+[\\w<>]+\\s+([a-zA-Z0-9_]+)\\s*\\(");
    // Simple regex for React components (e.g., export default function MyComponent)
    private static final Pattern REACT_FUNC_PATTERN = Pattern.compile("export\\s+default\\s+function\\s+([A-Za-z0-9_]+)");
    // Simple regex for React arrow func components (e.g., const MyComponent = () =>)
    private static final Pattern REACT_CONST_PATTERN = Pattern.compile("const\\s+([A-Za-z0-9_]+)\\s*=\\s*\\(");


    /**
     * Analyzes a project based on its type and saves metadata.
     */
    public Project analyzeProject(Project project) throws IOException {
        String type = project.getType();
        Path path = Paths.get(project.getPath());

        if (!Files.isDirectory(path)) {
            throw new IOException("Project path not found: " + path);
        }

        switch (type) {
            case "Spring":
                List<ApiEndpoint> endpoints = analyzeSpringProject(path);
                project.setApiMetadata(objectMapper.writeValueAsString(endpoints));
                project.setComponentMetadata(null); // Clear other metadata
                break;
            case "React":
                List<ReactComponent> components = analyzeReactProject(path);
                project.setComponentMetadata(objectMapper.writeValueAsString(components));
                project.setApiMetadata(null); // Clear other metadata
                break;
            default:
                // No analysis for this type
                break;
        }
        return projectRepository.save(project);
    }

    /**
     * Scans a Spring project directory for API endpoints.
     * This is a simple regex-based scan and not a full AST parse.
     */
    private List<ApiEndpoint> analyzeSpringProject(Path projectPath) throws IOException {
        List<ApiEndpoint> endpoints = new ArrayList<>();
        // Only scan within src/main/java
        Path srcPath = projectPath.resolve("src").resolve("main").resolve("java");

        if (!Files.isDirectory(srcPath)) {
            return endpoints; // No src path found
        }

        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .filter(p -> {
                     try {
                         // Only process files that contain @RestController
                         return Files.readString(p).contains("@RestController");
                     } catch (IOException e) {
                         return false;
                     }
                 })
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         String[] lines = content.split("\\R");
                         String lastMethodName = "unknownMethod";

                         for (String line : lines) {
                             Matcher methodMatcher = METHOD_PATTERN.matcher(line);
                             if (methodMatcher.find()) {
                                 lastMethodName = methodMatcher.group(1);
                             }

                             Matcher mappingMatcher = MAPPING_PATTERN.matcher(line);
                             if (mappingMatcher.find()) {
                                 String method = mappingMatcher.group(1).toUpperCase();
                                 String path = mappingMatcher.group(2);
                                 endpoints.add(new ApiEndpoint(method, path, lastMethodName));
                             }
                         }
                     } catch (IOException e) {
                         System.err.println("Error reading file: " + file);
                     }
                 });
        }
        return endpoints;
    }

    /**
     * Scans a React project directory for components.
     * This is a simple regex-based scan.
     */
    private List<ReactComponent> analyzeReactProject(Path projectPath) throws IOException {
        List<ReactComponent> components = new ArrayList<>();
        // Only scan within src
        Path srcPath = projectPath.resolve("src");

        if (!Files.isDirectory(srcPath)) {
            return components; // No src path found
        }

        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String fileName = p.toString();
                     return (fileName.endsWith(".tsx") || fileName.endsWith(".jsx")) &&
                            !fileName.contains(".test.") && !fileName.contains(".spec.");
                 })
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         String fileName = file.getFileName().toString();

                         Matcher funcMatcher = REACT_FUNC_PATTERN.matcher(content);
                         if (funcMatcher.find()) {
                             components.add(new ReactComponent(funcMatcher.group(1), fileName));
                         } else {
                             Matcher constMatcher = REACT_CONST_PATTERN.matcher(content);
                             if (constMatcher.find()) {
                                 // Check if it's likely a component (starts with uppercase)
                                 String name = constMatcher.group(1);
                                 if (Character.isUpperCase(name.charAt(0))) {
                                     components.add(new ReactComponent(name, fileName));
                                 }
                             }
                         }
                     } catch (IOException e) {
                         System.err.println("Error reading file: " + file);
                     }
                 });
        }
        return components;
    }
}
