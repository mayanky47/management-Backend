package com.example.demo.repository;


import com.example.demo.model.planner.ProjectStrategyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectStrategyVersionRepository extends JpaRepository<ProjectStrategyVersion, Long> {

    /**
     * Finds all strategy versions for a given project, ordered by the most recent first.
     * Spring Data JPA automatically implements this method based on its name.
     *
     * @param projectId The ID of the project.
     * @return A list of historical versions.
     */
    List<ProjectStrategyVersion> findByProjectIdOrderByVersionTimestampDesc(Long projectId);
}