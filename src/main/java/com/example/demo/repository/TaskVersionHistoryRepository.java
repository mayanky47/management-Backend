package com.example.demo.repository;


import com.example.demo.model.planner.TaskVersionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskVersionHistoryRepository extends JpaRepository<TaskVersionHistory, Long> {
    // Custom query to fetch all history entries for a given taskId, ordered by timestamp
    List<TaskVersionHistory> findByTaskIdOrderByVersionTimestampAsc(Long taskId);
}

