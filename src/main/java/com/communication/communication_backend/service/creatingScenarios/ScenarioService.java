package com.communication.communication_backend.service.creatingScenarios;

import com.communication.communication_backend.dtos.ScenarioSummary;
import com.communication.communication_backend.entity.MarkingSchema;
import com.communication.communication_backend.entity.Scenario;
import com.communication.communication_backend.repository.MarkingSchemaRepository;
import com.communication.communication_backend.repository.ScenarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class ScenarioService {

  private static final String CREATE_CONFIG_URL = "https://api.hume.ai/v0/evi/configs";
  private static final String CREATE_PROMPT_URL = "https://api.hume.ai/v0/evi/prompts";
  private final ScenariosOpenAiClient openAiClient;
  private final RubricsOpenAiClient rubricsOpenAiClient;
  private final ScenarioRepository scenarioRepository;
  // private final GeneratedScenarioRepository generatedScenarioRepository;
  private final MarkingSchemaRepository markingSchemaRepository;
  @Value("${humeai.api.key}")
  private String humeaiApiKey;


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
        .orElseThrow(() -> new NoSuchElementException("Scenario with scenarioId " + scenarioId + " not found."));

    existingScenario.setTitle(scenario.getTitle());
    existingScenario.setShortDescription(scenario.getShortDescription());
    existingScenario.setPrompt(scenario.getPrompt());
    existingScenario.setUserId(scenario.getUserId());

    String newConfigId = createHumeConfig(existingScenario);
    existingScenario.setAgentId(newConfigId);

    scenarioRepository.save(existingScenario);
  }

  // Create a prompt on Hume AI and return its id
  private String createPrompt(Scenario scenario) {
    // Build the prompt payload.
    Map<String, Object> payload = new HashMap<>();
    payload.put("name", scenario.getTitle() + UUID.randomUUID().toString());
    payload.put("text",
        "You are the patient!" + "You are the patient!" + scenario.getBackgroundInformation() + scenario.getPersonality() + scenario.getQuestionsForDoctor() + scenario.getResponseGuidelines() + scenario.getSampleResponses() + ".Try to generate short sentence for the response, disclose information step by step based on user response, use a lot of annotation to express emotion." + "You are the patient!");

    ObjectMapper objectMapper = new ObjectMapper();
    String requestBody;
    try {
      requestBody = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize prompt payload to JSON", e);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(CREATE_PROMPT_URL))
        .header("X-Hume-Api-Key", humeaiApiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();

    HttpClient client = HttpClient.newHttpClient();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      int statusCode = response.statusCode();
      if (statusCode >= 200 && statusCode < 300) {
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        if (responseMap != null && responseMap.containsKey("id")) {
          return (String) responseMap.get("id");
        } else {
          throw new RuntimeException("Prompt id not found in response");
        }
      } else {
        throw new RuntimeException("Failed to create prompt. HTTP status code: " + statusCode);
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("HTTP request to create prompt failed", e);
    }
  }

  // Create a new Hume AI config using the created prompt id
  private String createHumeConfig(Scenario scenario) {
    // Create a prompt first and get its id
    String promptId = createPrompt(scenario);

    Map<String, Object> payload = new HashMap<>();
    payload.put("evi_version", "2");
    // Use scenario title as the config name (or customize as needed)
    payload.put("name", scenario.getTitle() + UUID.randomUUID().toString());

    // Use the prompt id returned from createPrompt()
    Map<String, Object> prompt = new HashMap<>();
    prompt.put("id", promptId);
    prompt.put("version", 0);
    payload.put("prompt", prompt);

    // Voice configuration using a custom voice
    Map<String, Object> voice = new HashMap<>();
    voice.put("provider", "CUSTOM_VOICE");
    Map<String, Object> customVoice = new HashMap<>();
    customVoice.put("name", "LIM WEI JIE 3");
    customVoice.put("base_voice", "SUNNY");
    customVoice.put("description", "More Masculine");
    customVoice.put("parameter_model", "20241004-11parameter");
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("assertiveness", -13);
    parameters.put("buoyancy", 8);
    parameters.put("confidence", -9);
    parameters.put("enthusiasm", -9);
    parameters.put("nasality", -8);
    parameters.put("relaxedness", -9);
    parameters.put("smoothness", -8);
    parameters.put("tepidity", 9);
    parameters.put("tightness", 7);
    customVoice.put("parameters", parameters);
    voice.put("custom_voice", customVoice);
    payload.put("voice", voice);

    // Language model configuration using Open AI's GPT-4o
    Map<String, Object> languageModel = new HashMap<>();
    languageModel.put("model_provider", "OPEN_AI");
    languageModel.put("model_resource", "gpt-4o");
    languageModel.put("temperature", 1);
    payload.put("language_model", languageModel);

    // Timeouts configuration (25 minutes = 1500 seconds for max duration)
    Map<String, Object> timeouts = new HashMap<>();
    Map<String, Object> inactivity = new HashMap<>();
    inactivity.put("enabled", true);
    inactivity.put("duration_secs", 600);
    timeouts.put("inactivity", inactivity);
    Map<String, Object> maxDuration = new HashMap<>();
    maxDuration.put("enabled", true);
    maxDuration.put("duration_secs", 1500);
    timeouts.put("max_duration", maxDuration);
    payload.put("timeouts", timeouts);

    ObjectMapper objectMapper = new ObjectMapper();
    String requestBody;
    try {
      requestBody = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to convert config payload to JSON", e);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(CREATE_CONFIG_URL))
        .header("X-Hume-Api-Key", humeaiApiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();

    HttpClient client = HttpClient.newHttpClient();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      int statusCode = response.statusCode();
      if (statusCode >= 200 && statusCode < 300) {
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        if (responseMap != null && responseMap.containsKey("id")) {
          return (String) responseMap.get("id");
        } else {
          throw new RuntimeException("Config id not found in response");
        }
      } else {
        throw new RuntimeException("Failed to create Hume config. HTTP status code: " + statusCode);
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("HTTP request to create Hume config failed", e);
    }
  }

  // Save additional scenario details (taskInstruction, backgroundInformation, personality, etc.)
  public void saveScenarioDetails(int scenarioId, Scenario scenarioData) {
    Scenario scenario = scenarioRepository.findById(scenarioId)
        .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + scenarioId + " not found."));
    scenario.setTaskInstruction(scenarioData.getTaskInstruction());
    scenario.setBackgroundInformation(scenarioData.getBackgroundInformation());
    scenario.setPersonality(scenarioData.getPersonality());
    scenario.setQuestionsForDoctor(scenarioData.getQuestionsForDoctor());
    scenario.setResponseGuidelines(scenarioData.getResponseGuidelines());
    scenario.setSampleResponses(scenarioData.getSampleResponses());

    // Always create a new config when scenario details are updated.
    String newConfigId = createHumeConfig(scenario);
    scenario.setAgentId(newConfigId);

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
  public void saveMarkingSchema(int configId, List<MarkingSchema> markingSchemas) {
    // Retrieve the Scenario by configId
    Scenario scenario = scenarioRepository.findById(configId)
        .orElseThrow(() -> new NoSuchElementException("Scenario with configId " + configId + " not found."));

    // Delete all existing marking schemas for this scenario
    List<MarkingSchema> existingSchemas = markingSchemaRepository.findByScenario(scenario);
    if (!existingSchemas.isEmpty()) {
      markingSchemaRepository.deleteAll(existingSchemas);
    }

    // Set the scenario for each new marking schema and save them all
    markingSchemas.forEach(schema -> schema.setScenario(scenario));
    markingSchemaRepository.saveAll(markingSchemas);
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