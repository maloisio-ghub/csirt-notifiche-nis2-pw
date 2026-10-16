package com.csirt.notifiche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servizio_critico")
public class ServizioCritico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codice", nullable = false, unique = true, length = 10)
    private String codice;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "descrizione", length = 1000)
    private String descrizione;

    /** Utenti del servizio: base per la soglia del 5% (art. 10 Reg. 2024/2690). */
    @Column(name = "base_utenti", nullable = false)
    private long baseUtenti;

    protected ServizioCritico() {
        // richiesto da JPA
    }

    public Long getId() { return id; }
    public String getCodice() { return codice; }
    public String getNome() { return nome; }
    public String getDescrizione() { return descrizione; }
    public long getBaseUtenti() { return baseUtenti; }
}
