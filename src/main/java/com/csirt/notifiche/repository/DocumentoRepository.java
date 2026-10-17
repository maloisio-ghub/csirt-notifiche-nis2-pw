package com.csirt.notifiche.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.csirt.notifiche.model.Documento;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.TipoDocumento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findByIncidenteIdOrderByIdAsc(Long incidenteId);

    /** Il documento più recente dell'incidente, di qualunque tipo: la base per il riuso. */
    Optional<Documento> findTopByIncidenteIdOrderByIdDesc(Long incidenteId);

    /** L'ultima versione di un certo tipo. */
    Optional<Documento> findTopByIncidenteIdAndTipoOrderByVersioneDesc(Long incidenteId, TipoDocumento tipo);

    boolean existsByIncidenteIdAndTipoAndStato(Long incidenteId, TipoDocumento tipo, StatoDocumento stato);

    /** La prima versione trasmessa di un tipo: da lì si misura il rispetto dei termini. */
    Optional<Documento> findFirstByIncidenteIdAndTipoAndStatoOrderByVersioneAsc(Long incidenteId, TipoDocumento tipo, StatoDocumento stato);
}