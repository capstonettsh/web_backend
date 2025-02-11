package com.communication.doctorconfig.service.creatingScenarios;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScenarioSummary {
    private int configId;
    private String title;    
}
