package com.communication.communication_backend.repository;

import com.communication.communication_backend.entity.MarkingSchema;
import com.communication.communication_backend.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarkingSchemaRepository extends JpaRepository<MarkingSchema, Integer> {
    List<MarkingSchema> findByScenario(Scenario scenario);
}
