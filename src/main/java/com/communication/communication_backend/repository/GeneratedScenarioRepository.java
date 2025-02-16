package com.communication.communication_backend.repository;

import com.communication.communication_backend.dtos.GeneratedScenario;
import com.communication.communication_backend.dtos.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedScenarioRepository extends JpaRepository<GeneratedScenario, Integer> {
    List<GeneratedScenario> findByScenario(Scenario scenario);
}
