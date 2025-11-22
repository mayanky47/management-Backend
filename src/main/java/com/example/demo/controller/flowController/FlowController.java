package com.example.demo.controller.flowController;

import com.example.demo.model.flowModel.ProjectFlow;
import com.example.demo.repository.ProjectFlowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flows")
public class FlowController {

    @Autowired
    private ProjectFlowRepository repository;

    @GetMapping("/{projectName}")
    public List<ProjectFlow> getProjectFlows(@PathVariable String projectName) {
        return repository.findByProjectName(projectName);
    }

    @PostMapping
    public ProjectFlow createFlow(@RequestBody ProjectFlow flow) {
        return repository.save(flow);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectFlow> updateFlow(@PathVariable Long id, @RequestBody ProjectFlow flowDetails) {
        return repository.findById(id).map(flow -> {
            flow.setName(flowDetails.getName());
            flow.setDescription(flowDetails.getDescription());
            flow.setFlowData(flowDetails.getFlowData());
            return ResponseEntity.ok(repository.save(flow));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlow(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}