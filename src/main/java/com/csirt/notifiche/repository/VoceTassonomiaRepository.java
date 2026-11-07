package com.csirt.notifiche.repository;

import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.csirt.notifiche.model.VoceTassonomia;

public interface VoceTassonomiaRepository extends JpaRepository<VoceTassonomia, Long> {

    List<VoceTassonomia> findByVersioneIdOrderByOrdine(Long versioneId);
    List<VoceTassonomia> findByVersioneIdAndCodiceIn(Long versioneId, Collection<String> codici);

    /** Query di conteggio per livello. */
    @Query(value = """
            SELECT v.tipo AS tipo, COUNT(*) AS numero
            FROM voce_tassonomia v
            JOIN versione_tassonomia t ON t.id = v.versione_id
            WHERE t.codice = :codice
            GROUP BY v.tipo
            ORDER BY v.tipo
            """, nativeQuery = true)
    List<Object[]> contaPerTipo(@Param("codice") String codice);

}  