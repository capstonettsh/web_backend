package com.communication.communication_backend.repository;

import com.communication.communication_backend.entity.GeneratedScenario;
import com.communication.communication_backend.entity.Scenario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratedScenarioRepository extends JpaRepository<GeneratedScenario, Integer> {
    List<GeneratedScenario> findByScenario(Scenario scenario);

    Optional<GeneratedScenario> findFirstByScenarioOrderByIdDesc(Scenario scenario);
}
