package com.csirt.notifiche.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.csirt.notifiche.model.VersioneTassonomia;

public interface VersioneTassonomiaRepository extends JpaRepository<VersioneTassonomia, Long> {

    boolean existsByCodice(String codice);

    Optional<VersioneTassonomia> findByAttivaTrue();
}