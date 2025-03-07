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

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String unsatisfactory;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String borderline;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String satisfactory;
}
