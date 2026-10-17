package com.csirt.notifiche.scadenze;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.csirt.notifiche.model.TipoDocumento;

/**
 * Calcola i termini di notifica. Le ore sono tempo reale (Instant + Duration);
 * il mese è calendariale e si calcola nel fuso orario italiano.
 */
public class CalcolatoreScadenze {

    public static final ZoneId FUSO_ORARIO = ZoneId.of("Europe/Rome");
    static final Duration TERMINE_PRE_NOTIFICA = Duration.ofHours(24);
    static final Duration TERMINE_NOTIFICA = Duration.ofHours(72);

    /** Variante semplice: è nota al più la trasmissione della notifica. */
    public List<Scadenza> calcola(Instant conoscenza, Instant trasmissioneNotifica, Instant adesso) {
        Map<TipoDocumento, Instant> trasmissioni = new EnumMap<>(TipoDocumento.class);
        if (trasmissioneNotifica != null) {
            trasmissioni.put(TipoDocumento.NOTIFICA, trasmissioneNotifica);
        }
        return calcolaConTrasmissioni(conoscenza, trasmissioni, adesso);
    }

    /**
     * @param trasmissioni per ogni tipo, il momento della prima trasmissione (se avvenuta)
     */
    public List<Scadenza> calcolaConTrasmissioni(Instant conoscenza, Map<TipoDocumento, Instant> trasmissioni,
                                                 Instant adesso) {
        if (conoscenza == null) {
            return Arrays.stream(TipoDocumento.values())
                    .map(tipo -> new Scadenza(tipo, null, StatoScadenza.NON_ATTIVA, null, null))
                    .toList();
        }
        Instant notificaInviata = trasmissioni.get(TipoDocumento.NOTIFICA);
        Instant termineRelazione = notificaInviata == null
                ? null
                : notificaInviata.atZone(FUSO_ORARIO).plusMonths(1).toInstant();

        return List.of(
                scadenza(TipoDocumento.PRE_NOTIFICA, conoscenza.plus(TERMINE_PRE_NOTIFICA),
                        trasmissioni.get(TipoDocumento.PRE_NOTIFICA), adesso),
                scadenza(TipoDocumento.NOTIFICA, conoscenza.plus(TERMINE_NOTIFICA), notificaInviata, adesso),
                scadenza(TipoDocumento.RELAZIONE_FINALE, termineRelazione,
                        trasmissioni.get(TipoDocumento.RELAZIONE_FINALE), adesso));
    }

    private Scadenza scadenza(TipoDocumento tipo, Instant termine, Instant trasmissione, Instant adesso) {
        if (termine == null) {
            return new Scadenza(tipo, null, StatoScadenza.IN_ATTESA, null, null);
        }
        if (trasmissione != null) {
            StatoScadenza esito = trasmissione.isAfter(termine)
                    ? StatoScadenza.TRASMESSA_IN_RITARDO
                    : StatoScadenza.RISPETTATA;
            return new Scadenza(tipo, termine, esito, Duration.between(trasmissione, termine).toMinutes(), trasmissione);
        }
        StatoScadenza stato = adesso.isAfter(termine) ? StatoScadenza.SCADUTA : StatoScadenza.IN_CORSO;
        return new Scadenza(tipo, termine, stato, Duration.between(adesso, termine).toMinutes(), null);
    }
}