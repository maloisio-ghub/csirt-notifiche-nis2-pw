package com.csirt.notifiche.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.csirt.notifiche.model.ParametriSignificativita;

public interface ParametriSignificativitaRepository extends JpaRepository<ParametriSignificativita, Long> {

    /** La versione più recente è quella in vigore. */
    Optional<ParametriSignificativita> findTopByOrderByVersioneDesc();
}
