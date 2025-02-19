package com.communication.communication_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marking_schemas")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarkingSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int schemaId;

    @ManyToOne
    @JoinColumn(name = "config_id", nullable = false)
    private Scenario scenario;  // Link to Scenario entity

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 2000)
    private String unsatisfactory;

    @Column(nullable = false, length = 2000)
    private String borderline;

    @Column(nullable = false, length = 2000)
    private String satisfactory;
}
