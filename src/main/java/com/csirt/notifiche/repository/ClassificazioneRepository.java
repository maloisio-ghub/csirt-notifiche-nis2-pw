package com.csirt.notifiche.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.csirt.notifiche.model.Classificazione;

public interface ClassificazioneRepository extends JpaRepository<Classificazione, Long> {

    Optional<Classificazione> findByIncidenteIdAndAttivaTrue(Long incidenteId);

    List<Classificazione> findByIncidenteIdOrderByVersioneAsc(Long incidenteId);
}