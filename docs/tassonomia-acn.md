# Tassonomia Cyber dell'ACN (TC-ACN)

## 1. Fonte

| Dato | Valore |
| --- | --- |
| Documento | La Tassonomia Cyber dell'ACN – versione 2.0 (TLP:CLEAR) |
| URL | https://www.acn.gov.it/portale/documents/20119/552690/ACN_Tassonomia_Cyber_CLEAR.pdf |
| Data di pubblicazione | 2025-11-03 (metadati del PDF) |
| Data di acquisizione | 2026-10-17 |
| SHA-256 del PDF | `13ba8bc27aaa56737f823392a3246461d5393f1b4f325e8109506267451e682b` |
| Copia nel repository | `docs/fonti/ACN_Tassonomia_Cyber_CLEAR.pdf` |

L'impronta SHA-256 identifica in modo univoco la copia del documento utilizzata: chiunque può ricalcolarla
(`shasum -a 256 docs/fonti/ACN_Tassonomia_Cyber_CLEAR.pdf`) e verificare che i dati caricati derivino
esattamente da quel file. Gli stessi metadati (URL, date, impronta) sono registrati nel database, nella
tabella `versione_tassonomia`, e sono esposti dall'API `GET /api/tassonomia`.

Alla data di acquisizione la versione 2.0 è quella pubblicata sul portale dell'ACN. Prima della consegna
il sito va ricontrollato: se fosse pubblicata una versione successiva, si carica con la procedura descritta
al paragrafo 2, senza modifiche al codice.

## 2. Procedura di caricamento

La tassonomia non è scritta nel codice: è un insieme di dati versionati, caricati all'avvio
dell'applicazione. La procedura si articola in quattro fasi.

1. **Estrazione del testo.** Il PDF ufficiale viene convertito in testo mantenendo l'impaginazione:
   `pdftotext -layout docs/fonti/ACN_Tassonomia_Cyber_CLEAR.pdf /tmp/tc-acn.txt`.
2. **Generazione del CSV.** Lo script `tools/estrai_tc_acn.py` legge i titoli del capitolo 3 del documento,
   che per ogni voce riportano nome e codice ufficiale (ad esempio `3.1.1.1 Account Compromise (BC:IM_AC)`),
   e produce il file `src/main/resources/tassonomia/tc-acn-2.0.csv` con una riga per voce: tipo
   (macrocategoria, predicato, valore), codice, nome, codice del padre, selezione multipla, gruppo e
   descrizione. La descrizione è il testo che segue il titolo nel documento. Lo script è deterministico:
   rieseguito sullo stesso PDF produce lo stesso CSV.
3. **Manifest delle versioni.** Il file `src/main/resources/tassonomia/versioni.csv` elenca le versioni
   disponibili con codice, titolo, URL, data di pubblicazione, data di acquisizione, impronta del PDF, file
   CSV di riferimento e flag `attiva`.
4. **Validazione e caricamento all'avvio.** All'avvio il componente `CaricamentoTassonomia` legge il
   manifest e, per ogni versione non ancora presente nel database, valida il CSV con
   `DefinizioneTassonomia` prima di scrivere qualsiasi dato. I controlli sono: intestazione corretta,
   codici univoci e nel formato atteso, ogni voce collegata a un padre del livello superiore già definito,
   codice del figlio coerente con quello del padre (`BC` → `BC:IM` → `BC:IM_AV`). Se un controllo fallisce
   l'applicazione non si avvia e l'errore indica il numero di riga: è preferibile fermarsi piuttosto che
   classificare incidenti con una tassonomia incompleta o incoerente.

**Aggiunta di una nuova versione.** Quando l'ACN pubblica un aggiornamento: si salva il nuovo PDF in
`docs/fonti/`, si genera il nuovo CSV con lo script, si aggiunge una riga al manifest con `attiva=S`. Al
riavvio successivo l'applicazione carica la nuova versione e disattiva la precedente. Nessuna riga di codice
cambia, e le classificazioni già registrate restano collegate alla versione con cui sono state fatte.

## 3. Conteggi

Il documento ufficiale dichiara (capitolo 3, introduzione): «La TC-ACN è costituita
da 219 "valori", ognuno appartenente a uno dei 21 "predicati", i quali sono
organizzati in 4 "macrocategorie".»

La query di conteggio eseguita dall'applicazione (`VoceTassonomiaRepository.contaPerTipo`) è la seguente:

```sql
SELECT v.tipo AS tipo, COUNT(*) AS numero
FROM voce_tassonomia v
JOIN versione_tassonomia t ON t.id = v.versione_id
WHERE t.codice = :codice
GROUP BY v.tipo
ORDER BY v.tipo
```

Output per la versione `TC-ACN-2.0` (`GET /api/tassonomia/versioni/TC-ACN-2.0/conteggi`):

