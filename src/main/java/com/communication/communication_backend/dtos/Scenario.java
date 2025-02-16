package com.communication.communication_backend.dtos;

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

    @Column(nullable = false, length = 1000)
    private String shortDescription;

    @Column(nullable = true, length = 2000)
    private String prompt;
}
