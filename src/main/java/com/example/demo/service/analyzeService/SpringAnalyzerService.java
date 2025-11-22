package com.example.demo.service.analyzeService;

import com.example.demo.config.AppConfig;
import com.example.demo.model.analyzeModel.ApiEndpoint;
import com.example.demo.model.analyzeModel.DependencyInfo;
import com.example.demo.model.analyzeModel.EntityInfo;
import com.example.demo.model.analyzeModel.ProjectAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

@Service
public class SpringAnalyzerService {

    @Autowired
    private AppConfig appConfig;

    private static final Path ANALYSIS_DIR = Paths.get("analyzed");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // -----------------------------------------------------------------
    // AUTO-DETECTION + ANALYSIS ENTRY
    // -----------------------------------------------------------------
    public ProjectAnalysisResult analyzeProject(String projectName) {
        ProjectAnalysisResult result = new ProjectAnalysisResult();
        result.setProjectName(projectName);
        result.setAnalyzedAt(LocalDateTime.now());

        // Use Config Path
        Path projectPath = appConfig.getBackendPath().resolve(projectName);

        if (!Files.exists(projectPath)) {
            result.setError("❌ Project directory not found: " + projectPath);
            return result;
        }

        try {
            // --- Step 1: Auto-detect project type ---
            boolean isSpring = detectSpringProject(projectPath);

            if (!isSpring) {
                result.setType("unsupported");
                result.setError("⚠️ Not a Spring Boot project (skipped).");
                saveResult(projectName, result);
                return result;
            }

            // --- Step 2: Run full Spring analysis ---
            result.setType("spring");
            result.setApiEndpoints(scanControllers(projectPath));
            result.setEntities(scanEntities(projectPath));
            result.setDependencies(readPomDependencies(projectPath));

            // 1. Get the configuration map
            Map<String, String> config = readApplicationProperties(projectPath);
            result.setConfiguration(config);

            // 2. Build and set the projectUrl from the config
            String port = config.getOrDefault("server.port", "8080");
            String contextPath = config.getOrDefault("server.servlet.context-path", "");

            // 3. Set the final URL (with path normalization)
            if (!contextPath.isBlank() && !contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
            result.setProjectUrl("http://localhost:" + port + contextPath);

            saveResult(projectName, result);

        } catch (Exception e) {
            result.setError("Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // -----------------------------------------------------------------
    // GET EXISTING RESULT
    // -----------------------------------------------------------------
    public ProjectAnalysisResult getAnalysis(String projectName) {
        Path resultPath = ANALYSIS_DIR.resolve(projectName).resolve("analysis.json");
        if (!Files.exists(resultPath)) return null;

        try {
            return objectMapper.readValue(resultPath.toFile(), ProjectAnalysisResult.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read analysis result", e);
        }
    }

    // -----------------------------------------------------------------
    // AUTO-DETECT PROJECT TYPE
    // -----------------------------------------------------------------
    private boolean detectSpringProject(Path projectPath) {
        Path pom = projectPath.resolve("pom.xml");
        if (!Files.exists(pom)) return false;

        try {
            String pomContent = Files.readString(pom);
            if (pomContent.contains("spring-boot-starter")) return true;

            try (Stream<Path> stream = Files.walk(projectPath)) {
                return stream
                        .filter(p -> p.toString().endsWith(".java"))
                        .anyMatch(file -> {
                            try {
                                String content = Files.readString(file);
                                return content.contains("@SpringBootApplication")
                                        || content.contains("@RestController")
                                        || content.contains("SpringApplication.run");
                            } catch (IOException ignored) {
                                return false;
                            }
                        });
            }

        } catch (IOException e) {
            System.err.println("Spring detection failed: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------
    // CONTROLLER SCAN
    // -----------------------------------------------------------------
    private List<ApiEndpoint> scanControllers(Path projectPath) throws IOException {
        List<ApiEndpoint> endpoints = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(projectPath)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                try {
                    List<String> lines = Files.readAllLines(file);
                    String controllerClass = null;
                    boolean isRestController = false;
                    String basePath = "";

                    for (String line : lines) {
                        line = line.trim();

                        if (line.contains("@RestController")) isRestController = true;
                        if (line.startsWith("@RequestMapping(")) basePath = extractMappingValue(line);
                        if (line.startsWith("public class ")) {
                            String[] parts = line.split("\\s+");
                            controllerClass = parts.length >= 3 ? parts[2] : "UnknownController";
                        }

                        if (isRestController && line.matches("@(Get|Post|Put|Delete)Mapping\\(.*\\)")) {
                            String method = line.substring(1, line.indexOf("Mapping")).toUpperCase();
                            String subPath = extractMappingValue(line);

                            ApiEndpoint ep = new ApiEndpoint();
                            ep.setHttpMethod(method);
                            ep.setPath((basePath + "/" + subPath).replaceAll("//+", "/"));
                            ep.setController(controllerClass);
                            endpoints.add(ep);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Failed to parse controller: " + file);
                }
            });
        }

        return endpoints;
    }

    // -----------------------------------------------------------------
    // ENTITY SCAN
    // -----------------------------------------------------------------
    private List<EntityInfo> scanEntities(Path projectPath) throws IOException {
        List<EntityInfo> entities = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(projectPath)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                try {
                    List<String> lines = Files.readAllLines(file);
                    boolean isEntity = lines.stream().anyMatch(l -> l.contains("@Entity"));
                    if (!isEntity) return;

                    EntityInfo entity = new EntityInfo();
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("public class ")) {
                            String[] parts = line.split("\\s+");
                            entity.setName(parts.length >= 3 ? parts[2] : "UnknownEntity");
                        }
                        if (line.contains("@Table(")) {
                            entity.setTableName(extractMappingValue(line));
                        }
                    }
                    entities.add(entity);
                } catch (IOException e) {
                    System.err.println("Failed to parse entity: " + file);
                }
            });
        }

        return entities;
    }

    // -----------------------------------------------------------------
    // DEPENDENCY SCAN
    // -----------------------------------------------------------------
    private List<DependencyInfo> readPomDependencies(Path projectPath) throws IOException {
        Path pom = projectPath.resolve("pom.xml");
        List<DependencyInfo> deps = new ArrayList<>();
        if (!Files.exists(pom)) return deps;

        String xml = Files.readString(pom);
        Matcher matcher = Pattern.compile(
                "<dependency>\\s*<groupId>(.*?)</groupId>\\s*<artifactId>(.*?)</artifactId>\\s*(<version>(.*?)</version>)?\\s*</dependency>",
                Pattern.DOTALL
        ).matcher(xml);

        while (matcher.find()) {
            DependencyInfo info = new DependencyInfo();
            info.setGroupId(matcher.group(1));
            info.setArtifactId(matcher.group(2));
            info.setVersion(matcher.group(4));
            deps.add(info);
        }

        return deps;
    }

    // -----------------------------------------------------------------
    // CONFIG SCAN (.properties / .yml)
    // -----------------------------------------------------------------
    private Map<String, String> readApplicationProperties(Path projectPath) throws IOException {
        Map<String, String> props = new LinkedHashMap<>();

        Path propFile = projectPath.resolve("src/main/resources/application.properties");
        Path ymlFile = projectPath.resolve("src/main/resources/application.yml");

        Path fileToRead = Files.exists(propFile) ? propFile : (Files.exists(ymlFile) ? ymlFile : null);
        if (fileToRead == null) return props;

        Files.readAllLines(fileToRead).forEach(line -> {
            line = line.trim();
            if (line.startsWith("#") || line.isBlank()) return;
            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                props.put(parts[0].trim(), parts[1].trim());
            } else if (line.contains(": ")) {
                String[] parts = line.split(": ", 2);
                props.put(parts[0].trim(), parts[1].trim());
            }
        });

        return props;
    }

    // -----------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------
    private String extractMappingValue(String line) {
        Matcher matcher = Pattern.compile("\\(\"(.*?)\"\\)").matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }

    private void saveResult(String projectName, ProjectAnalysisResult result) throws IOException {
        Path outDir = ANALYSIS_DIR.resolve(projectName);
        Files.createDirectories(outDir);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(outDir.resolve("analysis.json").toFile(), result);
    }
}