package com.communication.communication_backend.controller;

import com.communication.communication_backend.dtos.ScenarioSummary;
import com.communication.communication_backend.entity.MarkingSchema;
import com.communication.communication_backend.entity.Scenario;
import com.communication.communication_backend.service.creatingScenarios.ScenarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    // Initialize a new scenario with an auto-generated configId
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Integer>> initializeScenario() {
        int configId = scenarioService.initializeScenarioId();
        return ResponseEntity.ok(Collections.singletonMap("configId", configId));
    }

    // Save Part 1: Basic scenario details (title, shortDescription, prompt, userId)
    @PostMapping("/{configId}/scenario-prompt")
    public ResponseEntity<Map<String, String>> saveBasicScenario(@PathVariable("configId") int configId,
                                                                 @RequestBody Scenario scenario) {
        scenarioService.saveBasicScenario(configId, scenario);
        return ResponseEntity.ok(Collections.singletonMap("message", "success"));
    }

    // Save Part 2: Additional scenario details (taskInstruction, backgroundInformation, personality, questionsForDoctor, responseGuidelines, sampleResponses)
    @PostMapping("/{configId}/scenario-details")
    public ResponseEntity<Map<String, String>> saveScenarioDetails(@PathVariable("configId") int configId,
                                                                   @RequestBody Scenario scenario) {
        scenarioService.saveScenarioDetails(configId, scenario);
        return ResponseEntity.ok(Collections.singletonMap("message", "Scenario additional details saved successfully."));
    }

    // Retrieve a specific scenario's details
    // @GetMapping("/{configId}/scenario-prompt")
    // public ResponseEntity<Scenario> getScenario(@PathVariable int configId) {
    //     return scenarioService.getScenarioById(configId)
    //             .map(ResponseEntity::ok)
    //             .orElse(ResponseEntity.notFound().build());
    // }

    // Retrieve full scenario details (both basic and additional)
    @GetMapping("/{configId}/scenario")
    public ResponseEntity<Scenario> getScenario(@PathVariable int configId) {
        return scenarioService.getScenarioById(configId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Add a scenario summary to the global list (for the overview page)
//    @PostMapping
//    public ResponseEntity<String> saveScenarioSummary(@RequestBody ScenarioSummary summary) {
//        Scenario scenario = new Scenario(summary.getConfigId(), summary.getTitle(), "", "", summary.getUserId());
//        scenarioService.saveScenario(summary.getConfigId(), scenario);
//        return ResponseEntity.ok("Scenario summary saved successfully.");
//    }


    // Retrieve all scenario summaries
    @GetMapping
    public ResponseEntity<List<ScenarioSummary>> getAllScenarioSummaries() {
        List<ScenarioSummary> summaries = scenarioService.getAllScenarioSummaries();
        return ResponseEntity.ok(summaries);
    }

    // Delete a scenario by configId
    @DeleteMapping("/{configId}")
    public ResponseEntity<String> deleteScenario(@PathVariable("configId") int configId) {
        boolean isDeleted = scenarioService.deleteScenario(configId);
        return isDeleted ? ResponseEntity.ok("Scenario deleted successfully.") : ResponseEntity.notFound().build();
    }

    // Generate scenario via AI based on existing title, description, and prompt
    @GetMapping("/{configId}/scenario/generate")
    public ResponseEntity<Scenario> generateScenario(@PathVariable("configId") int configId) {
        try {
            Scenario updatedScenario = scenarioService.generateScenario(configId);
            return ResponseEntity.ok(updatedScenario);
        } catch (Exception e) {
            e.printStackTrace(); // Log error for debugging
            return ResponseEntity.status(500).body(null);
        }
    }

    // Save the generated scenario (AI-generated)
    // @PostMapping("/{configId}/scenario")
    // public ResponseEntity<String> saveGeneratedScenario(@PathVariable("configId") int configId, @RequestBody GeneratedScenario generatedScenario) {
    //     try {
    //         scenarioService.saveGeneratedScenario(configId, generatedScenario);
    //         return ResponseEntity.ok("Generated scenario saved successfully for configId: " + configId);
    //     } catch (Exception e) {
    //         e.printStackTrace(); // Log error for debugging
    //         return ResponseEntity.status(500).body("Failed to save generated scenario.");
    //     }
    // }

    // Retrieve saved generated scenario
    // @GetMapping("/{configId}/scenario")
    // public ResponseEntity<GeneratedScenario> getGeneratedScenario(@PathVariable("configId") int configId) {
    //     try {
    //         Optional<GeneratedScenario> generatedScenario = scenarioService.getGeneratedScenario(configId);
    //         return generatedScenario.map(ResponseEntity::ok)
    //                 .orElse(ResponseEntity.notFound().build());
    //     } catch (Exception e) {
    //         e.printStackTrace(); // Log error for debugging
    //         return ResponseEntity.status(500).body(null);
    //     }
    // }

    // Generate a new marking schema
    @GetMapping("/{configId}/marking-schema/generate")
    public ResponseEntity<MarkingSchema> generateMarkingSchema(@PathVariable("configId") int configId,
                                                               @RequestParam("title") String title) {
        try {
            MarkingSchema markingSchema = scenarioService.generateMarkingSchema(configId, title);
            return ResponseEntity.ok(markingSchema);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Save a marking schema
    @PostMapping("/{configId}/marking-schema")
    public ResponseEntity<String> saveMarkingSchema(@PathVariable("configId") int configId, @RequestBody MarkingSchema markingSchema) {
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
    public ResponseEntity<List<MarkingSchema>> getMarkingSchemas(@PathVariable("configId") int configId) {
        try {
            return ResponseEntity.ok(scenarioService.getMarkingSchemas(configId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // Delete a marking schema
    @DeleteMapping("/{configId}/marking-schema/{schemaId}")
    public ResponseEntity<String> deleteMarkingSchema(@PathVariable("configId") int configId, @PathVariable int schemaId) {
        boolean deleted = scenarioService.deleteMarkingSchema(configId, schemaId);
        return deleted ? ResponseEntity.ok("Marking schema deleted.") : ResponseEntity.notFound().build();
    }
}