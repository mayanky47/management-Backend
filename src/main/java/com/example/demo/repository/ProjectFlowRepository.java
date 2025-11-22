package com.example.demo.repository;

import com.example.demo.model.flowModel.ProjectFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectFlowRepository extends JpaRepository<ProjectFlow, Long> {
    List<ProjectFlow> findByProjectName(String projectName);
}