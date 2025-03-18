package com.communication.communication_backend.controller;

import com.communication.communication_backend.dtos.ScenarioSummary;
import com.communication.communication_backend.entity.MarkingSchema;
import com.communication.communication_backend.entity.Scenario;
import com.communication.communication_backend.service.creatingScenarios.ScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioControllerTest {

    @Mock
    private ScenarioService scenarioService;

    @InjectMocks
    private ScenarioController scenarioController;

    @BeforeEach
    void setUp() {
        // No additional setup needed; @InjectMocks initializes the controller with the mocked service
    }

    @Test
    void testInitializeScenario_Success() {
        // Arrange
        when(scenarioService.initializeScenarioId()).thenReturn(1);

        // Act
        ResponseEntity<Map<String, Integer>> response = scenarioController.initializeScenario();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.singletonMap("configId", 1), response.getBody());
        verify(scenarioService, times(1)).initializeScenarioId();
    }

    @Test
    void testSaveBasicScenario_Success() throws Exception {
        // Arrange
        int configId = 1;
        Scenario scenario = new Scenario();
        doNothing().when(scenarioService).saveBasicScenario(eq(configId), any(Scenario.class));

        // Act
        ResponseEntity<Map<String, String>> response = scenarioController.saveBasicScenario(configId, scenario);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.singletonMap("message", "success"), response.getBody());
        verify(scenarioService, times(1)).saveBasicScenario(eq(configId), eq(scenario));
    }

    @Test
    void testSaveScenarioDetails_Success() throws Exception {
        // Arrange
        int configId = 1;
        Scenario scenario = new Scenario();
        doNothing().when(scenarioService).saveScenarioDetails(eq(configId), any(Scenario.class));

        // Act
        ResponseEntity<Map<String, String>> response = scenarioController.saveScenarioDetails(configId, scenario);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.singletonMap("message", "Scenario additional details saved successfully."), response.getBody());
        verify(scenarioService, times(1)).saveScenarioDetails(eq(configId), eq(scenario));
    }

    @Test
    void testGetScenario_Found() {
        // Arrange
        int configId = 1;
        Scenario scenario = new Scenario();
        when(scenarioService.getScenarioById(configId)).thenReturn(Optional.of(scenario));

        // Act
        ResponseEntity<Scenario> response = scenarioController.getScenario(configId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(scenario, response.getBody());
        verify(scenarioService, times(1)).getScenarioById(configId);
    }

    @Test
    void testGetScenario_NotFound() {
        // Arrange
        int configId = 1;
        when(scenarioService.getScenarioById(configId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Scenario> response = scenarioController.getScenario(configId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(scenarioService, times(1)).getScenarioById(configId);
    }

    @Test
    void testGetAllScenarioSummaries_Success() {
        // Arrange
        List<ScenarioSummary> summaries = Collections.singletonList(new ScenarioSummary());
        when(scenarioService.getAllScenarioSummaries()).thenReturn(summaries);

        // Act
        ResponseEntity<List<ScenarioSummary>> response = scenarioController.getAllScenarioSummaries();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(summaries, response.getBody());
        verify(scenarioService, times(1)).getAllScenarioSummaries();
    }

    @Test
    void testDeleteScenario_Success() {
        // Arrange
        int configId = 1;
        when(scenarioService.deleteScenario(configId)).thenReturn(true);

        // Act
        ResponseEntity<String> response = scenarioController.deleteScenario(configId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Scenario deleted successfully.", response.getBody());
        verify(scenarioService, times(1)).deleteScenario(configId);
    }

    @Test
    void testDeleteScenario_NotFound() {
        // Arrange
        int configId = 1;
        when(scenarioService.deleteScenario(configId)).thenReturn(false);

        // Act
        ResponseEntity<String> response = scenarioController.deleteScenario(configId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(scenarioService, times(1)).deleteScenario(configId);
    }

    @Test
    void testGenerateScenario_Success() throws Exception {
        // Arrange
        int configId = 1;
        Scenario scenario = new Scenario();
        when(scenarioService.generateScenario(configId)).thenReturn(scenario);

        // Act
        ResponseEntity<Scenario> response = scenarioController.generateScenario(configId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(scenario, response.getBody());
        verify(scenarioService, times(1)).generateScenario(configId);
    }

    @Test
    void testGenerateScenario_Error() throws Exception {
        // Arrange
        int configId = 1;
        when(scenarioService.generateScenario(configId)).thenThrow(new RuntimeException("AI error"));

        // Act
        ResponseEntity<Scenario> response = scenarioController.generateScenario(configId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(scenarioService, times(1)).generateScenario(configId);
    }

    @Test
    void testGenerateMarkingSchema_Success() throws Exception {
        // Arrange
        int configId = 1;
        String title = "Test Title";
        MarkingSchema markingSchema = new MarkingSchema();
        when(scenarioService.generateMarkingSchema(configId, title)).thenReturn(markingSchema);

        // Act
        ResponseEntity<MarkingSchema> response = scenarioController.generateMarkingSchema(configId, title);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(markingSchema, response.getBody());
        verify(scenarioService, times(1)).generateMarkingSchema(configId, title);
    }

    @Test
    void testGenerateMarkingSchema_Error() throws Exception {
        // Arrange
        int configId = 1;
        String title = "Test Title";
        when(scenarioService.generateMarkingSchema(configId, title)).thenThrow(new RuntimeException("Generation error"));

        // Act
        ResponseEntity<MarkingSchema> response = scenarioController.generateMarkingSchema(configId, title);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(scenarioService, times(1)).generateMarkingSchema(configId, title);
    }

    @Test
    void testSaveMarkingSchema_Success() throws Exception {
        // Arrange
        int configId = 1;
        List<MarkingSchema> markingSchemas = Collections.singletonList(new MarkingSchema());
        doNothing().when(scenarioService).saveMarkingSchema(eq(configId), anyList());

        // Act
        ResponseEntity<String> response = scenarioController.saveMarkingSchema(configId, markingSchemas);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Marking schemas saved successfully.", response.getBody());
        verify(scenarioService, times(1)).saveMarkingSchema(eq(configId), eq(markingSchemas));
    }

    @Test
    void testSaveMarkingSchema_Error() throws Exception {
        // Arrange
        int configId = 1;
        List<MarkingSchema> markingSchemas = Collections.singletonList(new MarkingSchema());
        doThrow(new RuntimeException("Save error")).when(scenarioService).saveMarkingSchema(eq(configId), anyList());

        // Act
        ResponseEntity<String> response = scenarioController.saveMarkingSchema(configId, markingSchemas);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to save marking schemas.", response.getBody());
        verify(scenarioService, times(1)).saveMarkingSchema(eq(configId), eq(markingSchemas));
    }

    @Test
    void testGetMarkingSchemas_Success() throws Exception {
        // Arrange
        int configId = 1;
        List<MarkingSchema> markingSchemas = Collections.singletonList(new MarkingSchema());
        when(scenarioService.getMarkingSchemas(configId)).thenReturn(markingSchemas);

        // Act
        ResponseEntity<List<MarkingSchema>> response = scenarioController.getMarkingSchemas(configId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(markingSchemas, response.getBody());
        verify(scenarioService, times(1)).getMarkingSchemas(configId);
    }

    @Test
    void testGetMarkingSchemas_Error() throws Exception {
        // Arrange
        int configId = 1;
        when(scenarioService.getMarkingSchemas(configId)).thenThrow(new RuntimeException("Retrieval error"));

        // Act
        ResponseEntity<List<MarkingSchema>> response = scenarioController.getMarkingSchemas(configId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(scenarioService, times(1)).getMarkingSchemas(configId);
    }

    @Test
    void testDeleteMarkingSchema_Success() {
        // Arrange
        int configId = 1;
        int schemaId = 2;
        when(scenarioService.deleteMarkingSchema(configId, schemaId)).thenReturn(true);

        // Act
        ResponseEntity<String> response = scenarioController.deleteMarkingSchema(configId, schemaId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Marking schema deleted.", response.getBody());
        verify(scenarioService, times(1)).deleteMarkingSchema(configId, schemaId);
    }

    @Test
    void testDeleteMarkingSchema_NotFound() {
        // Arrange
        int configId = 1;
        int schemaId = 2;
        when(scenarioService.deleteMarkingSchema(configId, schemaId)).thenReturn(false);

        // Act
        ResponseEntity<String> response = scenarioController.deleteMarkingSchema(configId, schemaId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(scenarioService, times(1)).deleteMarkingSchema(configId, schemaId);
    }
}