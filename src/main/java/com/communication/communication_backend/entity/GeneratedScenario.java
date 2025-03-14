package com.communication.communication_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "generated_scenarios")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneratedScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "config_id", nullable = false)
    private Scenario scenario;  // Link to Scenario entity

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String taskInstruction;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String backgroundInformation;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String personality;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String questionsForDoctor;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String responseGuidelines;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String sampleResponses;
}
