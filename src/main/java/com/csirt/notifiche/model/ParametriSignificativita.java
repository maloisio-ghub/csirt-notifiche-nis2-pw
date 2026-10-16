package com.csirt.notifiche.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.csirt.notifiche.significativita.ParametriSoglia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Soglie di significatività versionate: una nuova versione non modifica le precedenti. */
@Entity
@Table(name = "parametri_significativita")
public class ParametriSignificativita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "versione", nullable = false, unique = true)
    private int versione;

    @Column(name = "valido_dal", nullable = false)
    private LocalDate validoDal;

    @Column(name = "fatturato_annuo", nullable = false, precision = 15, scale = 2)
    private BigDecimal fatturatoAnnuo;

    @Column(name = "perdita_massima", nullable = false, precision = 15, scale = 2)
    private BigDecimal perditaMassima;

    @Column(name = "quota_fatturato", nullable = false, precision = 5, scale = 4)
    private BigDecimal quotaFatturato;

    @Column(name = "minuti_indisponibilita_totale", nullable = false)
    private int minutiIndisponibilitaTotale;

    @Column(name = "minuti_disponibilita_limitata", nullable = false)
    private int minutiDisponibilitaLimitata;

    @Column(name = "quota_utenti", nullable = false, precision = 5, scale = 4)
    private BigDecimal quotaUtenti;

    @Column(name = "utenti_massimi", nullable = false)
    private long utentiMassimi;

    /** Fonte e motivazione dei valori. */
    @Column(name = "nota", length = 1000)
    private String nota;

    protected ParametriSignificativita() {
        // richiesto da JPA
    }

    /** Converte i dati salvati nell'oggetto usato dal motore delle regole. */
    public ParametriSoglia comeSoglia() {
        return new ParametriSoglia(fatturatoAnnuo, perditaMassima, quotaFatturato,
                minutiIndisponibilitaTotale, minutiDisponibilitaLimitata, quotaUtenti, utentiMassimi);
    }

    public Long getId() { return id; }
    public int getVersione() { return versione; }
    public LocalDate getValidoDal() { return validoDal; }
    public BigDecimal getFatturatoAnnuo() { return fatturatoAnnuo; }
    public BigDecimal getPerditaMassima() { return perditaMassima; }
    public BigDecimal getQuotaFatturato() { return quotaFatturato; }
    public int getMinutiIndisponibilitaTotale() { return minutiIndisponibilitaTotale; }
    public int getMinutiDisponibilitaLimitata() { return minutiDisponibilitaLimitata; }
    public BigDecimal getQuotaUtenti() { return quotaUtenti; }
    public long getUtentiMassimi() { return utentiMassimi; }
    public String getNota() { return nota; }
}
