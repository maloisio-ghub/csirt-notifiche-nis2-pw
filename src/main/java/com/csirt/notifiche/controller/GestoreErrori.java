package com.csirt.notifiche.controller;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.csirt.notifiche.service.OperazioneNonConsentitaException;
import com.csirt.notifiche.service.RichiestaNonValidaException;
import com.csirt.notifiche.service.DocumentoNonTrovatoException;
import com.csirt.notifiche.service.IncidenteNonTrovatoException;
import org.springframework.http.HttpStatusCode;
import com.csirt.notifiche.service.DocumentoIncompletoException;

@RestControllerAdvice
public class GestoreErrori {

    @ExceptionHandler(IncidenteNonTrovatoException.class)
    public ProblemDetail nonTrovato(IncidenteNonTrovatoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datiNonValidi(MethodArgumentNotValidException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dati non validi");
        Map<String, String> errori = new TreeMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(campo -> errori.put(campo.getField(), campo.getDefaultMessage()));
        problema.setProperty("errori", errori);
        return problema;
    }
        @ExceptionHandler(RichiestaNonValidaException.class)
    public ProblemDetail richiestaNonValida(RichiestaNonValidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(OperazioneNonConsentitaException.class)
    public ProblemDetail operazioneNonConsentita(OperazioneNonConsentitaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DocumentoNonTrovatoException.class)
    public ProblemDetail documentoNonTrovato(DocumentoNonTrovatoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DocumentoIncompletoException.class)
    public ProblemDetail documentoIncompleto(DocumentoIncompletoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
        problema.setTitle("Documento incompleto");
        problema.setProperty("campiMancanti", e.getCampiMancanti());
        return problema;
    }
}
