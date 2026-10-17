package com.csirt.notifiche.dto;

import com.csirt.notifiche.model.ContenutoNotifica;
import com.csirt.notifiche.model.GravitaIncidente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Contenuto di un documento. In bozza ogni campo può essere vuoto: qui si controlla
 * solo il formato. I campi obbligatori per adempimento si verificano alla trasmissione.
 */
public record ContenutoDto(
        @Size(max = 200) String referenteNome,
        @Email @Size(max = 200) String referenteEmail,
        @Size(max = 2000) String descrizioneSintetica,
        Boolean sospettoMalevolo,
        Boolean impattoTransfrontaliero,
        GravitaIncidente gravita,
        @Size(max = 4000) String impatto,
        @Size(max = 4000) String indicatoriCompromissione,
        @Size(max = 8000) String descrizioneDettagliata,
        @Size(max = 4000) String causaPrincipale,
        @Size(max = 4000) String misureMitigazione) {

    public ContenutoNotifica comeContenuto() {
        return new ContenutoNotifica(referenteNome, referenteEmail, descrizioneSintetica, sospettoMalevolo,
                impattoTransfrontaliero, gravita, impatto, indicatoriCompromissione, descrizioneDettagliata,
                causaPrincipale, misureMitigazione);
    }

    public static ContenutoDto da(ContenutoNotifica c) {
        return new ContenutoDto(c.getReferenteNome(), c.getReferenteEmail(), c.getDescrizioneSintetica(),
                c.getSospettoMalevolo(), c.getImpattoTransfrontaliero(), c.getGravita(), c.getImpatto(),
                c.getIndicatoriCompromissione(), c.getDescrizioneDettagliata(), c.getCausaPrincipale(),
                c.getMisureMitigazione());
    }
}