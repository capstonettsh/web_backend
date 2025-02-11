package com.communication.doctorconfig.service.creatingScenarios;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Scenario {
    private int configId;
    private String title;
    private String shortDescription;
    private String prompt;
}
