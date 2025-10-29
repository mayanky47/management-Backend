package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPackage {
    @Id
    @Column(name = "package_name", nullable = false)
    private String name;

    // Package-specific fields
    private String purpose;
    private String status;
    private String priority;
    private String dueDate;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "package_name") // foreign key in Project table
    private List<Project> projects = new ArrayList<>();
}
