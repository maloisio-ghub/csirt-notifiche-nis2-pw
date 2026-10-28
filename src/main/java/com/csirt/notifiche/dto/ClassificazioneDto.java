package com.csirt.notifiche.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.csirt.notifiche.model.Classificazione;
import com.csirt.notifiche.model.VoceTassonomia;

public record ClassificazioneDto(
        Long id,
        int versione,
        String versioneTassonomia,
        List<ValoreClassificato> valori,
        Instant dataClassificazione,
        String motivazione,
        Integer versionePrecedente,
        boolean attiva) {

    public static ClassificazioneDto da(Classificazione c) {
        List<ValoreClassificato> valori = c.getValori().stream()
                .sorted(Comparator.comparing(VoceTassonomia::getCodice))
                .map(v -> new ValoreClassificato(v.getCodice(), v.getNome(), v.getPadre().getCodice()))
                .toList();
        return new ClassificazioneDto(c.getId(), c.getVersione(), c.getVersioneTassonomia().getCodice(), valori,
                c.getDataClassificazione(), c.getMotivazione(),
                c.getVersione() > 1 ? c.getVersione() - 1 : null, c.isAttiva());
    }
}