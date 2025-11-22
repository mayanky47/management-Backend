package com.example.demo.model.diaryModel;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data // Lombok annotation for getters, setters, toString, etc.
public class DiaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;

    // The date the entry was created
    private LocalDate entryDate = LocalDate.now();

    // You can add more fields like mood, weather, tags, etc.
    private String mood;
}