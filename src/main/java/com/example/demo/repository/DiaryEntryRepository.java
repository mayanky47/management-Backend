package com.example.demo.repository;

import com.example.demo.model.diaryModel.DiaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    // You can add custom query methods here, e.g., to find entries by date
    List<DiaryEntry> findAllByOrderByEntryDateDesc();
}