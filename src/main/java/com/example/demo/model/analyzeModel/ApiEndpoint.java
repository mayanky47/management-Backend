package com.example.demo.model.analyzeModel;

import lombok.Data;

@Data
public class ApiEndpoint {
    private String httpMethod;
    private String path;
    private String controller;
}
