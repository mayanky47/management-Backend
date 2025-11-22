package com.example.demo.model.graphModel;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArchitectureGraph {
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();

    @Data
    @AllArgsConstructor
    public static class Node {
        private String id;      // Unique ID (usually class name)
        private String label;   // Display name
        private String type;    // CONTROLLER, SERVICE, REPOSITORY, ENTITY, UNKNOWN
    }

    @Data
    @AllArgsConstructor
    public static class Edge {
        private String source;  // ID of source node
        private String target;  // ID of target node
        private String relation; // e.g., "USES", "EXTENDS"
    }
}