package com.csirt.notifiche.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.csirt.notifiche.model.ValutazioneSignificativita;

public interface ValutazioneSignificativitaRepository extends JpaRepository<ValutazioneSignificativita, Long> {

    List<ValutazioneSignificativita> findByIncidenteIdOrderByDataValutazioneDesc(Long incidenteId);
}