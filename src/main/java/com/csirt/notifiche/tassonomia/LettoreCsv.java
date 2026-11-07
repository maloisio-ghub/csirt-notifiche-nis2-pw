package com.csirt.notifiche.tassonomia;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class LettoreCsv {

    private static final CSVFormat FORMATO = CSVFormat.DEFAULT.builder().setDelimiter(';').build();

    private LettoreCsv() {
    }

    public static List<List<String>> leggi(Reader sorgente) throws IOException {
        List<List<String>> righe = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(sorgente, FORMATO)) {
            for (CSVRecord riga : parser) {
                righe.add(riga.toList());
            }
        }
        return righe;
    }
}