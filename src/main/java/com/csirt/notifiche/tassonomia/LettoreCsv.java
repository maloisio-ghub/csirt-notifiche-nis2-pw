package com.csirt.notifiche.tassonomia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Lettore CSV minimale: separatore ";", campi tra virgolette quando contengono ";" o virgolette
 * (le virgolette interne sono raddoppiate), righe terminate da \n o \r\n.
 */
public final class LettoreCsv {

    private LettoreCsv() {
    }

    public static List<List<String>> leggi(Reader sorgente) throws IOException {
        BufferedReader in = new BufferedReader(sorgente);
        List<List<String>> righe = new ArrayList<>();
        List<String> riga = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean traVirgolette = false;
        int c;
        while ((c = in.read()) != -1) {
            char ch = (char) c;
            if (traVirgolette) {
                if (ch == '"') {
                    in.mark(1);
                    int successivo = in.read();
                    if (successivo == '"') {
                        campo.append('"');
                    } else {
                        traVirgolette = false;
                        if (successivo != -1) {
                            in.reset();
                        }
                    }
                } else {
                    campo.append(ch);
                }
            } else if (ch == '"' && campo.length() == 0) {
                traVirgolette = true;
            } else if (ch == ';') {
                riga.add(campo.toString());
                campo.setLength(0);
            } else if (ch == '\n') {
                riga.add(campo.toString());
                campo.setLength(0);
                righe.add(riga);
                riga = new ArrayList<>();
            } else if (ch != '\r') {
                campo.append(ch);
            }
        }
        if (campo.length() > 0 || !riga.isEmpty()) {
            riga.add(campo.toString());
            righe.add(riga);
        }
        return righe;
    }
}