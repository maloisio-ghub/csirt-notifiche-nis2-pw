package com.csirt.notifiche.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.dto.ImpattoRichiesto;
import com.csirt.notifiche.dto.RichiestaValutazione;
import com.csirt.notifiche.dto.ValutazioneDto;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.model.ParametriSignificativita;
import com.csirt.notifiche.model.ServizioCritico;
import com.csirt.notifiche.model.ValutazioneSignificativita;
import com.csirt.notifiche.repository.ServizioCriticoRepository;
import com.csirt.notifiche.repository.ValutazioneSignificativitaRepository;
import com.csirt.notifiche.significativita.DatiValutazione;
import com.csirt.notifiche.significativita.EsitoValutazione;
import com.csirt.notifiche.significativita.ImpattoServizio;
import com.csirt.notifiche.significativita.MotoreSignificativita;

@Service
public class SignificativitaService {

    private final MotoreSignificativita motore = new MotoreSignificativita();
    private final IncidenteService incidenteService;
    private final ConfigurazioneService configurazione;
    private final ServizioCriticoRepository servizi;
    private final ValutazioneSignificativitaRepository valutazioni;
    private final Clock clock;

    public SignificativitaService(IncidenteService incidenteService, ConfigurazioneService configurazione,
                                  ServizioCriticoRepository servizi, ValutazioneSignificativitaRepository valutazioni,
                                  Clock clock) {
        this.incidenteService = incidenteService;
        this.configurazione = configurazione;
        this.servizi = servizi;
        this.valutazioni = valutazioni;
        this.clock = clock;
    }

    @Transactional
    public ValutazioneDto valuta(Long incidenteId, RichiestaValutazione richiesta) {
        Incidente incidente = incidenteService.trova(incidenteId);
        ParametriSignificativita parametri = configurazione.parametriInVigore();

        DatiValutazione dati = new DatiValutazione(
                richiesta.perditaEconomica(),
                vero(richiesta.esfiltrazioneSegretiCommerciali()),
                vero(richiesta.decesso()),
                vero(richiesta.dannoConsiderevoleSalute()),
                vero(richiesta.accessoMalevoloConGravePerturbazione()),
                impatti(richiesta.impatti()));
        EsitoValutazione esito = motore.valuta(dati, parametri.comeSoglia());
        Instant adesso = Instant.now(clock);

        try {
            if (esito.significativo()) {
                incidente.dichiaraSignificativo(adesso);
            } else {
                incidente.dichiaraNonSignificativo();
            }
        } catch (IllegalStateException e) {
            throw new OperazioneNonConsentitaException(e.getMessage());
        }

        ValutazioneSignificativita valutazione =
                valutazioni.save(new ValutazioneSignificativita(incidente, parametri, adesso, dati, esito));
        return ValutazioneDto.da(valutazione);
    }

    @Transactional(readOnly = true)
    public List<ValutazioneDto> storico(Long incidenteId) {
        incidenteService.trova(incidenteId); // 404 se l'incidente non esiste
        return valutazioni.findByIncidenteIdOrderByDataValutazioneDesc(incidenteId).stream()
                .map(ValutazioneDto::da).toList();
    }

    /** Arricchisce ogni impatto con la base utenti presa dal database. */
    private List<ImpattoServizio> impatti(List<ImpattoRichiesto> richiesti) {
        if (richiesti == null) {
            return List.of();
        }
        List<ImpattoServizio> risultato = new ArrayList<>();
        Set<String> visti = new HashSet<>();
        for (ImpattoRichiesto r : richiesti) {
            ServizioCritico servizio = servizi.findByCodice(r.codiceServizio())
                    .orElseThrow(() -> new RichiestaNonValidaException(
                            "Servizio critico sconosciuto: " + r.codiceServizio()));
            if (!visti.add(servizio.getCodice())) {
                throw new RichiestaNonValidaException("Servizio indicato più volte: " + servizio.getCodice());
            }
            risultato.add(new ImpattoServizio(servizio.getCodice(), servizio.getBaseUtenti(),
                    intero(r.minutiIndisponibilitaTotale()), intero(r.minutiDisponibilitaLimitata()),
                    lungo(r.utentiDisponibilitaLimitata()), vero(r.compromissioneMalevola()),
                    lungo(r.utentiCompromissione())));
        }
        return risultato;
    }

    private static boolean vero(Boolean valore) {
        return Boolean.TRUE.equals(valore);
    }

    private static int intero(Integer valore) {
        return valore == null ? 0 : valore;
    }

    private static long lungo(Long valore) {
        return valore == null ? 0 : valore;
    }
}