```json
{"versione":"TC-ACN-2.0",
 "perTipo":[{"tipo":"MACROCATEGORIA","numero":4},
            {"tipo":"PREDICATO","numero":21},
            {"tipo":"VALORE","numero":219}],
 "perMacrocategoria":[{"macrocategoria":"BC","predicati":4,"valori":26},
                      {"macrocategoria":"TT","predicati":9,"valori":62},
                      {"macrocategoria":"TA","predicati":2,"valori":10},
                      {"macrocategoria":"AC","predicati":6,"valori":121}]}
```

| Livello | Caricati nel database | Documento ACN |
| --- | --- | --- |
| Macrocategorie | 4 | 4 |
| Predicati (categorie) | 21 | 21 |
| Valori (sottocategorie) | 219 | 219 |

Dettaglio per macrocategoria (query `contaPerMacrocategoria`):

| Macrocategoria | Predicati | Valori |
| --- | --- | --- |
| BC – Baseline Characterization | 4 | 26 |
| TT – Threat Type | 9 | 62 |
| TA – Threat Actor | 2 | 10 |
| AC – Additional Context | 6 | 121 |
| **Totale** | **21** | **219** |

La corrispondenza è verificata automaticamente dal test `TassonomiaIntegrazioneTest`, che esegue le stesse
query sul database caricato all'avvio: se un caricamento futuro alterasse i numeri, la build fallirebbe.

## 4. Tabella delle discrepanze

Per ogni elemento del documento ufficiale che non si traduce direttamente nel modello dati adottato, la
tabella riporta la voce di origine, l'adattamento scelto e la motivazione.

| Voce ufficiale | Discrepanza | Adattamento | Motivazione |
| --- | --- | --- | --- |
| Struttura a tre livelli: macrocategoria → predicato → valore (cap. 3, introduzione) | La traccia parla di "categorie e sottocategorie", cioè di due livelli; la TC-ACN ne prevede tre | Categoria = predicato (21), sottocategoria = valore (219); la macrocategoria (4) è conservata come livello di raggruppamento superiore. I tre livelli stanno nella tabella `voce_tassonomia`, collegati dal riferimento al padre | Conservare la struttura ufficiale rende i conteggi direttamente confrontabili con il documento ed evita di perdere informazione utile alla classificazione |
| 19 sotto-predicati del predicato Involved Asset (AC:IN, § 3.4.3), ad esempio "Hardware – OT" | Il documento li introduce "a causa della numerosità dei valori" per facilitarne la lettura, ma non assegna loro un codice | Registrati come attributo `gruppo` dei 98 valori di AC:IN, non come livello della gerarchia | Trattarli come livello aggiungerebbe 19 voci non previste e altererebbe i conteggi ufficiali; come attributo restano disponibili per raggruppare i valori nell'interfaccia |
| Codici TT:VU_0- (0-Day Vulnerability) e TT:VU_N- (n-Day Vulnerability), § 3.2.9.1 e 3.2.9.2 | Terminano con un trattino, a differenza degli altri valori, il cui suffisso è formato da lettere | Caricati identici al documento; il formato validato ammette il trattino | Il codice è l'identificativo ufficiale della voce: normalizzarlo (ad esempio in TT:VU_0D) creerebbe codici inesistenti nella tassonomia e non riconducibili alla fonte |
| Codici dei valori di Involved Asset, ad esempio AC:IN_HW-OT_PLC | Il suffisso contiene trattini e un secondo underscore, quindi ha una struttura più articolata degli altri codici | Caricati identici; il formato validato ammette lettere, cifre, `-` e `_` dopo il prefisso del predicato | Come per i codici precedenti, fedeltà all'identificativo ufficiale |
| Cardinalità dei predicati | Il documento non indica se per un incidente un predicato ammetta uno o più valori | Scelta progettuale (colonna `selezione_multipla`): selezione singola per Severity (BC:SE) e Outlook (AC:OU), multipla per gli altri 19 predicati | Un incidente ha una sola gravità e una sola tendenza, mentre può avere più impatti, più asset coinvolti o più vettori. La scelta è dichiarata nello script di estrazione e modificabile nel CSV |
| Lingua di nomi e descrizioni | I nomi delle voci sono in inglese, le descrizioni in italiano | Mantenuti come nel documento, senza traduzioni | Fedeltà alla fonte; i nomi inglesi coincidono con quelli usati negli scambi con il CSIRT |
| Macrocategorie (§ 3.1, 3.2, 3.3, 3.4) | Nel capitolo 3 il titolo di ogni macrocategoria è seguito direttamente dal primo predicato, senza un testo descrittivo proprio | Descrizione lasciata vuota | Non si introducono contenuti assenti dalla fonte ufficiale |
| Appendice (pag. 63 e seguenti) | Riporta l'elenco completo della tassonomia in forma grafica, ripetendo le voci del capitolo 3 | Non utilizzata per il caricamento: l'unica fonte è il capitolo 3 | Una sola fonte evita duplicati; il capitolo 3 è l'unica parte che riporta per ogni voce codice, nome e descrizione in forma testuale |