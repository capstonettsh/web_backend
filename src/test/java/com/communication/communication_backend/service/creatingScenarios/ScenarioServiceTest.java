package com.communication.communication_backend.service.creatingScenarios;

import com.communication.communication_backend.entity.Scenario;
import com.communication.communication_backend.repository.MarkingSchemaRepository;
import com.communication.communication_backend.repository.ScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceTest {

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private ScenariosOpenAiClient openAiClient;

    @Mock
    private RubricsOpenAiClient rubricsOpenAiClient;

    @Mock
    private MarkingSchemaRepository markingSchemaRepository;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @InjectMocks
    private ScenarioService scenarioService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() throws Exception {
        // Inject humeaiApiKey using reflection
        setPrivateField(scenarioService, "humeaiApiKey", "test-humeai-key");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close(); // Clean up static mock only if initialized
        }
    }

    @Test
    void testInitializeScenarioId() {
        // Arrange
        Scenario savedScenario = new Scenario();
        savedScenario.setScenarioId(1);
        when(scenarioRepository.save(any(Scenario.class))).thenReturn(savedScenario);

        // Act
        int scenarioId = scenarioService.initializeScenarioId();

        // Assert
        assertNotNull(scenarioId, "Scenario ID should not be null");
        assertEquals(1, scenarioId);
        verify(scenarioRepository, times(1)).save(any(Scenario.class));
    }

    @Test
    void testSaveBasicScenario() throws Exception {
        // Arrange HTTP mocking
        closeable = mockStatic(HttpClient.class);
        when(HttpClient.newHttpClient()).thenReturn(mockHttpClient);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body())
                .thenReturn("{\"id\": \"prompt-123\"}") // First call: createPrompt
                .thenReturn("{\"id\": \"config-123\"}"); // Second call: createHumeConfig

        // Arrange scenario data
        int scenarioId = 1;
        Scenario existingScenario = new Scenario();
        existingScenario.setScenarioId(scenarioId);
        existingScenario.setTitle("Old Title");
        when(scenarioRepository.findById(scenarioId)).thenReturn(Optional.of(existingScenario));
        when(scenarioRepository.save(any(Scenario.class))).thenReturn(existingScenario);

        Scenario newScenarioData = new Scenario();
        newScenarioData.setTitle("New Title");

        // Act
        scenarioService.saveBasicScenario(scenarioId, newScenarioData);

        // Assert
        verify(scenarioRepository, times(1)).findById(scenarioId);
        verify(scenarioRepository, times(1)).save(any(Scenario.class));
        assertEquals("New Title", existingScenario.getTitle(), "Scenario title should be updated");
        assertEquals("config-123", existingScenario.getAgentId(), "Agent ID should be set");
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    // Utility to set private instance fields via reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}