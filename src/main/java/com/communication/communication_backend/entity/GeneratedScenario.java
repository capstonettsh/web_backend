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

    @Column(nullable = false, length = 5000)
    private String taskInstruction;

    @Column(nullable = false, length = 5000)
    private String backgroundInformation;

    @Column(nullable = false, length = 2000)
    private String personality;

    @Column(nullable = false, length = 5000)
    private String questionsForDoctor;

    @Column(nullable = false, length = 5000)
    private String responseGuidelines;

    @Column(nullable = false, length = 5000)
    private String sampleResponses;
}
