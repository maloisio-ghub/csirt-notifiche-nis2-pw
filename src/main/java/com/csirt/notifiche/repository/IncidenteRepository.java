package com.csirt.notifiche.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.csirt.notifiche.model.Incidente;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {
}
