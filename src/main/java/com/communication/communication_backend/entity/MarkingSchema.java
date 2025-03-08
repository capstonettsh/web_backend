package com.communication.communication_backend.entity;

import com.communication.communication_backend.entity.Scenario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marking_schema")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarkingSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int markingSchemaId;

    @ManyToOne
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;  // Link to Scenario entity

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String unsatisfactory;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String borderline;

    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String satisfactory;
}
