package com.csirt.notifiche.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.csirt.notifiche.model.ServizioCritico;

public interface ServizioCriticoRepository extends JpaRepository<ServizioCritico, Long> {

    Optional<ServizioCritico> findByCodice(String codice);
}
