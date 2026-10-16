package com.csirt.notifiche.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "incidente")
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titolo;

    @Column(length = 4000)
    private String descrizione;

    /** Quando l'incidente è stato rilevato. */
    @Column(name = "data_rilevamento", nullable = false)
    private Instant dataRilevamento;

    /**
     * Quando si è saputo che l'incidente è significativo.
     * Da qui decorrono i termini di 24 h, 72 h e 1 mese (art. 23 NIS2).
     */
    @Column(name = "data_conoscenza")
    private Instant dataConoscenza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatoIncidente stato;

    @Column(name = "data_registrazione", nullable = false)
    private Instant dataRegistrazione;

    protected Incidente() {
        // richiesto da JPA
    }

    public Incidente(String titolo, String descrizione, Instant dataRilevamento, Instant adesso) {
        this.titolo = titolo;
        this.descrizione = descrizione;
        this.dataRilevamento = dataRilevamento;
        this.dataRegistrazione = adesso;
        this.stato = StatoIncidente.REGISTRATO;
    }

    /** Esito positivo: da questo momento decorrono i termini di notifica. */
    public void dichiaraSignificativo(Instant momento) {
        verificaValutabile();
        this.stato = StatoIncidente.SIGNIFICATIVO;
        this.dataConoscenza = momento;
    }

    /** Esito negativo: l'incidente potrà essere rivalutato se emergono nuove informazioni. */
    public void dichiaraNonSignificativo() {
        verificaValutabile();
        this.stato = StatoIncidente.NON_SIGNIFICATIVO;
    }

    private void verificaValutabile() {
        if (stato != StatoIncidente.REGISTRATO && stato != StatoIncidente.NON_SIGNIFICATIVO) {
            throw new IllegalStateException("L'incidente è " + stato + ": non può essere rivalutato");
        }
    }

    public Long getId() { return id; }
    public String getTitolo() { return titolo; }
    public String getDescrizione() { return descrizione; }
    public Instant getDataRilevamento() { return dataRilevamento; }
    public Instant getDataConoscenza() { return dataConoscenza; }
    public StatoIncidente getStato() { return stato; }
    public Instant getDataRegistrazione() { return dataRegistrazione; }
}