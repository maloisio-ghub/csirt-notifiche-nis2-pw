package com.csirt.notifiche.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.csirt.notifiche.documenti.RegoleDocumento;
import com.csirt.notifiche.documenti.Riclassificazioni.Riclassificazione;
import com.csirt.notifiche.dto.DocumentoDto;
import com.csirt.notifiche.model.ContenutoNotifica;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.scadenze.CalcolatoreScadenze;
import com.csirt.notifiche.scadenze.Scadenza;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Versione stampabile dei documenti di notifica. I campi e il loro ordine vengono dalla matrice
 * (RegoleDocumento): il PDF mostra solo i campi previsti per l'adempimento, con * sugli obbligatori.
 */
@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(CalcolatoreScadenze.FUSO_ORARIO);
    private static final Color GRIGIO = new Color(230, 230, 230);
    private static final Font TITOLO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SEZIONE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font ETICHETTA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMALE = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font PICCOLO = FontFactory.getFont(FontFactory.HELVETICA, 7);

    public byte[] documento(DocumentoDto d, Incidente incidente, Scadenza scadenza) {
        ByteArrayOutputStream uscita = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 50, 50, 50, 50);
        boolean trasmesso = d.stato() == StatoDocumento.TRASMESSO;
        try {
            PdfWriter.getInstance(pdf, uscita);
            pdf.open();

            Paragraph titolo = new Paragraph(d.tipo().getEtichetta() + " al CSIRT Italia", TITOLO);
            titolo.setAlignment(Element.ALIGN_CENTER);
            pdf.add(titolo);
            Paragraph sottotitolo = new Paragraph("Incidente " + incidente.getId() + " - versione " + d.versione()
                    + " - " + (trasmesso ? "TRASMESSO" : "BOZZA (documento non trasmesso)"), SEZIONE);
            sottotitolo.setAlignment(Element.ALIGN_CENTER);
            sottotitolo.setSpacingAfter(12);
            pdf.add(sottotitolo);

            pdf.add(new Paragraph("Incidente: " + incidente.getTitolo(), NORMALE));
            pdf.add(new Paragraph("Rilevato il " + data(incidente.getDataRilevamento())
                    + " - significatività accertata il " + data(incidente.getDataConoscenza()), NORMALE));
            if (scadenza != null) {
                pdf.add(new Paragraph("Termine (" + d.tipo().getTermine() + ", " + d.tipo().getRiferimento()
                        + " Dir. UE 2022/2555): " + data(scadenza.termine()) + " - stato: " + scadenza.stato(), NORMALE));
            }
            if (trasmesso) {
                pdf.add(new Paragraph("Trasmesso il " + data(d.dataTrasmissione()), NORMALE));
            }

            PdfPTable tabella = new PdfPTable(new float[] {2.2f, 5f});
            tabella.setWidthPercentage(100);
            tabella.setSpacingBefore(10);
            tabella.addCell(cella("Campo", ETICHETTA, GRIGIO));
            tabella.addCell(cella("Valore", ETICHETTA, GRIGIO));
            ContenutoNotifica contenuto = d.contenuto().comeContenuto();
            for (RegoleDocumento.Campo campo : RegoleDocumento.matrice()) {
                if (!campo.previstoIn(d.tipo())) {
                    continue;
                }
                boolean obbligatorio = campo.requisito(d.tipo()) == RegoleDocumento.Requisito.OBBLIGATORIO;
                String valore = campo.nome().equals(RegoleDocumento.CLASSIFICAZIONE)
                        ? classificazione(d)
                        : formatta(RegoleDocumento.valore(contenuto, campo));
                tabella.addCell(cella(campo.etichetta() + (obbligatorio ? " *" : ""), ETICHETTA, null));
                tabella.addCell(cella(valore, NORMALE, null));
            }
            pdf.add(tabella);
            pdf.add(new Paragraph("* campo obbligatorio per questo adempimento", PICCOLO));

            if (!d.riclassificazioni().isEmpty()) {
                Paragraph sezione = new Paragraph("Riclassificazioni successive alla notifica", SEZIONE);
                sezione.setSpacingBefore(12);
                pdf.add(sezione);
                PdfPTable cambi = new PdfPTable(new float[] {2.5f, 2.5f, 1.4f, 3.2f});
                cambi.setWidthPercentage(100);
                cambi.setSpacingBefore(6);
                for (String intestazione : new String[] {"Classificazione precedente", "Classificazione nuova",
                        "Momento", "Motivazione"}) {
                    cambi.addCell(cella(intestazione, ETICHETTA, GRIGIO));
                }
                for (Riclassificazione r : d.riclassificazioni()) {
                    cambi.addCell(cella("v" + r.versionePrecedente() + ": " + r.classificazionePrecedente(), NORMALE, null));
                    cambi.addCell(cella("v" + r.versioneNuova() + ": " + r.classificazioneNuova(), NORMALE, null));
                    cambi.addCell(cella(data(r.momento()), NORMALE, null));
                    cambi.addCell(cella(r.motivazione(), NORMALE, null));
                }
                pdf.add(cambi);
            }

            Paragraph piede = new Paragraph(trasmesso
                    ? "Documento trasmesso: non più modificabile."
                    : "Documento in bozza: non ancora trasmesso.", PICCOLO);
            piede.setSpacingBefore(14);
            pdf.add(piede);
            pdf.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Generazione del PDF non riuscita per il documento " + d.id(), e);
        }
        return uscita.toByteArray();
    }

    private static String classificazione(DocumentoDto d) {
        return d.versioneClassificazione() == null
                ? "Non ancora fissata: si fissa alla trasmissione"
                : "v" + d.versioneClassificazione() + ": " + d.classificazione();
    }

    private static String formatta(Object valore) {
        if (valore == null || (valore instanceof String testo && testo.isBlank())) {
            return "–";
        }
        if (valore instanceof Boolean b) {
            return b ? "Sì" : "No";
        }
        return valore.toString();
    }

    private static String data(Instant istante) {
        return istante == null ? "–" : DATA.format(istante);
    }

    private static PdfPCell cella(String testo, Font font, Color sfondo) {
        PdfPCell cella = new PdfPCell(new Phrase(testo, font));
        cella.setPadding(5);
        if (sfondo != null) {
            cella.setBackgroundColor(sfondo);
        }
        return cella;
    }
}