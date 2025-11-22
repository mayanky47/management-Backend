package com.example.demo.service.planService;

import com.example.demo.model.planner.ProjectPlan;
import com.example.demo.model.planner.ProjectStrategyVersion;
import com.example.demo.repository.ProjectPlanRepository;
import com.example.demo.repository.ProjectStrategyVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProjectPlanService {

    @Autowired
    private ProjectPlanRepository projectRepository;

    @Autowired
    private ProjectStrategyVersionRepository versionRepository;

    // ... (findAllProjects, findProjectById, saveProject methods are correct) ...
    public List<ProjectPlan> findAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<ProjectPlan> findProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public ProjectPlan saveProject(ProjectPlan project) {
        return projectRepository.save(project);
    }

    @Transactional
    public ProjectPlan updateProject(Long id, ProjectPlan projectDetails) {
        ProjectPlan existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        String oldPlan = existingProject.getStrategyPlan();
        String newPlan = projectDetails.getStrategyPlan();

        if (newPlan != null && !Objects.equals(oldPlan, newPlan)) {
            if (oldPlan != null && !oldPlan.trim().isEmpty()) {
                ProjectStrategyVersion version = new ProjectStrategyVersion(
                        existingProject,
                        oldPlan
                );
                existingProject.getStrategyVersions().add(version);
            }
            existingProject.setStrategyPlan(newPlan);
        }

        // --- FIX: Conditionally update fields to avoid nulling them out ---
        if (projectDetails.getName() != null) {
            existingProject.setName(projectDetails.getName());
        }
        if (projectDetails.getDescription() != null) {
            existingProject.setDescription(projectDetails.getDescription());
        }
        if (projectDetails.getStatus() != null) {
            existingProject.setStatus(projectDetails.getStatus());
        }
        if (projectDetails.getStartDate() != null) {
            existingProject.setStartDate(projectDetails.getStartDate());
        }
        if (projectDetails.getEndDate() != null) {
            existingProject.setEndDate(projectDetails.getEndDate());
        }
        if (projectDetails.getMarkdownPlan() != null) {
            existingProject.setMarkdownPlan(projectDetails.getMarkdownPlan());
        }
        // Always update the plan, as the history logic is based on its change


        return projectRepository.save(existingProject);
    }

    public List<ProjectStrategyVersion> findProjectStrategyHistory(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        return versionRepository.findByProjectIdOrderByVersionTimestampDesc(projectId);
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}