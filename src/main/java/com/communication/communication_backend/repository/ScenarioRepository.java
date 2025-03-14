package com.communication.communication_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.communication.communication_backend.entity.Scenario;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Integer> {
}
