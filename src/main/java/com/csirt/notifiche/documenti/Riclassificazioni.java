package com.csirt.notifiche.documenti;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import com.csirt.notifiche.model.Classificazione;

/** Ricostruisce i cambi di classificazione tra due versioni, seguendo la catena delle precedenti. */
public final class Riclassificazioni {

    public record Riclassificazione(int versionePrecedente, String classificazionePrecedente, int versioneNuova,
                                    String classificazioneNuova, Instant momento, String motivazione) {
    }

    private Riclassificazioni() {
    }

    /**
     * Cambiamenti da {@code riferimento} (esclusa) a {@code finale} (inclusa), in ordine cronologico.
     * Lista vuota se manca una delle due o se il riferimento non è nella catena di {@code finale}.
     */
    public static List<Riclassificazione> tra(Classificazione riferimento, Classificazione finale) {
        if (riferimento == null || finale == null) {
            return List.of();
        }
        LinkedList<Riclassificazione> passi = new LinkedList<>();
        Classificazione corrente = finale;
        while (!corrente.getId().equals(riferimento.getId())) {
            Classificazione precedente = corrente.getPrecedente();
            if (precedente == null) {
                return List.of();
            }
            passi.addFirst(new Riclassificazione(precedente.getVersione(), precedente.codiciOrdinati(),
                    corrente.getVersione(), corrente.codiciOrdinati(), corrente.getDataClassificazione(),
                    corrente.getMotivazione()));
            corrente = precedente;
        }
        return List.copyOf(passi);
    }
}