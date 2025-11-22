package com.example.demo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Getter
public class AppConfig {

    @Value("${app.projects.base-dir}")
    private String baseDir;

    /**
     * Returns the root path as a Path object.
     */
    public Path getRootPath() {
        return Paths.get(baseDir);
    }

    /**
     * Returns the backend directory (e.g., D:/project/projects/backend).
     */
    public Path getBackendPath() {
        return getRootPath().resolve("backend");
    }

    /**
     * Returns the frontend directory (e.g., D:/project/projects/frontend).
     */
    public Path getFrontendPath() {
        return getRootPath().resolve("frontend");
    }
}