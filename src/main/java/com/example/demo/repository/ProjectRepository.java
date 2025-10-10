package com.example.demo.repository;

import com.example.demo.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// @Repository is optional here as JpaRepository already implies it
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    // JpaRepository provides methods like findAll(), findById(String name), save(), deleteById(String name)
    // You can add custom query methods here if needed
}
