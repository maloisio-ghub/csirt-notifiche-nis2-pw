package com.csirt.notifiche.scadenze;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import com.csirt.notifiche.model.TipoDocumento;

/**
 * Calcola i termini di notifica. Le ore sono tempo reale (Instant + Duration);
 * il mese è calendariale e si calcola nel fuso orario italiano.
 */
public class CalcolatoreScadenze {

    public static final ZoneId FUSO_ORARIO = ZoneId.of("Europe/Rome");
    static final Duration TERMINE_PRE_NOTIFICA = Duration.ofHours(24);
    static final Duration TERMINE_NOTIFICA = Duration.ofHours(72);

    /**
     * @param conoscenza            momento di conoscenza della significatività (null se non significativo)
     * @param trasmissioneNotifica  momento di invio della notifica delle 72 ore (null se non ancora inviata)
     * @param adesso                istante di riferimento per lo stato
     */
    public List<Scadenza> calcola(Instant conoscenza, Instant trasmissioneNotifica, Instant adesso) {
        if (conoscenza == null) {
            return Arrays.stream(TipoDocumento.values())
                    .map(tipo -> new Scadenza(tipo, null, StatoScadenza.NON_ATTIVA, null))
                    .toList();
        }
        Instant termineRelazione = trasmissioneNotifica == null
                ? null
                : trasmissioneNotifica.atZone(FUSO_ORARIO).plusMonths(1).toInstant();

        return List.of(
                scadenza(TipoDocumento.PRE_NOTIFICA, conoscenza.plus(TERMINE_PRE_NOTIFICA), adesso),
                scadenza(TipoDocumento.NOTIFICA, conoscenza.plus(TERMINE_NOTIFICA), adesso),
                scadenza(TipoDocumento.RELAZIONE_FINALE, termineRelazione, adesso));
    }

    private Scadenza scadenza(TipoDocumento tipo, Instant termine, Instant adesso) {
        if (termine == null) {
            return new Scadenza(tipo, null, StatoScadenza.IN_ATTESA, null);
        }
        long minuti = Duration.between(adesso, termine).toMinutes();
        StatoScadenza stato = adesso.isAfter(termine) ? StatoScadenza.SCADUTA : StatoScadenza.IN_CORSO;
        return new Scadenza(tipo, termine, stato, minuti);
    }
}