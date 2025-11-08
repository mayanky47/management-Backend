package com.example.demo.service;

import com.example.demo.model.projectModel.Project;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class DependencyService {

    private static final String SQLITE_JDBC_DEPENDENCY =
            "        <dependency>\n" +
            "            <groupId>org.xerial</groupId>\n" +
            "            <artifactId>sqlite-jdbc</artifactId>\n" +
            "            <version>3.45.1.0</version>\n" +
            "        </dependency>\n";

    private static final String HIBERNATE_DIALECT_DEPENDENCY =
            "        <!-- Correct Hibernate Dialect for SQLite (for Hibernate 6 / Spring Boot 3.x) -->\n" +
            "        <dependency>\n" +
            "            <groupId>org.hibernate.orm</groupId>\n" +
            "            <artifactId>hibernate-community-dialects</artifactId>\n" +
            "        </dependency>\n";

    private static final String SQLITE_PROPERTIES =
            "\n# --- SQLite Configuration ---\n" +
            "spring.datasource.url=jdbc:sqlite:projecthub.db\n" +
            "spring.datasource.driver-class-name=org.sqlite.JDBC\n" +
            "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect\n" +
            "spring.jpa.hibernate.ddl-auto=update\n" +
            "spring.jpa.show-sql=true\n";

    /**
     * Adds SQLite dependencies to pom.xml and properties to application.properties.
     * @param project The Spring project to modify.
     * @throws IOException If file operations fail.
     * @throws IllegalStateException If the project is not a Spring project or files are missing.
     */
    public void addSQLiteSupport(Project project) throws IOException, IllegalStateException {
        if (!"Spring".equals(project.getType())) {
            throw new IllegalStateException("Dependency management is only supported for Spring projects.");
        }

        Path projectPath = Paths.get(project.getPath());
        Path pomPath = projectPath.resolve("pom.xml");
        Path propsPath = projectPath.resolve("src/main/resources/application.properties");

        if (!Files.exists(pomPath)) {
            throw new IOException("pom.xml not found at: " + pomPath);
        }
        if (!Files.exists(propsPath)) {
            // Create it if it doesn't exist
            Files.createDirectories(propsPath.getParent());
            Files.createFile(propsPath);
        }

        // 1. Modify pom.xml
        addDependenciesToPom(pomPath);

        // 2. Modify application.properties
        addPropertiesToConfig(propsPath);
    }

    private void addDependenciesToPom(Path pomPath) throws IOException {
        String content = Files.readString(pomPath);

        // Check if dependencies already exist
        if (content.contains("<artifactId>sqlite-jdbc</artifactId>")) {
            System.out.println("sqlite-jdbc dependency already exists in " + pomPath);
            return; // Already added
        }

        // Find the </dependencies> tag and insert before it
        int dependenciesEndIndex = content.lastIndexOf("</dependencies>");
        if (dependenciesEndIndex == -1) {
            // If no <dependencies> block, we'd need a more complex XML parser.
            // For now, assume it exists.
            throw new IOException("<dependencies> block not found in pom.xml. Automatic injection failed.");
        }

        StringBuilder newContent = new StringBuilder(content);
        // Insert new dependencies just before the closing </dependencies> tag
        newContent.insert(dependenciesEndIndex, SQLITE_JDBC_DEPENDENCY + HIBERNATE_DIALECT_DEPENDENCY);

        Files.writeString(pomPath, newContent.toString(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void addPropertiesToConfig(Path propsPath) throws IOException {
        String content = Files.readString(propsPath);

        // Check if properties already exist
        if (content.contains("spring.datasource.driver-class-name=org.sqlite.JDBC")) {
            System.out.println("SQLite properties already exist in " + propsPath);
            return; // Already added
        }

        // Append the properties to the end of the file
        Files.writeString(propsPath, SQLITE_PROPERTIES, StandardOpenOption.APPEND);
    }
}
