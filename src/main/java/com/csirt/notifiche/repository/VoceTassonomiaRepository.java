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

    /** Predicati e valori per macrocategoria. */
    @Query(value = """
            SELECT m.codice AS macrocategoria,
                   SUM(CASE WHEN v.tipo = 'PREDICATO' THEN 1 ELSE 0 END) AS predicati,
                   SUM(CASE WHEN v.tipo = 'VALORE' THEN 1 ELSE 0 END) AS valori
            FROM voce_tassonomia v
            JOIN versione_tassonomia t ON t.id = v.versione_id
            JOIN voce_tassonomia m ON m.versione_id = v.versione_id
                 AND m.tipo = 'MACROCATEGORIA'
                 AND v.codice LIKE m.codice || ':%'
            WHERE t.codice = :codice
            GROUP BY m.codice
            ORDER BY MIN(m.ordine)
            """, nativeQuery = true)
    List<Object[]> contaPerMacrocategoria(@Param("codice") String codice);
}