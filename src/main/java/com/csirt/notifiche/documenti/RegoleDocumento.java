package com.csirt.notifiche.documenti;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import com.csirt.notifiche.documenti.Riclassificazioni.Riclassificazione;
import com.csirt.notifiche.model.CampoNotifica;
import com.csirt.notifiche.model.Classificazione;
import com.csirt.notifiche.model.ContenutoNotifica;
import com.csirt.notifiche.model.Documento;
import com.csirt.notifiche.model.TipoDocumento;

/**
 * Regole dei documenti: matrice campo x adempimento (dalle annotazioni @CampoNotifica più la
 * classificazione TC-ACN), campi mancanti, testo canonico e impronta SHA-256.
 */
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

    private static final List<Campo> MATRICE;
    private static final Map<String, Field> CAMPI = new HashMap<>();

    static {
        List<Campo> campi = new ArrayList<>();
        for (Field field : ContenutoNotifica.class.getDeclaredFields()) {
            CampoNotifica regola = field.getAnnotation(CampoNotifica.class);
            if (regola == null) {
                continue;
            }
            field.setAccessible(true);
            CAMPI.put(field.getName(), field);
            campi.add(new Campo(field.getName(), regola.etichetta(), regola.ordine(), regola.riferimento(),
                    requisiti(field.getName(), regola.obbligatorioIn(), regola.facoltativoIn())));
        }
        campi.sort(Comparator.comparingInt(Campo::ordine));
        // La classificazione TC-ACN: facoltativa nella pre-notifica, obbligatoria in notifica e relazione finale.
        campi.add(new Campo(CLASSIFICAZIONE, "Classificazione TC-ACN", campi.size() + 1, "tassonomia ACN",
                requisiti(CLASSIFICAZIONE,
                        new TipoDocumento[] {TipoDocumento.NOTIFICA, TipoDocumento.RELAZIONE_FINALE},
                        new TipoDocumento[] {TipoDocumento.PRE_NOTIFICA})));
        MATRICE = List.copyOf(campi);
    }

    private RegoleDocumento() {
    }

    private static Map<TipoDocumento, Requisito> requisiti(String nome, TipoDocumento[] obbligatorio,
                                                          TipoDocumento[] facoltativo) {
        Map<TipoDocumento, Requisito> requisiti = new EnumMap<>(TipoDocumento.class);
        for (TipoDocumento tipo : TipoDocumento.values()) {
            requisiti.put(tipo, Requisito.NON_PREVISTO);
        }
        for (TipoDocumento tipo : facoltativo) {
            requisiti.put(tipo, Requisito.FACOLTATIVO);
        }
        for (TipoDocumento tipo : obbligatorio) {
            if (requisiti.get(tipo) == Requisito.FACOLTATIVO) {
                throw new IllegalStateException("Campo " + nome + " dichiarato sia obbligatorio sia facoltativo in " + tipo);
            }
            requisiti.put(tipo, Requisito.OBBLIGATORIO);
        }
        return Collections.unmodifiableMap(requisiti);
    }

    public static List<Campo> matrice() {
        return MATRICE;
    }

    public static Object valore(ContenutoNotifica contenuto, Campo campo) {
        try {
            return CAMPI.get(campo.nome()).get(contenuto);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
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

    /** Rappresentazione stabile del documento trasmesso, base dell'impronta. */
    public static String testoCanonico(Documento documento, Instant dataTrasmissione, Classificazione classificazione,
                                       List<Riclassificazione> riclassificazioni) {
        StringBuilder testo = new StringBuilder()
                .append("incidente=").append(documento.getIncidente().getId()).append('\n')
                .append("tipo=").append(documento.getTipo()).append('\n')
                .append("versione=").append(documento.getVersione()).append('\n')
                .append("dataTrasmissione=").append(dataTrasmissione).append('\n');
        for (Campo campo : MATRICE) {
            if (campo.previstoIn(documento.getTipo()) && !campo.nome().equals(CLASSIFICAZIONE)) {
                Object valore = valore(documento.getContenuto(), campo);
                testo.append(campo.nome()).append('=')
                        .append(valore == null ? "" : valore.toString().replace("\n", "\\n")).append('\n');
            }
        }
        if (classificazione != null) {
            testo.append("classificazione=v").append(classificazione.getVersione()).append(':')
                    .append(classificazione.codiciOrdinati()).append('\n');
        }
        for (Riclassificazione r : riclassificazioni) {
            testo.append("riclassificazione=v").append(r.versionePrecedente()).append("->v").append(r.versioneNuova())
                    .append('@').append(r.momento()).append(':').append(r.motivazione().replace("\n", "\\n"))
                    .append('\n');
        }
        return testo.toString();
    }

    public static String sha256(String testo) {
        try {
            byte[] impronta = MessageDigest.getInstance("SHA-256").digest(testo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(impronta);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String matriceMarkdown() {
        StringBuilder md = new StringBuilder("# Matrice campo × adempimento\n\n")
                .append("Generata dal codice (annotazioni @CampoNotifica su ContenutoNotifica). ")
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