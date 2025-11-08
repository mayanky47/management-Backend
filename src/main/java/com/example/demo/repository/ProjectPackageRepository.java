package com.example.demo.repository;

import com.example.demo.model.packageModel.ProjectPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectPackageRepository extends JpaRepository<ProjectPackage, String> {
    // You can add custom query methods here if needed
}