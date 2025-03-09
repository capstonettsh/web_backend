package com.communication.communication_backend.service.creatingScenarios;

import com.communication.communication_backend.entity.MarkingSchema;
import com.communication.communication_backend.entity.Scenario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.communication.communication_backend.dtos.*;
import com.communication.communication_backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class ScenarioService {

    private final ScenariosOpenAiClient openAiClient;
    private final RubricsOpenAiClient rubricsOpenAiClient;
    private final ScenarioRepository scenarioRepository;
    private final MarkingSchemaRepository markingSchemaRepository;

    @Value("${elevenlabs.api.key}")
    private String elevenlabsApiKey;
    private static final String CREATE_AGENT_URL = "https://api.elevenlabs.io/v1/convai/agents/create";
    private static final String UPDATE_AGENT_URL = "https://api.elevenlabs.io/v1/convai/agents/";

    public ScenarioService(
            ScenariosOpenAiClient openAiClient,
            RubricsOpenAiClient rubricsOpenAiClient,
            ScenarioRepository scenarioRepository,
            MarkingSchemaRepository markingSchemaRepository) {
        this.openAiClient = openAiClient;
        this.rubricsOpenAiClient = rubricsOpenAiClient;
        this.scenarioRepository = scenarioRepository;
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

        if (existingScenario.getAgentId() != null && !existingScenario.getAgentId().isEmpty()) {
            // Agent exists → update it via PATCH
            updateAgent(existingScenario.getAgentId(), scenario);
        } else {
            // No agent exists → create a new one and save its agentId
            String newAgentId = createAgent(scenario);
            existingScenario.setAgentId(newAgentId);
        }

        scenarioRepository.save(existingScenario);
    }

    private String createAgent(Scenario scenario) {
        // Build the conversation config payload to match the curl command.
        Map<String, Object> conversationConfig = new HashMap<>();

        // Set up the "agent" configuration with a prompt.
        Map<String, Object> agentConfig = new HashMap<>();
        Map<String, Object> promptConfig = new HashMap<>();
        promptConfig.put("prompt", scenario.getTitle() + scenario.getShortDescription());
        agentConfig.put("prompt", promptConfig);
        conversationConfig.put("agent", agentConfig);

        // Set up the TTS configuration.
        Map<String, Object> ttsConfig = new HashMap<>();
        ttsConfig.put("voice_id", "aSXZu6bgEOS8MXVRzjPi");
        conversationConfig.put("tts", ttsConfig);

        // Create the overall request payload.
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("conversation_config", conversationConfig);

        // Serialize the request payload to JSON.
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestBodyMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert request body to JSON", e);
        }

        // Build the HttpRequest.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.elevenlabs.io/v1/convai/agents/create"))
                .header("xi-api-key", elevenlabsApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send the request using HttpClient.
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                // Assuming the response JSON contains {"agent_id": "new_agent_id"}
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                if (responseMap != null && responseMap.containsKey("agent_id")) {
                    return (String) responseMap.get("agent_id");
                } else {
                    throw new RuntimeException("agent_id not found in response");
                }
            } else {
                throw new RuntimeException("Failed to create agent. HTTP status code: " + statusCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request failed", e);
        }
    }

    private void updateAgent(String agentId, Scenario scenario) {
        String url = UPDATE_AGENT_URL + agentId;

        // Build an updated conversation config payload.
        Map<String, Object> conversationConfig = new HashMap<>();
        Map<String, Object> agentConfig = new HashMap<>();
        Map<String, Object> promptConfig = new HashMap<>();
        promptConfig.put("prompt", scenario.getTitle() + scenario.getShortDescription() +
                scenario.getBackgroundInformation() + scenario.getPersonality() + scenario.getQuestionsForDoctor() +
                scenario.getResponseGuidelines() + scenario.getSampleResponses() + "Generate the sentence with a lot " +
                "of punctuation marks to represent the emotion.");
        agentConfig.put("prompt", promptConfig);
        conversationConfig.put("agent", agentConfig);
//        Map<String, Object> ttsConfig = new HashMap<>();
        // For simplicity, we’re reusing the same voice id.
//        ttsConfig.put("voice_id", DEFAULT_VOICE_ID);
//        conversationConfig.put("tts", ttsConfig);
        // You can update other configurations based on scenario details if needed.

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("conversation_config", conversationConfig);

        // Convert the request body map to a JSON string using Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestBodyMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert request body to JSON", e);
        }

        // Build the HttpRequest using PATCH method.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("xi-api-key", elevenlabsApiKey)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Create an HttpClient instance and send the request.
        HttpClient client = HttpClient.newHttpClient();
        try {
            // We can use BodyHandlers.discarding() since no response body is expected.
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("Failed to update agent on ElevenLabs. HTTP status code: " + statusCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request failed", e);
        }
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

        if (scenario.getAgentId() != null && !scenario.getAgentId().isEmpty()) {
            // Agent exists → update it via PATCH
            updateAgent(scenario.getAgentId(), scenario);
        } else {
            // No agent exists → create a new one and save its agentId
            String newAgentId = createAgent(scenario);
            scenario.setAgentId(newAgentId);
        }
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
    public boolean deleteScenario(int scenarioId) {
        Optional<Scenario> scenarioOpt = scenarioRepository.findById(scenarioId);
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

    // Generate a rubric schema
    public MarkingSchema generateMarkingSchema(int configId, String schemaTitle) throws Exception {
        Scenario scenario = scenarioRepository.findById(configId)
                .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", "You are assisting in generating a marking rubric schema."),
                Map.of("role", "user", "content", String.format(
                        "Scenario Title: %s\nShort Description: %s\nPrompt: %s\nTask Instructions: %s\nRubric Title: %s",
                        scenario.getTitle(), scenario.getShortDescription(), scenario.getPrompt(),
                        scenario.getTaskInstruction() + scenario.getBackgroundInformation() + scenario.getPersonality()
                        , schemaTitle))
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
    public List<MarkingSchema> getMarkingSchemas(int scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElse(null);
        return markingSchemaRepository.findByScenario(scenario);
    }

    // Delete a rubric schema
    public boolean deleteMarkingSchema(int configId, int schemaId) {
        Optional<MarkingSchema> markingSchemaOpt = markingSchemaRepository.findById(schemaId);
        if (markingSchemaOpt.isEmpty()) return false;

        markingSchemaRepository.delete(markingSchemaOpt.get());
        return true;
    }
}