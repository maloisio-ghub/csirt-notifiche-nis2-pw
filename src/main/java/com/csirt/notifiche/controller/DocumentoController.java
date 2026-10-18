package com.csirt.notifiche.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.csirt.notifiche.dto.ContenutoDto;
import com.csirt.notifiche.dto.DocumentoDto;
import com.csirt.notifiche.dto.NuovoDocumento;
import com.csirt.notifiche.dto.TracciaDto;
import com.csirt.notifiche.service.DocumentoService;
import com.csirt.notifiche.documenti.RegoleDocumento;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @PostMapping("/incidenti/{id}/documenti")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoDto crea(@PathVariable Long id, @Valid @RequestBody NuovoDocumento richiesta) {
        return service.crea(id, richiesta.tipo());
    }

    @GetMapping("/incidenti/{id}/documenti")
    public List<DocumentoDto> elenco(@PathVariable Long id) {
        return service.elenco(id);
    }

    @GetMapping("/documenti/{id}")
    public DocumentoDto dettaglio(@PathVariable Long id) {
        return service.dettaglio(id);
    }

    @PutMapping("/documenti/{id}")
    public DocumentoDto aggiorna(@PathVariable Long id, @Valid @RequestBody ContenutoDto contenuto) {
        return service.aggiorna(id, contenuto);
    }

    @PostMapping("/documenti/{id}/trasmissione")
    public DocumentoDto trasmetti(@PathVariable Long id) {
        return service.trasmetti(id);
    }

    @GetMapping("/documenti/matrice")
    public List<RegoleDocumento.Campo> matrice() {
        return RegoleDocumento.matrice();
    }

    @GetMapping(value = "/documenti/matrice.md", produces = "text/markdown;charset=UTF-8")
    public String matriceMarkdown() {
        return RegoleDocumento.matriceMarkdown();
    }

    @GetMapping("/incidenti/{id}/traccia")
    public TracciaDto traccia(@PathVariable Long id) {
        return service.traccia(id);
    }
}