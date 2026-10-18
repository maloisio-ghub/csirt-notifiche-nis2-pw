package com.csirt.notifiche.tassonomia;

import com.csirt.notifiche.model.TipoVoce;

public record RigaTassonomia(TipoVoce tipo, String codice, String nome, String padre, boolean multiplo,
                             String gruppo, String descrizione) {
}