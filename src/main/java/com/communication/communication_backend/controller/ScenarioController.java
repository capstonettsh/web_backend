package com.communication.communication_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.communication.communication_backend.service.creatingScenarios.ScenarioService;
import com.communication.communication_backend.dtos.GeneratedScenario;
import com.communication.communication_backend.dtos.MarkingSchema;
import com.communication.communication_backend.dtos.Scenario;
import com.communication.communication_backend.dtos.ScenarioSummary;
import com.communication.communication_backend.service.creatingScenarios.ScenariosOpenAiClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/config")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    // Initialize a new scenario with an auto-generated configId
    @PostMapping("/initialize")
    public ResponseEntity<Integer> initializeScenario() {
        int configId = scenarioService.initializeConfigId();
        return ResponseEntity.ok(configId);
    }

    // Save the scenario details (title, shortDescription, prompt)
    @PostMapping("/{configId}/scenario-prompt")
    public ResponseEntity<String> saveScenario(@PathVariable int configId, @RequestBody Scenario scenario) {
        scenarioService.saveScenario(configId, scenario);
        return ResponseEntity.ok("Scenario saved successfully with configId: " + configId);
    }

    // Retrieve a specific scenario's details
    @GetMapping("/{configId}/scenario-prompt")
    public ResponseEntity<Scenario> getScenario(@PathVariable int configId) {
        return scenarioService.getScenarioById(configId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Add a scenario summary to the global list (for the overview page)
    @PostMapping
    public ResponseEntity<String> saveScenarioSummary(@RequestBody ScenarioSummary summary) {
        Scenario scenario = new Scenario(summary.getConfigId(), summary.getTitle(), "", "");
        scenarioService.saveScenario(summary.getConfigId(), scenario);
        return ResponseEntity.ok("Scenario summary saved successfully.");
    }

    // Retrieve all scenario summaries
    @GetMapping
    public ResponseEntity<List<ScenarioSummary>> getAllScenarioSummaries() {
        List<ScenarioSummary> summaries = scenarioService.getAllScenarioSummaries();
        return ResponseEntity.ok(summaries);
    }

    // Delete a scenario by configId
    @DeleteMapping("/{configId}")
    public ResponseEntity<String> deleteScenario(@PathVariable int configId) {
        boolean isDeleted = scenarioService.deleteScenario(configId);
        return isDeleted ? ResponseEntity.ok("Scenario deleted successfully.") : ResponseEntity.notFound().build();
    }

    // Generate scenario via AI based on existing title, description, and prompt
    @GetMapping("/{configId}/scenario/generate")
    public ResponseEntity<GeneratedScenario> generateScenario(@PathVariable int configId) {
        try {
            GeneratedScenario generatedScenario = scenarioService.generateScenario(configId);
            return ResponseEntity.ok(generatedScenario);
        } catch (Exception e) {
            e.printStackTrace(); // Log error for debugging
            return ResponseEntity.status(500).body(null);
        }
    }

    // Save the generated scenario (AI-generated)
    @PostMapping("/{configId}/scenario")
    public ResponseEntity<String> saveGeneratedScenario(@PathVariable int configId, @RequestBody GeneratedScenario generatedScenario) {
        try {
            scenarioService.saveGeneratedScenario(configId, generatedScenario);
            return ResponseEntity.ok("Generated scenario saved successfully for configId: " + configId);
        } catch (Exception e) {
            e.printStackTrace(); // Log error for debugging
            return ResponseEntity.status(500).body("Failed to save generated scenario.");
        }
    }

    // Retrieve saved generated scenario
    @GetMapping("/{configId}/scenario")
    public ResponseEntity<GeneratedScenario> getGeneratedScenario(@PathVariable int configId) {
        try {
            Optional<GeneratedScenario> generatedScenario = scenarioService.getGeneratedScenario(configId);
            return generatedScenario.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace(); // Log error for debugging
            return ResponseEntity.status(500).body(null);
        }
    }

    // Generate a new marking schema
    @GetMapping("/{configId}/marking-schema/generate")
    public ResponseEntity<MarkingSchema> generateMarkingSchema(@PathVariable int configId, @RequestParam String title) {
        try {
            MarkingSchema markingSchema = scenarioService.generateMarkingSchema(configId, title);
            return ResponseEntity.ok(markingSchema);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Save a marking schema
    @PostMapping("/{configId}/marking-schema")
    public ResponseEntity<String> saveMarkingSchema(@PathVariable int configId, @RequestBody MarkingSchema markingSchema) {
        try {
            scenarioService.saveMarkingSchema(configId, markingSchema);
            return ResponseEntity.ok("Marking schema saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to save marking schema.");
        }
    }

    // Retrieve all marking schemas for a scenario
    @GetMapping("/{configId}/marking-schema")
    public ResponseEntity<List<MarkingSchema>> getMarkingSchemas(@PathVariable int configId) {
        try {
            return ResponseEntity.ok(scenarioService.getMarkingSchemas(configId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // Delete a marking schema
    @DeleteMapping("/{configId}/marking-schema/{schemaId}")
    public ResponseEntity<String> deleteMarkingSchema(@PathVariable int configId, @PathVariable int schemaId) {
        boolean deleted = scenarioService.deleteMarkingSchema(configId, schemaId);
        return deleted ? ResponseEntity.ok("Marking schema deleted.") : ResponseEntity.notFound().build();
    }
}