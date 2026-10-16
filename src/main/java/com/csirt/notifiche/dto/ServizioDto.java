package com.csirt.notifiche.dto;

import com.csirt.notifiche.model.ServizioCritico;
import com.csirt.notifiche.significativita.ParametriSoglia;

public record ServizioDto(String codice, String nome, String descrizione, long baseUtenti, long sogliaUtenti) {

    public static ServizioDto da(ServizioCritico s, ParametriSoglia soglie) {
        return new ServizioDto(s.getCodice(), s.getNome(), s.getDescrizione(), s.getBaseUtenti(),
                soglie.sogliaUtenti(s.getBaseUtenti()));
    }
}
