package com.communication.communication_backend.service.creatingScenarios;

import com.communication.communication_backend.entity.GeneratedScenario;
import com.communication.communication_backend.entity.MarkingSchema;
import com.communication.communication_backend.entity.Scenario;
import org.springframework.stereotype.Service;
import com.communication.communication_backend.dtos.*;
import com.communication.communication_backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import java.util.*;

@Service
public class ScenarioService {

    private final ScenariosOpenAiClient openAiClient;
    private final RubricsOpenAiClient rubricsOpenAiClient;
    private final ScenarioRepository scenarioRepository;
    // private final GeneratedScenarioRepository generatedScenarioRepository;
    private final MarkingSchemaRepository markingSchemaRepository;

    public ScenarioService(
            ScenariosOpenAiClient openAiClient,
            RubricsOpenAiClient rubricsOpenAiClient,
            ScenarioRepository scenarioRepository,
            // GeneratedScenarioRepository generatedScenarioRepository,
            MarkingSchemaRepository markingSchemaRepository) {
        this.openAiClient = openAiClient;
        this.rubricsOpenAiClient = rubricsOpenAiClient;
        this.scenarioRepository = scenarioRepository;
        // this.generatedScenarioRepository = generatedScenarioRepository;
        this.markingSchemaRepository = markingSchemaRepository;
    }

    // Create a new Scenario with empty/default values and return its configId
    public int initializeScenarioId() {
        Scenario scenario = new Scenario();
        scenario.setTitle("");
        scenario.setShortDescription("");
        scenario.setPrompt("");
        scenario.setTaskInstruction("");
        scenario.setBackgroundInformation("");
        scenario.setPersonality("");
        scenario.setQuestionsForDoctor("");
        scenario.setResponseGuidelines("");
        scenario.setSampleResponses("");
        scenario.setUserId("");
        scenario = scenarioRepository.save(scenario);
        return scenario.getScenarioId();
    }

    // Save scenario details (title, shortDescription, prompt, userId)
    public void saveBasicScenario(int scenarioId, Scenario scenario) {
        Scenario existingScenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with scenarioId " + scenarioId + " not found" +
                        "."));

        existingScenario.setTitle(scenario.getTitle());
        existingScenario.setShortDescription(scenario.getShortDescription());
        existingScenario.setPrompt(scenario.getPrompt());
        existingScenario.setUserId(scenario.getUserId()); // Assign the userId

        scenarioRepository.save(existingScenario);
    }

    public void saveScenarioDetails(int scenarioId, Scenario scenarioData) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + scenarioId + " not found."));
        scenario.setTaskInstruction(scenarioData.getTaskInstruction());
        scenario.setBackgroundInformation(scenarioData.getBackgroundInformation());
        scenario.setPersonality(scenarioData.getPersonality());
        scenario.setQuestionsForDoctor(scenarioData.getQuestionsForDoctor());
        scenario.setResponseGuidelines(scenarioData.getResponseGuidelines());
        scenario.setSampleResponses(scenarioData.getSampleResponses());
        scenarioRepository.save(scenario);
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
            summaries.add(new ScenarioSummary(scenario.getScenarioId(), scenario.getTitle(), scenario.getUserId(), scenario.getShortDescription()));
        }
        return summaries;
    }

    // Delete scenario (including associated generated scenarios and marking schemas)
    @Transactional
    public boolean deleteScenario(int configId) {
        Optional<Scenario> scenarioOpt = scenarioRepository.findById(configId);
        if (scenarioOpt.isEmpty()) return false;

        Scenario scenario = scenarioOpt.get();
        markingSchemaRepository.deleteAll(markingSchemaRepository.findByScenario(scenario));
        scenarioRepository.delete(scenario);
        return true;
    }

    // Generate scenario using OpenAI
    public Scenario generateScenario(int configId) throws Exception {
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

        scenario.setTaskInstruction(generatedScenarioNode.get("taskInstructions").asText());
        scenario.setBackgroundInformation(generatedScenarioNode.get("backgroundInfo").asText());
        scenario.setPersonality(generatedScenarioNode.get("personalityTraits").asText());
        scenario.setQuestionsForDoctor(generatedScenarioNode.get("questionsForDoctor").asText());
        scenario.setResponseGuidelines(generatedScenarioNode.get("responseGuidelines").asText());
        scenario.setSampleResponses(generatedScenarioNode.get("sampleResponses").asText());

        return scenarioRepository.save(scenario);
    }

    // Save generated scenario to database
    // public void saveGeneratedScenario(int configId, GeneratedScenario generatedScenario) {
    //     Scenario scenario = scenarioRepository.findById(configId)
    //             .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));
    //     generatedScenario.setScenario(scenario);
    //     generatedScenarioRepository.save(generatedScenario);
    // }

    // Retrieve saved generated scenario
    // public Optional<GeneratedScenario> getGeneratedScenario(int configId) {
    //     Scenario scenario = scenarioRepository.findById(configId).orElse(null);
    //     if (scenario == null) {
    //         return Optional.empty();
    //     }
    //     return generatedScenarioRepository.findFirstByScenarioOrderByIdDesc(scenario);
    // }

    // Generate a rubric schema
    public MarkingSchema generateMarkingSchema(int configId, String schemaTitle) throws Exception {
        Scenario scenario = scenarioRepository.findById(configId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", "You are assisting in generating a marking rubric schema."),
                Map.of("role", "user", "content", String.format(
                        "Scenario Title: %s\nShort Description: %s\nPrompt: %s\nTask Instructions: %s\nRubric Title: %s",
                        scenario.getTitle(), scenario.getShortDescription(), scenario.getPrompt(),
                        scenario.getTaskInstruction() + scenario.getBackgroundInformation() + scenario.getPersonality(), schemaTitle))
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