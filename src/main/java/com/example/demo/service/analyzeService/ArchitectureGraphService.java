package com.example.demo.service.analyzeService;

import com.example.demo.config.AppConfig;
import com.example.demo.model.graphModel.ArchitectureGraph;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class ArchitectureGraphService {

    @Autowired
    private AppConfig appConfig;

    public ArchitectureGraph generateGraph(String projectName) throws IOException {
        ArchitectureGraph graph = new ArchitectureGraph();
        Path projectPath = appConfig.getBackendPath().resolve(projectName);

        if (!Files.exists(projectPath)) {
            throw new IOException("Project not found: " + projectPath);
        }

        // 1. Map to store all discovered class names to avoid linking to external libs (like String, List)
        Map<String, String> projectClasses = new HashMap<>();

        // First Pass: Identify all Nodes (Classes)
        try (Stream<Path> stream = Files.walk(projectPath)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(path);
                    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
                        String className = c.getNameAsString();
                        String type = determineType(c);
                        
                        // Store for second pass
                        projectClasses.put(className, type);

                        // Add Node to Graph
                        graph.getNodes().add(new ArchitectureGraph.Node(className, className, type));
                    });
                } catch (Exception ignored) {}
            });
        }

        // Second Pass: Identify Edges (Relationships)
        try (Stream<Path> stream = Files.walk(projectPath)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(path);
                    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(sourceClass -> {
                        String sourceName = sourceClass.getNameAsString();

                        // 1. Check Fields (Dependency Injection)
                        sourceClass.findAll(FieldDeclaration.class).forEach(field -> {
                            field.getVariables().forEach(variable -> {
                                String targetType = variable.getType().asString();
                                // Clean up generic types e.g., List<Project> -> Project
                                if(targetType.contains("<")) {
                                    targetType = targetType.substring(targetType.indexOf("<") + 1, targetType.indexOf(">"));
                                }
                                
                                // Only create edge if target is part of this project
                                if (projectClasses.containsKey(targetType)) {
                                    graph.getEdges().add(new ArchitectureGraph.Edge(sourceName, targetType, "USES"));
                                }
                            });
                        });

                        // 2. Check Extensions (Inheritance)
                        sourceClass.getExtendedTypes().forEach(extendedType -> {
                            String targetType = extendedType.getNameAsString();
                            if (projectClasses.containsKey(targetType)) {
                                graph.getEdges().add(new ArchitectureGraph.Edge(sourceName, targetType, "EXTENDS"));
                            }
                        });
                    });
                } catch (Exception ignored) {}
            });
        }

        return graph;
    }

    private String determineType(ClassOrInterfaceDeclaration c) {
        if (c.isAnnotationPresent("RestController") || c.isAnnotationPresent("Controller")) return "CONTROLLER";
        if (c.isAnnotationPresent("Service")) return "SERVICE";
        if (c.isAnnotationPresent("Repository")) return "REPOSITORY";
        if (c.isAnnotationPresent("Entity")) return "ENTITY";
        if (c.isAnnotationPresent("Component")) return "COMPONENT";
        if (c.isAnnotationPresent("Configuration")) return "CONFIG";
        return "OTHER";
    }
}