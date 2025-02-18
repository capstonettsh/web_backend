package com.communication.communication_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenarios")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    private int configId;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String shortDescription;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(nullable = false)
    private String userId;  // New field to store the user who created or last edited the scenario
}