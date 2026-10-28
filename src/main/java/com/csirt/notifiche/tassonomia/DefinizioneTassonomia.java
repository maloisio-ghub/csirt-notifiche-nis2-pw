package com.csirt.notifiche.tassonomia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.csirt.notifiche.model.TipoVoce;

/** Contenuto di un file della tassonomia, con i controlli essenziali prima del caricamento. */
public final class DefinizioneTassonomia {

    public static final List<String> INTESTAZIONE =
            List.of("tipo", "codice", "nome", "padre", "multiplo", "gruppo", "descrizione");

    private final List<RigaTassonomia> righe;

    private DefinizioneTassonomia(List<RigaTassonomia> righe) {
        this.righe = List.copyOf(righe);
    }

    public static DefinizioneTassonomia da(List<List<String>> csv) {
        if (csv.isEmpty() || !csv.get(0).equals(INTESTAZIONE)) {
            throw new TassonomiaNonValidaException("Intestazione attesa: " + String.join(";", INTESTAZIONE));
        }
        Map<String, TipoVoce> definite = new HashMap<>();
        List<RigaTassonomia> righe = new ArrayList<>();
        for (int i = 1; i < csv.size(); i++) {
            int numero = i + 1;
            List<String> colonne = csv.get(i);
            if (colonne.size() != INTESTAZIONE.size()) {
                throw errore(numero, "attese " + INTESTAZIONE.size() + " colonne, trovate " + colonne.size());
            }
            TipoVoce tipo;
            try {
                tipo = TipoVoce.valueOf(colonne.get(0));
            } catch (IllegalArgumentException e) {
                throw errore(numero, "tipo sconosciuto '" + colonne.get(0) + "'");
            }
            String codice = colonne.get(1);
            String nome = colonne.get(2).trim();
            String padre = colonne.get(3);

            if (codice.isBlank() || nome.isEmpty()) {
                throw errore(numero, "codice o nome mancante");
            }
            if (definite.containsKey(codice)) {
                throw errore(numero, "codice duplicato " + codice);
            }
            // Macrocategoria senza padre; predicato sotto una macrocategoria; valore sotto un predicato.
            TipoVoce padreAtteso = switch (tipo) {
                case MACROCATEGORIA -> null;
                case PREDICATO -> TipoVoce.MACROCATEGORIA;
                case VALORE -> TipoVoce.PREDICATO;
            };
            if (padreAtteso == null ? !padre.isEmpty() : definite.get(padre) != padreAtteso) {
                throw errore(numero, "padre '" + padre + "' non valido per " + codice);
            }
            definite.put(codice, tipo);
            righe.add(new RigaTassonomia(tipo, codice, nome, padre, "S".equals(colonne.get(4)),
                    colonne.get(5), colonne.get(6)));
        }
        return new DefinizioneTassonomia(righe);
    }

    public List<RigaTassonomia> righe() {
        return righe;
    }

    public Map<TipoVoce, Long> conteggi() {
        Map<TipoVoce, Long> conteggi = new EnumMap<>(TipoVoce.class);
        righe.forEach(r -> conteggi.merge(r.tipo(), 1L, Long::sum));
        return conteggi;
    }

    private static TassonomiaNonValidaException errore(int riga, String messaggio) {
        return new TassonomiaNonValidaException("Riga " + riga + ": " + messaggio);
    }
}