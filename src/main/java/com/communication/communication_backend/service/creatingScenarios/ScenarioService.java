package com.communication.communication_backend.service.creatingScenarios;

import org.springframework.stereotype.Service;
import com.communication.communication_backend.dtos.*;
import com.communication.communication_backend.entity.GeneratedScenario;
import com.communication.communication_backend.entity.MarkingSchema;
import com.communication.communication_backend.entity.Scenario;
import com.communication.communication_backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import java.util.*;

@Service
public class ScenarioService {

    private final ScenariosOpenAiClient openAiClient;
    private final RubricsOpenAiClient rubricsOpenAiClient;
    private final ScenarioRepository scenarioRepository;
    private final GeneratedScenarioRepository generatedScenarioRepository;
    private final MarkingSchemaRepository markingSchemaRepository;

    public ScenarioService(
            ScenariosOpenAiClient openAiClient,
            RubricsOpenAiClient rubricsOpenAiClient,
            ScenarioRepository scenarioRepository,
            GeneratedScenarioRepository generatedScenarioRepository,
            MarkingSchemaRepository markingSchemaRepository) {
        this.openAiClient = openAiClient;
        this.rubricsOpenAiClient = rubricsOpenAiClient;
        this.scenarioRepository = scenarioRepository;
        this.generatedScenarioRepository = generatedScenarioRepository;
        this.markingSchemaRepository = markingSchemaRepository;
    }

    // Initialize a new scenario with an auto-generated configId
    public int initializeConfigId() {
        Scenario scenario = new Scenario();
        scenario.setTitle("");
        scenario.setShortDescription("");
        scenario.setPrompt("");
        scenario.setUserId(""); // Initialize userId as empty
        scenario = scenarioRepository.save(scenario);
        return scenario.getConfigId();
    }

    // Save scenario details (title, shortDescription, prompt, userId)
    public void saveScenario(int configId, Scenario scenario) {
        Scenario existingScenario = scenarioRepository.findById(configId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));

        existingScenario.setTitle(scenario.getTitle());
        existingScenario.setShortDescription(scenario.getShortDescription());
        existingScenario.setPrompt(scenario.getPrompt());
        existingScenario.setUserId(scenario.getUserId()); // Assign the userId

        scenarioRepository.save(existingScenario);
    }

    // Retrieve a scenario by configId
    public Optional<Scenario> getScenarioById(int configId) {
        return scenarioRepository.findById(configId);
    }

    // Get all scenario summaries (configId, title, userId)
    public List<ScenarioSummary> getAllScenarioSummaries() {
        List<Scenario> scenarios = scenarioRepository.findAll();
        List<ScenarioSummary> summaries = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            summaries.add(new ScenarioSummary(scenario.getConfigId(), scenario.getTitle(), scenario.getUserId()));
        }
        return summaries;
    }

    // Delete scenario (including associated generated scenarios and marking schemas)
    @Transactional
    public boolean deleteScenario(int configId) {
        Optional<Scenario> scenarioOpt = scenarioRepository.findById(configId);
        if (scenarioOpt.isEmpty()) return false;

        Scenario scenario = scenarioOpt.get();
        generatedScenarioRepository.deleteAll(generatedScenarioRepository.findByScenario(scenario));
        markingSchemaRepository.deleteAll(markingSchemaRepository.findByScenario(scenario));
        scenarioRepository.delete(scenario);
        return true;
    }

    // Generate scenario using OpenAI
    public GeneratedScenario generateScenario(int configId) throws Exception {
        Scenario scenario = scenarioRepository.findById(configId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));

        // Prepare input for OpenAI request
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", "You are assisting in generating a mock patient scenario."),
            Map.of("role", "user", "content", String.format("Title: %s\nShort Description: %s\nPrompt: %s",
                    scenario.getTitle(), scenario.getShortDescription(), scenario.getPrompt()))
        );

        JsonNode generatedResponse = openAiClient.getGeneratedResponse(messages);

        // Extract relevant fields
        JsonNode generatedScenarioNode = generatedResponse.get("generatedScenario").get(0);

        GeneratedScenario generatedScenario = new GeneratedScenario(
            0, // Auto-generated ID
            scenario,
            generatedScenarioNode.get("taskInstructions").asText(),
            generatedScenarioNode.get("backgroundInformation").asText(),
            generatedScenarioNode.get("personalityTraits").asText(),
            generatedScenarioNode.get("questionsForDoctor").asText(),
            generatedScenarioNode.get("responseGuidelines").asText(),
            generatedScenarioNode.get("sampleResponses").asText()
        );

        return generatedScenarioRepository.save(generatedScenario);
    }

    // Save generated scenario to database
    public void saveGeneratedScenario(int configId, GeneratedScenario generatedScenario) {
        Scenario scenario = scenarioRepository.findById(configId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));
        generatedScenario.setScenario(scenario);
        generatedScenarioRepository.save(generatedScenario);
    }

    // Retrieve saved generated scenario
    public Optional<GeneratedScenario> getGeneratedScenario(int configId) {
        return generatedScenarioRepository.findByScenario(
                scenarioRepository.findById(configId).orElse(null)).stream().findFirst();
    }

    // Generate a rubric schema
    public MarkingSchema generateMarkingSchema(int configId, String schemaTitle) throws Exception {
        Scenario scenario = scenarioRepository.findById(configId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));

        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", "You are assisting in generating a marking rubric schema."),
            Map.of("role", "user", "content", String.format(
                    "Scenario Title: %s\nShort Description: %s\nPrompt: %s\nTask Instructions: %s\nRubric Title: %s",
                    scenario.getTitle(), scenario.getShortDescription(), scenario.getPrompt(),
                    getGeneratedScenario(configId).orElseThrow().getTaskInstruction(), schemaTitle))
        );

        JsonNode generatedResponse = rubricsOpenAiClient.getGeneratedRubrics(messages);
        JsonNode rubricNode = generatedResponse.get("generatedRubrics").get(0);

        MarkingSchema markingSchema = new MarkingSchema(
            0, // Auto-generated ID
            scenario,
            rubricNode.get("title").asText(),
            rubricNode.get("unsatisfactory").asText(),
            rubricNode.get("borderline").asText(),
            rubricNode.get("satisfactory").asText()
        );

        return markingSchemaRepository.save(markingSchema);
    }

    // Save a rubric schema
    public void saveMarkingSchema(int configId, MarkingSchema markingSchema) {
        Scenario scenario = scenarioRepository.findById(configId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));
        markingSchema.setScenario(scenario);
        markingSchemaRepository.save(markingSchema);
    }

    // Retrieve all rubric schemas for a scenario
    public List<MarkingSchema> getMarkingSchemas(int configId) {
        return markingSchemaRepository.findByScenario(
                scenarioRepository.findById(configId).orElse(null));
    }

    // Delete a rubric schema
    public boolean deleteMarkingSchema(int configId, int schemaId) {
        Optional<MarkingSchema> markingSchemaOpt = markingSchemaRepository.findById(schemaId);
        if (markingSchemaOpt.isEmpty()) return false;

        markingSchemaRepository.delete(markingSchemaOpt.get());
        return true;
    }
}