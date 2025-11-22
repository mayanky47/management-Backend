package com.example.demo.service.packageService;

import com.example.demo.model.packageModel.ProjectPackage;
import com.example.demo.model.projectModel.Project;
import com.example.demo.repository.ProjectPackageRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.service.ProjectCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectPackageService {
    private final ProjectPackageRepository projectPackageRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCreationService projectCreationService;

    public ProjectPackage createPackage(ProjectPackage pkg) throws Exception {
        validatePackage(pkg);

        if (projectPackageRepository.existsById(pkg.getName())) {
            throw new Exception("Package name already exists: " + pkg.getName());
        }

        List<Project> projectsToCreate = pkg.getProjects();

        if (projectsToCreate != null) {
            for (Project project : projectsToCreate) {
                if(!projectExist(project)) {
                    createAndSaveProject(project);
                }
            }
        }

        // Save the package after all subprojects are created
        ProjectPackage savedPackage = projectPackageRepository.save(pkg);

        // Return the fully fetched package with all associated projects
        return projectPackageRepository.findById(savedPackage.getName())
                .orElseThrow(() -> new RuntimeException("Failed to fetch saved package"));
    }

    private void validatePackage(ProjectPackage pkg) {
        if (pkg.getName() == null || pkg.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty.");
        }
    }

    private boolean projectExist(Project project) {
        if (projectRepository.existsById(project.getName())) {
            return true;
        }
        return false;
    }

    private void createAndSaveProject(Project project) {
        System.out.println("Creating sub-project: " + project.getName() + " of type: " + project.getType());
        try {
            projectCreationService.createProject(project);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create project: " + project.getName(), e);
        }

        projectRepository.save(project);
    }
}
