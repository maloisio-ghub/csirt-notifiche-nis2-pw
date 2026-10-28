package com.csirt.notifiche.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.dto.ClassificazioneDto;
import com.csirt.notifiche.dto.RichiestaClassificazione;
import com.csirt.notifiche.model.Classificazione;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.model.StatoIncidente;
import com.csirt.notifiche.model.TipoVoce;
import com.csirt.notifiche.model.VersioneTassonomia;
import com.csirt.notifiche.model.VoceTassonomia;
import com.csirt.notifiche.repository.ClassificazioneRepository;
import com.csirt.notifiche.repository.VersioneTassonomiaRepository;
import com.csirt.notifiche.repository.VoceTassonomiaRepository;

/**
 * Classificazione TC-ACN degli incidenti. La prima richiesta crea la versione 1; le successive sono
 * riclassificazioni, che richiedono una motivazione e conservano la versione precedente.
 */
@Service
public class ClassificazioneService {

    private final ClassificazioneRepository classificazioni;
    private final VersioneTassonomiaRepository versioni;
    private final VoceTassonomiaRepository voci;
    private final IncidenteService incidenteService;
    private final Clock clock;

    public ClassificazioneService(ClassificazioneRepository classificazioni, VersioneTassonomiaRepository versioni,
                                  VoceTassonomiaRepository voci, IncidenteService incidenteService, Clock clock) {
        this.classificazioni = classificazioni;
        this.versioni = versioni;
        this.voci = voci;
        this.incidenteService = incidenteService;
        this.clock = clock;
    }

    @Transactional
    public ClassificazioneDto classifica(Long incidenteId, RichiestaClassificazione richiesta) {
        Incidente incidente = incidenteService.trova(incidenteId);
        if (incidente.getStato() != StatoIncidente.SIGNIFICATIVO) {
            throw new OperazioneNonConsentitaException("Si classificano solo incidenti significativi non chiusi (stato "
                    + incidente.getStato() + ")");
        }
        VersioneTassonomia tassonomia = versioni.findByAttivaTrue()
                .orElseThrow(() -> new IllegalStateException("Nessuna versione della tassonomia attiva"));
        Set<VoceTassonomia> valori = valoriValidati(tassonomia, richiesta.codici());
        // Microsecondi, come nel database: il momento entra nell'impronta della relazione finale.
        Instant adesso = Instant.now(clock);

        Optional<Classificazione> attiva = classificazioni.findByIncidenteIdAndAttivaTrue(incidenteId);
        if (attiva.isEmpty()) {
            return ClassificazioneDto.da(classificazioni.save(
                    Classificazione.iniziale(incidente, tassonomia, valori, adesso)));
        }
        try {
            Classificazione nuova = attiva.get().riclassifica(tassonomia, valori, adesso, richiesta.motivazione());
            return ClassificazioneDto.da(classificazioni.save(nuova));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new RichiestaNonValidaException(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ClassificazioneDto> storico(Long incidenteId) {
        incidenteService.trova(incidenteId);
        return classificazioni.findByIncidenteIdOrderByVersioneAsc(incidenteId).stream()
                .map(ClassificazioneDto::da).toList();
    }

    
    private Set<VoceTassonomia> valoriValidati(VersioneTassonomia tassonomia, List<String> codici) {
        Set<String> richiesti = new TreeSet<>(codici);
        List<VoceTassonomia> trovate = voci.findByVersioneIdAndCodiceIn(tassonomia.getId(), richiesti);

        Set<String> mancanti = new TreeSet<>(richiesti);
        trovate.forEach(v -> mancanti.remove(v.getCodice()));
        if (!mancanti.isEmpty()) {
            throw new RichiestaNonValidaException("Codici non presenti nella tassonomia " + tassonomia.getCodice()
                    + ": " + mancanti);
        }
        List<String> nonValori = trovate.stream().filter(v -> v.getTipo() != TipoVoce.VALORE)
                .map(VoceTassonomia::getCodice).sorted().toList();
        if (!nonValori.isEmpty()) {
            throw new RichiestaNonValidaException("Si assegnano solo valori, non macrocategorie o predicati: " + nonValori);
        }
        Map<VoceTassonomia, List<VoceTassonomia>> perPredicato = trovate.stream()
                .collect(Collectors.groupingBy(VoceTassonomia::getPadre));
        perPredicato.forEach((predicato, valoriDelPredicato) -> {
            if (!predicato.isSelezioneMultipla() && valoriDelPredicato.size() > 1) {
                throw new RichiestaNonValidaException("Il predicato " + predicato.getCodice() + " ("
                        + predicato.getNome() + ") ammette un solo valore");
            }
        });
        return new HashSet<>(trovate);
    }
}