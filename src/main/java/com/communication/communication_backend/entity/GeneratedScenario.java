package com.communication.communication_backend.entity;

import com.communication.communication_backend.entity.Scenario;
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


    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String taskInstruction;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String backgroundInformation;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String personality;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String questionsForDoctor;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String responseGuidelines;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String sampleResponses;
}
