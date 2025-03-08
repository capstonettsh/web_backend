package com.communication.communication_backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneratedScenario {
    private int scenarioId;
    private String title;
    private String userId; // New field to track who created/edited the scenario
    private String shortDescription;
}
