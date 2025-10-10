package com.example.demo.controller;

import com.example.demo.model.Project;
import com.example.demo.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/create") // Base path for create-related endpoints
@CrossOrigin(origins = "http://localhost:5173") // Allow requests from your React frontend
public class CreateProjectController {

    private static final String CREATE_REACT_SCRIPT_PATH = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "create_react_project.bat";

    @Autowired
    private ProjectRepository projectRepository;

    // Define the fixed base directory for project discovery and creation
    private static final String DEFAULT_PROJECT_BASE_DIR = "D:\\project\\projects";

    /**
     * Creates a new project.
     * The project's name is used as its ID. The path is constructed based on type and name
     * using the fixed default base directory. The project is then saved to the database.
     * After saving, it attempts to create the physical project directory using command-line tools.
     * Returns 409 Conflict if a project with the same name already exists.
     */
    @PostMapping("/projects") // Endpoint for creating a project
    public ResponseEntity<Object> createProject(@RequestBody Project project) {
        if (project.getName() == null || project.getName().trim().isEmpty() ||
                project.getType() == null || project.getType().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Project name and type cannot be empty.");
        }

        // Check if a project with this name already exists in the database
        if (projectRepository.existsById(project.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Project with name '" + project.getName() + "' already exists.");
        }

        String subfolder = "";
        String creationCommand = "";
        Path workingDirectory = null; // The directory where the creation command will be run


        if ("React".equals(project.getType())) {
            subfolder = "frontend\\";
            workingDirectory = Paths.get(DEFAULT_PROJECT_BASE_DIR + File.separator + "frontend");

            // Verify the batch script exists before attempting to execute
            File scriptFile = new File(CREATE_REACT_SCRIPT_PATH);
            if (!scriptFile.exists() || !scriptFile.isFile()) {
                String errorMessage = "Error: React project creation script not found at expected path: " + CREATE_REACT_SCRIPT_PATH;
                System.err.println(errorMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage + ". Please ensure 'create_react_project.bat' is in your 'src/main/resources' folder.");
            }

            // Command to execute the new batch script for React project creation
            // Pass project name and parent directory as arguments
            creationCommand = CREATE_REACT_SCRIPT_PATH + " \"" + project.getName() + "\" \"" + workingDirectory.toString() + "\"";
            // The working directory for ProcessBuilder will be the project's root,
            // as the batch script handles changing to the parent directory.
            workingDirectory = Paths.get(System.getProperty("user.dir")); // Set to project root where the script is found

        } else if ("Spring".equals(project.getType())) {
            subfolder = "backend\\";
            workingDirectory = Paths.get(DEFAULT_PROJECT_BASE_DIR + File.separator + "backend");
            // Command to initialize a Spring Boot project using Spring Boot CLI
            // Using SQLite dependencies as per your current DB setup
            creationCommand = "spring init --dependencies=web,jpa --build=maven --packaging=jar --java-version=17 --name=" + project.getName() + " " + project.getName();
        } else {
            // For other types, just create the directory for now
            subfolder = ""; // No specific subfolder for 'Other' unless specified
            workingDirectory = Paths.get(DEFAULT_PROJECT_BASE_DIR + File.separator + subfolder);
            try {
                Files.createDirectories(workingDirectory.resolve(project.getName()));
                project.setPath(workingDirectory.resolve(project.getName()).toString());
                Project savedProject = projectRepository.save(project);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedProject);
            } catch (IOException e) {
                System.err.println("Error creating directory for project '" + project.getName() + "': " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create project directory: " + e.getMessage());
            }
        }

        // Construct the full path where the project will be created
        project.setPath(DEFAULT_PROJECT_BASE_DIR + File.separator + subfolder + project.getName());

        // Execute the project creation command
        try {
            // Use 'cmd.exe /c' to run commands on Windows
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", creationCommand);
            processBuilder.directory(workingDirectory.toFile()); // Set the working directory for the command

            System.out.println("Executing command: " + creationCommand + " in directory: " + workingDirectory.toString());

            Process process = processBuilder.start();

            // Capture output and error streams for debugging
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        System.out.println("CREATE SCRIPT OUT: " + line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading create script output: " + e.getMessage());
                }
            }).start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                        System.err.println("CREATE SCRIPT ERR: " + line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading create script error: " + e.getMessage());
                }
            }).start();

            int exitCode = process.waitFor(); // Wait for the command to finish

            if (exitCode == 0) {
                System.out.println("Project '" + project.getName() + "' created successfully on file system.");
                Project savedProject = projectRepository.save(project); // Save project details to DB
                return ResponseEntity.status(HttpStatus.CREATED).body(savedProject);
            } else {
                String errorMessage = "Failed to create project '" + project.getName() + "'. Command exited with code " + exitCode + ". Error: " + error.toString();
                System.err.println(errorMessage);
                // Attempt to delete the partially created directory if command failed
                try {
                    Path createdDirPath = workingDirectory.resolve(project.getName());
                    if (Files.exists(createdDirPath)) {
                        // For recursive delete (more robust):
                        Files.walk(createdDirPath)
                                .sorted(java.util.Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                        System.out.println("Cleaned up partially created directory: " + createdDirPath);
                    }
                } catch (IOException cleanupException) {
                    System.err.println("Error during cleanup of failed project creation: " + cleanupException.getMessage());
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing project creation command for '" + project.getName() + "': " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during project creation: " + e.getMessage());
        }
    }
}
