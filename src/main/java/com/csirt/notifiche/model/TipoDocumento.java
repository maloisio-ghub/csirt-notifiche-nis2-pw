package com.csirt.notifiche.model;

/** I tre adempimenti verso il CSIRT Italia (art. 23.4 Dir. UE 2022/2555, recepito dal D.Lgs. 138/2024). */
public enum TipoDocumento {

    PRE_NOTIFICA("Pre-notifica", "entro 24 ore dalla conoscenza dell'incidente significativo", "art. 23.4.a"),
    NOTIFICA("Notifica dell'incidente", "entro 72 ore dalla conoscenza dell'incidente significativo", "art. 23.4.b"),
    RELAZIONE_FINALE("Relazione finale", "entro un mese dalla trasmissione della notifica", "art. 23.4.d");

    private final String etichetta;
    private final String termine;
    private final String riferimento;

    TipoDocumento(String etichetta, String termine, String riferimento) {
        this.etichetta = etichetta;
        this.termine = termine;
        this.riferimento = riferimento;
    }

    public String getEtichetta() { return etichetta; }
    public String getTermine() { return termine; }
    public String getRiferimento() { return riferimento; }
}