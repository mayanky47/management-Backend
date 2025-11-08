package com.example.demo.controller.projectController;

import com.example.demo.model.projectModel.Project;
import com.example.demo.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;


@RestController
@RequestMapping("/api/open") // Base path for open-related endpoints

public class OpenProjectInIDEController {

    @Autowired
    private ProjectRepository projectRepository;

    // Path to the open_project.bat script (adjust this if you place it elsewhere)
    // Assuming it's in the root of the Spring Boot project or a known relative path
    private static final String OPEN_PROJECT_SCRIPT_PATH = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "open_project.bat";


    /**
     * Attempts to open a project by executing a local batch script.
     * This method first tries to find the project in the database. If not found,
     * it attempts to use the provided path and type from the request body.
     *
     * @param projectDetails A Project object containing at least name, path, and type.
     * @return ResponseEntity indicating success or failure of the operation.
     */
    @PostMapping("/projects") // Changed endpoint to accept a body, removed {name} from path
    public ResponseEntity<String> openProject(@RequestBody Project projectDetails) {
        String projectName = projectDetails.getName();
        String projectPath = projectDetails.getPath();
        String projectType = projectDetails.getType();

        // Try to get project details from DB first (authoritative source)
        Optional<Project> dbProjectOptional = projectRepository.findById(projectName);
        Project projectToOpen;

        if (dbProjectOptional.isPresent()) {
            projectToOpen = dbProjectOptional.get();
            // Use DB's path and type if available and more accurate
            projectPath = projectToOpen.getPath();
            projectType = projectToOpen.getType();
        } else {
            // If not in DB, ensure path and type are provided in the request body
            if (projectPath == null || projectPath.trim().isEmpty() ||
                    projectType == null || projectType.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Project not found in DB and insufficient details provided in request body (path or type missing).");
            }
            // Use details from the request body
            projectToOpen = projectDetails; // Use the provided details for logging/script
        }

        try {
            // Construct the command to execute the batch script
            // Use 'cmd.exe /c' to run batch files on Windows
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", OPEN_PROJECT_SCRIPT_PATH, projectPath, projectType);
            // Set the working directory for the script if it's not in the default app dir
            // For example, if 'open_project.bat' is in a 'scripts' folder at the root of your project:
            // processBuilder.directory(new File(System.getProperty("user.dir") + File.separator + "scripts"));

            Process process = processBuilder.start();

            // Read the output/error streams of the batch script for debugging
            // It's good practice to consume these streams to prevent deadlocks on some OSes
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        System.out.println("SCRIPT OUT: " + line); // Log script's standard output
                    }
                } catch (IOException e) {
                    System.err.println("Error reading script output: " + e.getMessage());
                }
            }).start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                        System.err.println("SCRIPT ERR: " + line); // Log script's error output
                    }
                } catch (IOException e) {
                    System.err.println("Error reading script error: " + e.getMessage());
                }
            }).start();


            int exitCode = process.waitFor(); // Wait for the script to finish
            if (exitCode == 0) {
                System.out.println("Successfully attempted to open project '" + projectToOpen.getName() + "' at path: " + projectPath);
                return ResponseEntity.ok("Project '" + projectName + "' opened successfully (via script).");
            } else {
                System.err.println("Script to open project '" + projectToOpen.getName() + "' exited with code: " + exitCode);
                System.err.println("Script Error Output:\n" + error.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to open project '" + projectName + "'. Script exited with code " + exitCode + ". Error: " + error.toString());
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing script to open project '" + projectToOpen.getName() + "': " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing open script: " + e.getMessage());
        }
    }
}

