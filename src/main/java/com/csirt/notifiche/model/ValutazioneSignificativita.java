package com.csirt.notifiche.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.csirt.notifiche.significativita.DatiValutazione;
import com.csirt.notifiche.significativita.EsitoValutazione;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/** Una valutazione di significatività: dati in ingresso ed esito. Non si modifica. */
@Entity
@Table(name = "valutazione_significativita")
public class ValutazioneSignificativita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @Column(name = "data_valutazione", nullable = false)
    private Instant dataValutazione;

    @Column(name = "perdita_economica", nullable = false, precision = 15, scale = 2)
    private BigDecimal perditaEconomica;

    @Column(name = "esfiltrazione_segreti", nullable = false)
    private boolean esfiltrazioneSegretiCommerciali;

    @Column(name = "decesso", nullable = false)
    private boolean decesso;

    @Column(name = "danno_salute", nullable = false)
    private boolean dannoConsiderevoleSalute;

    @Column(name = "accesso_malevolo", nullable = false)
    private boolean accessoMalevoloConGravePerturbazione;

    @ElementCollection
    @CollectionTable(name = "valutazione_impatto", joinColumns = @JoinColumn(name = "valutazione_id"))
    @OrderColumn(name = "posizione")
    private List<ImpattoValutato> impatti = new ArrayList<>();

    @Column(name = "esito_significativo", nullable = false)
    private boolean esitoSignificativo;

    /** Codici delle regole scattate, es. "R7,R8". */
    @Column(name = "regole", length = 100)
    private String regole;

    /** Una riga per regola, con riferimento normativo e numeri. */
    @Column(name = "motivazioni", length = 4000)
    private String motivazioni;

    protected ValutazioneSignificativita() {
        // richiesto da JPA
    }

      public ValutazioneSignificativita(Incidente incidente, Instant momento,
                                        DatiValutazione dati, EsitoValutazione esito) {
        this.incidente = incidente;
        this.dataValutazione = momento;
        this.perditaEconomica = dati.perditaEconomica();
        this.esfiltrazioneSegretiCommerciali = dati.esfiltrazioneSegretiCommerciali();
        this.decesso = dati.decesso();
        this.dannoConsiderevoleSalute = dati.dannoConsiderevoleSalute();
        this.accessoMalevoloConGravePerturbazione = dati.accessoMalevoloConGravePerturbazione();
        dati.impatti().forEach(i -> this.impatti.add(new ImpattoValutato(i)));
        this.esitoSignificativo = esito.significativo();
        this.regole = String.join(",", esito.codici());
        this.motivazioni = esito.regole().stream()
                .map(r -> r.codice() + " (" + r.riferimento() + "): " + r.motivazione())
                .collect(Collectors.joining("\n"));
    }

    public List<String> elencoRegole() {
        return regole == null || regole.isBlank() ? List.of() : List.of(regole.split(","));
    }

    public List<String> elencoMotivazioni() {
        return motivazioni == null || motivazioni.isBlank() ? List.of() : List.of(motivazioni.split("\n"));
    }

    public Long getId() { return id; }
    public Incidente getIncidente() { return incidente; }
    public Instant getDataValutazione() { return dataValutazione; }
    public BigDecimal getPerditaEconomica() { return perditaEconomica; }
    public List<ImpattoValutato> getImpatti() { return impatti; }
    public boolean isEsitoSignificativo() { return esitoSignificativo; }
}