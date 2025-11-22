package com.example.demo.repository;

import com.example.demo.model.planner.ProjectPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectPlanRepository extends JpaRepository<ProjectPlan, Long> {
    // Custom query to ensure projects are returned ordered by creation date, descending
    List<ProjectPlan> findAllByOrderByCreatedAtDesc();
}
