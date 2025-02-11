package com.communication.communication_backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarkingSchemaDto {
    private int schemaId;  // Unique ID for this marking schema within a scenario
    private String title;  // Title of the rubric schema
    private String unsatisfactory;  // Description of unsatisfactory performance
    private String borderline;  // Description of borderline performance
    private String satisfactory;  // Description of satisfactory performance
}