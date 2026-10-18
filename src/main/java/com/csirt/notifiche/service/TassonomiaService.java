package com.csirt.notifiche.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.dto.ConteggiDto;
import com.csirt.notifiche.dto.ConteggioMacrocategoria;
import com.csirt.notifiche.dto.ConteggioTipo;
import com.csirt.notifiche.dto.TassonomiaDto;
import com.csirt.notifiche.dto.VersioneDto;
import com.csirt.notifiche.dto.VoceDto;
import com.csirt.notifiche.model.VersioneTassonomia;
import com.csirt.notifiche.model.VoceTassonomia;
import com.csirt.notifiche.repository.VersioneTassonomiaRepository;
import com.csirt.notifiche.repository.VoceTassonomiaRepository;
import com.csirt.notifiche.tassonomia.DefinizioneTassonomia;
import com.csirt.notifiche.tassonomia.LettoreCsv;
import com.csirt.notifiche.tassonomia.RigaTassonomia;
import com.csirt.notifiche.tassonomia.TassonomiaNonValidaException;

@Service
public class TassonomiaService {

    private static final Logger log = LoggerFactory.getLogger(TassonomiaService.class);
    private static final List<String> INTESTAZIONE_MANIFEST = List.of("codice", "titolo", "fonte_url",
            "data_pubblicazione", "data_acquisizione", "sha256_documento", "attiva", "file");

    private final VersioneTassonomiaRepository versioni;
    private final VoceTassonomiaRepository voci;
    private final Clock clock;

    public TassonomiaService(VersioneTassonomiaRepository versioni, VoceTassonomiaRepository voci, Clock clock) {
        this.versioni = versioni;
        this.voci = voci;
        this.clock = clock;
    }

    /** Carica le versioni elencate nel manifest e non ancora presenti nel database. */
    @Transactional
    public void caricaDaManifest(String percorsoManifest) {
        List<List<String>> manifest = leggi(percorsoManifest);
        if (manifest.isEmpty() || !manifest.get(0).equals(INTESTAZIONE_MANIFEST)) {
            throw new TassonomiaNonValidaException("Intestazione del manifest attesa: "
                    + String.join(";", INTESTAZIONE_MANIFEST));
        }
        String cartella = percorsoManifest.substring(0, percorsoManifest.lastIndexOf('/') + 1);
        for (List<String> r : manifest.subList(1, manifest.size())) {
            String codice = r.get(0);
            if (versioni.existsByCodice(codice)) {
                continue;
            }
            DefinizioneTassonomia definizione = DefinizioneTassonomia.da(leggi(cartella + r.get(7)));
            boolean attiva = "S".equals(r.get(6));
            if (attiva) {
                versioni.findByAttivaTrue().ifPresent(VersioneTassonomia::disattiva);
            }
            VersioneTassonomia versione = versioni.save(new VersioneTassonomia(codice, r.get(1), r.get(2),
                    LocalDate.parse(r.get(3)), LocalDate.parse(r.get(4)), r.get(5), r.get(7), Instant.now(clock),
                    attiva));
            salvaVoci(versione, definizione);
            log.info("Tassonomia {} caricata da {}: {}", codice, r.get(7), definizione.conteggi());
        }
    }

    @Transactional(readOnly = true)
    public TassonomiaDto attiva() {
        VersioneTassonomia versione = versioni.findByAttivaTrue()
                .orElseThrow(() -> new IllegalStateException("Nessuna versione della tassonomia attiva"));
        List<VoceDto> elenco = voci.findByVersioneIdOrderByOrdine(versione.getId()).stream().map(VoceDto::da).toList();
        return new TassonomiaDto(VersioneDto.da(versione), elenco);
    }

    @Transactional(readOnly = true)
    public ConteggiDto conteggi(String codiceVersione) {
        if (!versioni.existsByCodice(codiceVersione)) {
            throw new RichiestaNonValidaException("Versione della tassonomia sconosciuta: " + codiceVersione);
        }
        List<ConteggioTipo> perTipo = voci.contaPerTipo(codiceVersione).stream()
                .map(r -> new ConteggioTipo(r[0].toString(), ((Number) r[1]).longValue()))
                .toList();
        List<ConteggioMacrocategoria> perMacro = voci.contaPerMacrocategoria(codiceVersione).stream()
                .map(r -> new ConteggioMacrocategoria(r[0].toString(), ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue()))
                .toList();
        return new ConteggiDto(codiceVersione, perTipo, perMacro);
    }

    private void salvaVoci(VersioneTassonomia versione, DefinizioneTassonomia definizione) {
        Map<String, VoceTassonomia> perCodice = new HashMap<>();
        int ordine = 0;
        for (RigaTassonomia r : definizione.righe()) {
            VoceTassonomia padre = r.padre().isEmpty() ? null : perCodice.get(r.padre());
            VoceTassonomia voce = new VoceTassonomia(versione, padre, r.tipo(), r.codice(), r.nome(),
                    vuotoComeNull(r.descrizione()), r.multiplo(), vuotoComeNull(r.gruppo()), ++ordine);
            perCodice.put(r.codice(), voci.save(voce));
        }
    }

    private static List<List<String>> leggi(String percorso) {
        try (Reader lettore = new InputStreamReader(new ClassPathResource(percorso).getInputStream(),
                StandardCharsets.UTF_8)) {
            return LettoreCsv.leggi(lettore);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile leggere " + percorso, e);
        }
    }

    private static String vuotoComeNull(String testo) {
        return testo == null || testo.isBlank() ? null : testo;
    }
}