package com.csirt.notifiche.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Classificazione TC-ACN di un incidente. Ogni cambiamento è una nuova versione numerata,
 * con momento e motivazione; le versioni precedenti restano consultabili.
 */
@Entity
@Table(name = "classificazione", uniqueConstraints = @UniqueConstraint(
        name = "uk_classificazione_versione", columnNames = {"incidente_id", "versione"}))
public class Classificazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @Column(name = "versione", nullable = false)
    private int versione;

    /** Versione della tassonomia da cui provengono i valori. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versione_tassonomia_id", nullable = false)
    private VersioneTassonomia versioneTassonomia;

    @ManyToMany
    @JoinTable(name = "classificazione_valore",
            joinColumns = @JoinColumn(name = "classificazione_id"),
            inverseJoinColumns = @JoinColumn(name = "voce_id"))
    private Set<VoceTassonomia> valori = new HashSet<>();

    @Column(name = "data_classificazione", nullable = false)
    private Instant dataClassificazione;

    /** Obbligatoria dalla versione 2 in poi: perché la classificazione è cambiata. */
    @Column(name = "motivazione", length = 2000)
    private String motivazione;

    /** Una sola classificazione attiva per incidente: l'ultima. */
    @Column(name = "attiva", nullable = false)
    private boolean attiva;

    protected Classificazione() {
        // richiesto da JPA
    }

    private Classificazione(Incidente incidente, int versione, VersioneTassonomia versioneTassonomia,
                            Set<VoceTassonomia> valori, Instant momento, String motivazione) {
        this.incidente = incidente;
        this.versione = versione;
        this.versioneTassonomia = versioneTassonomia;
        this.valori = new HashSet<>(valori);
        this.dataClassificazione = momento;
        this.motivazione = motivazione;
        this.attiva = true;
    }

    public static Classificazione iniziale(Incidente incidente, VersioneTassonomia versioneTassonomia,
                                           Set<VoceTassonomia> valori, Instant momento) {
    return new Classificazione(incidente, 1, versioneTassonomia, valori, momento, null);
    }

    /** Crea la versione successiva e disattiva questa. */
    public Classificazione riclassifica(VersioneTassonomia versioneTassonomia, Set<VoceTassonomia> nuoviValori,
                                        Instant momento, String motivazione) {
        if (!attiva) {
            throw new IllegalStateException("Solo la classificazione attiva può essere sostituita");
        }
        if (motivazione == null || motivazione.isBlank()) {
            throw new IllegalArgumentException("La riclassificazione richiede una motivazione");
        }
        if (codici(nuoviValori).equals(codiciOrdinati())) {
            throw new IllegalArgumentException("La nuova classificazione coincide con quella attuale (v" + versione + ")");
        }
        this.attiva = false;
        return new Classificazione(incidente, versione + 1, versioneTassonomia, nuoviValori, momento,
                motivazione.trim());
    }

    /** Codici ordinati, separati da virgola: forma stabile per i confronti. */
    public String codiciOrdinati() {
        return codici(valori);
    }

    private static String codici(Set<VoceTassonomia> voci) {
        return voci.stream().map(VoceTassonomia::getCodice).sorted().collect(Collectors.joining(", "));
    }

    public Long getId() { return id; }
    public Incidente getIncidente() { return incidente; }
    public int getVersione() { return versione; }
    public VersioneTassonomia getVersioneTassonomia() { return versioneTassonomia; }
    public Set<VoceTassonomia> getValori() { return valori; }
    public Instant getDataClassificazione() { return dataClassificazione; }
    public String getMotivazione() { return motivazione; }
    public boolean isAttiva() { return attiva; }
}