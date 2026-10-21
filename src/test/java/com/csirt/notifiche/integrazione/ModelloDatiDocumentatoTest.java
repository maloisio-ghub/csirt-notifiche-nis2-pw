package com.csirt.notifiche.integrazione;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Requisito 5 della traccia: le entità dell'ER devono corrispondere a quelle presenti nel DDL.
 * Il test legge lo schema reale del database (INFORMATION_SCHEMA) e lo confronta con
 * docs/modello-dati.md: tabelle del diagramma ER e colonne del data dictionary.
 * Esporta inoltre il DDL effettivo in target/schema.sql.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:modello-dati")
class ModelloDatiDocumentatoTest {

    private static final Path DOCUMENTO = Path.of("docs", "modello-dati.md");

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Diagramma ER e data dictionary coincidono con tabelle e colonne del database; DDL esportato")
    void documentazioneUgualeAlloSchema() throws SQLException, IOException {
        Map<String, Set<String>> schema = schemaDatabase();
        String documento = Files.readString(DOCUMENTO, StandardCharsets.UTF_8);

        assertEquals(schema.keySet(), tabelleDiagrammaEr(documento),
                "Le entità del diagramma ER in docs/modello-dati.md non coincidono con le tabelle del database");

        Map<String, Set<String>> dizionario = colonneDataDictionary(documento);
        assertEquals(schema.keySet(), dizionario.keySet(),
                "Le sezioni del data dictionary non coincidono con le tabelle del database");

        List<String> differenze = new ArrayList<>();
        for (String tabella : schema.keySet()) {
            Set<String> nonDocumentate = new TreeSet<>(schema.get(tabella));
            nonDocumentate.removeAll(dizionario.get(tabella));
            Set<String> inesistenti = new TreeSet<>(dizionario.get(tabella));
            inesistenti.removeAll(schema.get(tabella));
            if (!nonDocumentate.isEmpty() || !inesistenti.isEmpty()) {
                differenze.add(tabella + ": non documentate " + nonDocumentate + ", inesistenti " + inesistenti);
            }
        }
        assertTrue(differenze.isEmpty(), "Data dictionary diverso dallo schema: " + differenze);

        esportaDdl(Path.of("target", "schema.sql"));
    }

    private Map<String, Set<String>> schemaDatabase() throws SQLException {
        Map<String, Set<String>> schema = new TreeMap<>();
        try (Connection connessione = dataSource.getConnection();
             Statement st = connessione.createStatement();
             ResultSet rs = st.executeQuery("SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC'")) {
            while (rs.next()) {
                schema.computeIfAbsent(rs.getString(1).toLowerCase(Locale.ROOT), k -> new TreeSet<>())
                        .add(rs.getString(2).toLowerCase(Locale.ROOT));
            }
        }
        return schema;
    }

    private void esportaDdl(Path destinazione) throws SQLException, IOException {
        StringBuilder ddl = new StringBuilder("-- DDL generato da Hibernate ed esportato da H2 (SCRIPT NODATA)\n");
        try (Connection connessione = dataSource.getConnection();
             Statement st = connessione.createStatement();
             ResultSet rs = st.executeQuery("SCRIPT NODATA")) {
            while (rs.next()) {
                ddl.append(rs.getString(1)).append('\n');
            }
        }
        Files.createDirectories(destinazione.getParent());
        Files.writeString(destinazione, ddl.toString(), StandardCharsets.UTF_8);
    }

    /** Entità dichiarate nel blocco mermaid erDiagram (righe "    nome {"). */
    static Set<String> tabelleDiagrammaEr(String documento) {
        int inizio = documento.indexOf("erDiagram");
        int fine = documento.indexOf("```", inizio);
        Set<String> tabelle = new TreeSet<>();
        Matcher m = Pattern.compile("(?m)^\\s+([a-z_]+)\\s*\\{\\s*$").matcher(documento.substring(inizio, fine));
        while (m.find()) {
            tabelle.add(m.group(1));
        }
        return tabelle;
    }

    /** Colonne del data dictionary: sezioni "### `tabella`" seguite da righe "| `colonna` | ...". */
    static Map<String, Set<String>> colonneDataDictionary(String documento) {
        Map<String, Set<String>> dizionario = new TreeMap<>();
        String sezione = documento.substring(documento.indexOf("## Data dictionary"));
        String tabella = null;
        for (String riga : sezione.split("\n")) {
            Matcher titolo = Pattern.compile("^###\\s+`([a-z_]+)`").matcher(riga);
            if (titolo.find()) {
                tabella = titolo.group(1);
                dizionario.put(tabella, new TreeSet<>());
                continue;
            }
            Matcher colonna = Pattern.compile("^\\|\\s*`([a-z_0-9]+)`\\s*\\|").matcher(riga);
            if (tabella != null && colonna.find()) {
                dizionario.get(tabella).add(colonna.group(1));
            }
        }
        return dizionario;
    }
}
