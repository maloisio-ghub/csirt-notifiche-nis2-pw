package com.csirt.notifiche.documenti;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.csirt.notifiche.model.Classificazione;

/** Ricostruisce i cambi di classificazione tra due versioni, confrontando ogni versione con quella prima. */
public final class Riclassificazioni {

    public record Riclassificazione(int versionePrecedente, String classificazionePrecedente, int versioneNuova,
                                    String classificazioneNuova, Instant momento, String motivazione) {
    }

    private Riclassificazioni() {
    }

    /**
     * Cambiamenti successivi a {@code riferimento} e fino a {@code finale} (inclusa), in ordine cronologico.
     * {@code storico} contiene tutte le classificazioni dell'incidente, ordinate per versione.
     */
    public static List<Riclassificazione> tra(List<Classificazione> storico, Classificazione riferimento,
                                              Classificazione finale) {
        if (riferimento == null || finale == null) {
            return List.of();
        }
        List<Riclassificazione> passi = new ArrayList<>();
        for (int i = 1; i < storico.size(); i++) {
            Classificazione precedente = storico.get(i - 1);
            Classificazione corrente = storico.get(i);
            if (corrente.getVersione() > riferimento.getVersione() && corrente.getVersione() <= finale.getVersione()) {
                passi.add(new Riclassificazione(precedente.getVersione(), precedente.codiciOrdinati(),
                        corrente.getVersione(), corrente.codiciOrdinati(), corrente.getDataClassificazione(),
                        corrente.getMotivazione()));
            }
        }
        return passi;
    }
}