package com.communication.communication_backend.service.creatingScenarios;

import org.springframework.stereotype.Service;

import com.communication.communication_backend.dtos.GeneratedScenarioDto;
import com.communication.communication_backend.dtos.MarkingSchemaDto;
import com.communication.communication_backend.dtos.Scenario;
import com.communication.communication_backend.dtos.ScenarioSummary;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

@Service
public class ScenarioService {

    private final ScenariosOpenAiClient openAiClient;
    private final RubricsOpenAiClient rubricsOpenAiClient;

    // In-memory storage for simplicity; replace with SQL database later
    private final Map<Integer, Scenario> scenarios = new HashMap<>();
    private final Map<Integer, GeneratedScenarioDto> generatedScenarios = new HashMap<>();
    private final Map<Integer, List<MarkingSchemaDto>> markingSchemas = new HashMap<>(); // Storing marking schemas

    public ScenarioService(ScenariosOpenAiClient openAiClient, RubricsOpenAiClient rubricsOpenAiClient) {
        this.openAiClient = openAiClient;
        this.rubricsOpenAiClient = rubricsOpenAiClient;
    }

    // Initialize a new configId
    public int initializeConfigId() {
        int newConfigId = new Random().nextInt(10000);  // Simulated ID generation
        scenarios.put(newConfigId, new Scenario(newConfigId, "", "", ""));
        return newConfigId;
    }

    // Save scenario prompt (title, shortDescription, prompt)
    public void saveScenario(int configId, Scenario scenario) {
        scenarios.put(configId, scenario);
    }

    // Retrieve scenario prompt by configId
    public Optional<Scenario> getScenarioById(int configId) {
        return Optional.ofNullable(scenarios.get(configId));
    }

    // Save scenario summary (basic info for overview)
    public List<ScenarioSummary> getAllScenarioSummaries() {
        List<ScenarioSummary> summaries = new ArrayList<>();
        for (Scenario scenario : scenarios.values()) {
            summaries.add(new ScenarioSummary(scenario.getConfigId(), scenario.getTitle()));
        }
        return summaries;
    }

    // Delete a scenario by configId
    public boolean deleteScenario(int configId) {
        return scenarios.remove(configId) != null && generatedScenarios.remove(configId) != null;
    }

    // Generate scenario using OpenAI
    public GeneratedScenarioDto generateScenario(int configId) throws Exception {
        Scenario scenario = scenarios.get(configId);
        if (scenario == null) {
            throw new NoSuchElementException("Scenario with configId " + configId + " not found.");
        }

        // Prepare input for getGeneratedResponse
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", "You are assisting in generating a mock patient scenario."),
            Map.of("role", "user", "content", String.format("Title: %s\nShort Description: %s\nPrompt: %s", 
                    scenario.getTitle(), scenario.getShortDescription(), scenario.getPrompt()))
        );

        JsonNode generatedResponse = openAiClient.getGeneratedResponse(messages);

        // Extracting relevant fields
        JsonNode generatedScenarioNode = generatedResponse.get("generatedScenario").get(0);  // Assuming only one scenario is returned

        GeneratedScenarioDto generatedScenario = new GeneratedScenarioDto(
            generatedScenarioNode.get("taskInstructions").asText(),
            generatedScenarioNode.get("backgroundInfo").asText(),
            generatedScenarioNode.get("personalityTraits").asText(),
            generatedScenarioNode.get("questionsForDoctor").asText(),
            generatedScenarioNode.get("responseGuidelines").asText(),
            generatedScenarioNode.get("sampleResponses").asText()
        );

        // Save generated scenario
        generatedScenarios.put(configId, generatedScenario);
        return generatedScenario;
    }

    // Save the generated scenario (to be expanded later for DB integration)
    public void saveGeneratedScenario(int configId, GeneratedScenarioDto generatedScenario) {
        generatedScenarios.put(configId, generatedScenario);
    }

    // Retrieve the saved generated scenario
    public Optional<GeneratedScenarioDto> getGeneratedScenario(int configId) {
        return Optional.ofNullable(generatedScenarios.get(configId));
    }

    // Generate rubric schema
    public MarkingSchemaDto generateMarkingSchema(int configId, String schemaTitle) throws Exception {
        Scenario scenario = scenarios.get(configId);
        if (scenario == null) {
            throw new NoSuchElementException("Scenario with configId " + configId + " not found.");
        }

        // Prepare input for getGeneratedRubrics
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", "You are assisting in generating a marking rubric schema."),
            Map.of("role", "user", "content", String.format("Scenario Title: %s\nShort Description: %s\nPrompt: %s\nTask Instructions: %s\nRubric Title: %s",
                    scenario.getTitle(), scenario.getShortDescription(), scenario.getPrompt(),
                    generatedScenarios.get(configId).getTaskInstruction(), schemaTitle))
        );

        JsonNode generatedResponse = rubricsOpenAiClient.getGeneratedRubrics(messages);
        JsonNode rubricNode = generatedResponse.get("generatedRubrics").get(0); 

        // Assign a schemaId
        int schemaId = markingSchemas.getOrDefault(configId, new ArrayList<>()).size() + 1;

        MarkingSchemaDto rubricSchema = new MarkingSchemaDto(
            schemaId,
            rubricNode.get("title").asText(),
            rubricNode.get("unsatisfactory").asText(),
            rubricNode.get("borderline").asText(),
            rubricNode.get("satisfactory").asText()
        );

        markingSchemas.computeIfAbsent(configId, k -> new ArrayList<>()).add(rubricSchema);
        return rubricSchema;
    }

    // Save a marking schema
    public void saveMarkingSchema(int configId, MarkingSchemaDto markingSchema) {
        markingSchemas.computeIfAbsent(configId, k -> new ArrayList<>()).add(markingSchema);
    }

    // Retrieve all marking schemas for a scenario
    public List<MarkingSchemaDto> getMarkingSchemas(int configId) {
        return markingSchemas.getOrDefault(configId, Collections.emptyList());
    }

    // Delete a specific marking schema
    public boolean deleteMarkingSchema(int configId, int schemaId) {
        return markingSchemas.getOrDefault(configId, new ArrayList<>())
                .removeIf(schema -> schema.getSchemaId() == schemaId);
    }
}