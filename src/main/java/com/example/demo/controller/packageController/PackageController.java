package com.example.demo.controller.packageController;

import com.example.demo.model.projectModel.Project;
import com.example.demo.model.packageModel.ProjectPackage;

import com.example.demo.repository.ProjectPackageRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.service.ProjectCreationService;
import com.example.demo.service.packageService.ProjectPackageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/packages") // Dedicated base path for Package/Portfolio endpoints
public class PackageController {

    @Autowired
    private ProjectPackageRepository projectPackageRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectCreationService projectCreationService;

    @Autowired
    private ProjectPackageService projectPackageService;

    private static final String DEFAULT_PROJECT_BASE_DIR = "D:\\project\\projects";
    private static final String DB_MANAGED_PATH = "DB_MANAGED_PACKAGE";


    /**
     * Helper to set default/calculated fields for a new Project created as part of a Package.
     * This ensures the new project is complete before saving.
     */



    // --- READ Operations (No change) ---

    @GetMapping
    public ResponseEntity<List<ProjectPackage>> getAllPackages() {
        return ResponseEntity.ok(projectPackageRepository.findAll());
    }

    @GetMapping("/{name}")
    public ResponseEntity<ProjectPackage> getPackageByName(@PathVariable String name) {
        Optional<ProjectPackage> pkg = projectPackageRepository.findById(name);
        return pkg.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- CREATE Operation (MODIFIED for Project Creation) ---

    /**
     * Creates a new Package and instantiates new Project entities as its sub-projects.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<ProjectPackage> createPackage(@RequestBody ProjectPackage pkg) {
        try {
            ProjectPackage createdPackage = projectPackageService.createPackage(pkg);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPackage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }


    // --- UPDATE Operation (MODIFIED to manage sub-project additions/removals) ---
    // If the client sends a new list of projects, we assume this is the desired final state.

    @PutMapping("/{name}")
    @Transactional
    public ResponseEntity<ProjectPackage> updatePackage(@PathVariable String name, @RequestBody ProjectPackage updatedPackage) {
        Optional<ProjectPackage> existingPackageOpt = projectPackageRepository.findById(name);

        if (!existingPackageOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ProjectPackage existingPackage = existingPackageOpt.get();

        // 1. Update basic package fields (metadata)
        existingPackage.setPurpose(updatedPackage.getPurpose());
        existingPackage.setStatus(updatedPackage.getStatus());
        existingPackage.setPriority(updatedPackage.getPriority());
        existingPackage.setDueDate(updatedPackage.getDueDate());




        // 3. Save the package entity (this handles basic metadata updates)
        ProjectPackage savedPackage = projectPackageRepository.save(existingPackage);

        // 4. Re-fetch the package to ensure the response includes the correct and updated list of projects
        return projectPackageRepository.findById(savedPackage.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
    }

    // --- DELETE Operation (No change needed) ---

    @DeleteMapping("/{name}")
    @Transactional
    public ResponseEntity<Void> deletePackage(@PathVariable String name) {
        Optional<ProjectPackage> pkgOpt = projectPackageRepository.findById(name);

        if (pkgOpt.isPresent()) {
            ProjectPackage pkg = pkgOpt.get();

            // 2. Now delete the package itself
            projectPackageRepository.delete(pkg);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}