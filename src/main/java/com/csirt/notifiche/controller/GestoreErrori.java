package com.csirt.notifiche.controller;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.csirt.notifiche.service.IncidenteNonTrovatoException;

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
}
