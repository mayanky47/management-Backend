package com.example.demo.service;

import com.example.demo.model.Project;
import com.example.demo.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ProjectCreationService {

    private static final String CREATE_REACT_SCRIPT_PATH = System.getProperty("user.dir")
            + File.separator + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "create_react_project.bat";

    private static final String DEFAULT_PROJECT_BASE_DIR = "D:\\project\\projects";

    @Autowired
    private ProjectRepository projectRepository;

    public Project createProject(Project project) throws IOException, InterruptedException {

        // Validate project
        if (project.getName() == null || project.getName().trim().isEmpty() ||
                project.getType() == null || project.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name and type cannot be empty.");
        }

        // Check if project exists
        if (projectRepository.existsById(project.getName())) {
            throw new IllegalStateException("Project with name '" + project.getName() + "' already exists.");
        }

        String subfolder = "";
        String creationCommand = "";
        Path workingDirectory;

        switch (project.getType()) {
            case "React" -> {
                subfolder = "frontend";
                workingDirectory = Paths.get(System.getProperty("user.dir")); // Script runs from project root

                File scriptFile = new File(CREATE_REACT_SCRIPT_PATH);
                if (!scriptFile.exists() || !scriptFile.isFile()) {
                    throw new IOException("React project creation script not found at: " + CREATE_REACT_SCRIPT_PATH);
                }

                creationCommand = CREATE_REACT_SCRIPT_PATH + " \"" + project.getName() + "\" \"" +
                        DEFAULT_PROJECT_BASE_DIR + File.separator + "frontend" + "\"";
            }
            case "Spring" -> {
                subfolder = "backend";
                workingDirectory = Paths.get(DEFAULT_PROJECT_BASE_DIR + File.separator + "backend");
                creationCommand = "spring init --dependencies=web,jpa --build=maven --packaging=jar --java-version=17 --name="
                        + project.getName() + " " + project.getName();
            }
            default -> {
                subfolder = "";
                workingDirectory = Paths.get(DEFAULT_PROJECT_BASE_DIR);
                Path projectDir = workingDirectory.resolve(project.getName());
                Files.createDirectories(projectDir);
                project.setPath(projectDir.toString());
                return projectRepository.save(project);
            }
        }

        project.setPath(DEFAULT_PROJECT_BASE_DIR + File.separator + subfolder + File.separator + project.getName());
        executeCommand(creationCommand, workingDirectory, project.getName());
        return projectRepository.save(project);
    }

    private void executeCommand(String command, Path workingDirectory, String projectName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        processBuilder.directory(workingDirectory.toFile());

        Process process = processBuilder.start();

        // Capture output
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("CREATE SCRIPT OUT: " + line);
                }
            } catch (IOException ignored) {}
        }).start();

        // Capture errors
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("CREATE SCRIPT ERR: " + line);
                }
            } catch (IOException ignored) {}
        }).start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // Cleanup if failed
            Path createdDir = workingDirectory.resolve(projectName);
            if (Files.exists(createdDir)) {
                Files.walk(createdDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                System.err.println("Cleaned up partially created directory: " + createdDir);
            }
            throw new IOException("Failed to create project '" + projectName + "'. Exit code: " + exitCode);
        }
    }
}
