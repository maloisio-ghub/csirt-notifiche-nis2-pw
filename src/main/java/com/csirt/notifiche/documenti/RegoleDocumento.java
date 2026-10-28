package com.csirt.notifiche.documenti;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.csirt.notifiche.model.ContenutoNotifica;
import com.csirt.notifiche.model.TipoDocumento;

/** Regole dei documenti: matrice campo x adempimento e campi mancanti per ciascun adempimento. */
public final class RegoleDocumento {

    public enum Requisito {
        OBBLIGATORIO("O"), FACOLTATIVO("F"), NON_PREVISTO("–");

        private final String simbolo;

        Requisito(String simbolo) {
            this.simbolo = simbolo;
        }

        public String simbolo() {
            return simbolo;
        }
    }

    public record Campo(String nome, String etichetta, int ordine, String riferimento,
                        Map<TipoDocumento, Requisito> requisiti) {

        public Requisito requisito(TipoDocumento tipo) {
            return requisiti.get(tipo);
        }

        public boolean previstoIn(TipoDocumento tipo) {
            return requisito(tipo) != Requisito.NON_PREVISTO;
        }
    }

    /** Nome della riga della matrice dedicata alla classificazione. */
    public static final String CLASSIFICAZIONE = "classificazione";

    private static final Requisito O = Requisito.OBBLIGATORIO;
    private static final Requisito F = Requisito.FACOLTATIVO;
    private static final Requisito N = Requisito.NON_PREVISTO;

    /** Per ogni campo: requisito in pre-notifica, notifica e relazione finale. */
    private static final List<Campo> MATRICE = List.of(
            campo(1, "referenteNome", "Referente - nome", "punto di contatto", O, O, O),
            campo(2, "referenteEmail", "Referente - e-mail", "punto di contatto", O, O, O),
            campo(3, "descrizioneSintetica", "Descrizione sintetica", "art. 23.4", O, O, O),
            campo(4, "sospettoMalevolo", "Sospetto di atto illegittimo o malevolo", "art. 23.4.a", O, O, F),
            campo(5, "impattoTransfrontaliero", "Impatto transfrontaliero", "art. 23.4.a, 23.4.d.iv", O, O, O),
            campo(6, "gravita", "Gravità", "art. 23.4.b, 23.4.d.i", N, O, O),
            campo(7, "impatto", "Impatto", "art. 23.4.b, 23.4.d.i", N, O, O),
            campo(8, "indicatoriCompromissione", "Indicatori di compromissione", "art. 23.4.b", N, F, F),
            campo(9, "descrizioneDettagliata", "Descrizione dettagliata", "art. 23.4.d.i", N, N, O),
            campo(10, "causaPrincipale", "Tipo di minaccia o causa principale", "art. 23.4.d.ii", N, N, O),
            campo(11, "misureMitigazione", "Misure di attenuazione", "art. 23.4.d.iii", N, F, O),
            campo(12, CLASSIFICAZIONE, "Classificazione TC-ACN", "tassonomia ACN", F, O, O));

    private RegoleDocumento() {
    }

    private static Campo campo(int ordine, String nome, String etichetta, String riferimento,
                               Requisito preNotifica, Requisito notifica, Requisito relazioneFinale) {
        Map<TipoDocumento, Requisito> requisiti = new EnumMap<>(TipoDocumento.class);
        requisiti.put(TipoDocumento.PRE_NOTIFICA, preNotifica);
        requisiti.put(TipoDocumento.NOTIFICA, notifica);
        requisiti.put(TipoDocumento.RELAZIONE_FINALE, relazioneFinale);
        return new Campo(nome, etichetta, ordine, riferimento, Collections.unmodifiableMap(requisiti));
    }

    public static List<Campo> matrice() {
        return MATRICE;
    }

    /** Valore di un campo del contenuto. */
    public static Object valore(ContenutoNotifica c, Campo campo) {
        return switch (campo.nome()) {
            case "referenteNome" -> c.getReferenteNome();
            case "referenteEmail" -> c.getReferenteEmail();
            case "descrizioneSintetica" -> c.getDescrizioneSintetica();
            case "sospettoMalevolo" -> c.getSospettoMalevolo();
            case "impattoTransfrontaliero" -> c.getImpattoTransfrontaliero();
            case "gravita" -> c.getGravita();
            case "impatto" -> c.getImpatto();
            case "indicatoriCompromissione" -> c.getIndicatoriCompromissione();
            case "descrizioneDettagliata" -> c.getDescrizioneDettagliata();
            case "causaPrincipale" -> c.getCausaPrincipale();
            case "misureMitigazione" -> c.getMisureMitigazione();
            default -> throw new IllegalArgumentException("Campo sconosciuto: " + campo.nome());
        };
    }

    public static boolean vuoto(Object valore) {
        return valore == null || (valore instanceof String testo && testo.isBlank());
    }

    /** Campi del contenuto mancanti. */
    public static List<String> campiMancanti(ContenutoNotifica contenuto, TipoDocumento tipo) {
        return campiMancanti(contenuto, true, tipo);
    }

    /** Campi mancanti, compresa la classificazione TC-ACN se richiesta e assente. */
    public static List<String> campiMancanti(ContenutoNotifica contenuto, boolean classificato, TipoDocumento tipo) {
        return MATRICE.stream()
                .filter(campo -> campo.requisito(tipo) == Requisito.OBBLIGATORIO)
                .filter(campo -> campo.nome().equals(CLASSIFICAZIONE) ? !classificato : vuoto(valore(contenuto, campo)))
                .map(Campo::etichetta)
                .toList();
    }

    public static String matriceMarkdown() {
        StringBuilder md = new StringBuilder("# Matrice campo × adempimento\n\n")
                .append("Generata dal codice (tabella MATRICE in RegoleDocumento). ")
                .append("O = obbligatorio, F = facoltativo, – = non previsto.\n\n")
                .append("| # | Campo");
        for (TipoDocumento tipo : TipoDocumento.values()) {
            md.append(" | ").append(tipo.getEtichetta());
        }
        md.append(" | Riferimento |\n| --- | ---");
        for (int i = 0; i < TipoDocumento.values().length; i++) {
            md.append(" | ---");
        }
        md.append(" | --- |\n");
        for (Campo campo : MATRICE) {
            md.append("| ").append(campo.ordine()).append(" | ").append(campo.etichetta());
            for (TipoDocumento tipo : TipoDocumento.values()) {
                md.append(" | ").append(campo.requisito(tipo).simbolo());
            }
            md.append(" | ").append(campo.riferimento()).append(" |\n");
        }
        return md.toString();
    }
}