package com.communication.communication_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    private int scenarioId;

    @Column(nullable = true)
    private String title;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String shortDescription;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String prompt;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String taskInstruction;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String backgroundInformation;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String personality;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String questionsForDoctor;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String responseGuidelines;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String sampleResponses;

    @Column(nullable = true)
    private String userId;  // New field to store the user who created or last edited the scenario

    @Column(nullable = true)
    private String agentId;
}