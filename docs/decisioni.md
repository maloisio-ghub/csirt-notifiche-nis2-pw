# Registro delle decisioni

Le principali scelte progettuali del prototipo, con il contesto, le alternative considerate e scartate e le
conseguenze. Alcune alternative sono state adottate in una prima versione e poi sostituite: in quei casi
la voce indica anche il motivo del cambiamento.

## Riepilogo

| # | Decisione | Alternative scartate |
| --- | --- | --- |
| D1 | Spring Boot a livelli con interfaccia servita dallo stesso JAR | Microservizi; frontend separato con un framework SPA |
| D2 | Database H2 in memoria | PostgreSQL già nel prototipo; H2 su file |
| D3 | Tassonomia TC-ACN caricata come dati (CSV e manifest) | Enum Java; inserimento manuale in `data.sql` |
| D4 | Riclassificazione come nuova versione, documenti trasmessi immutabili | Modifica della classificazione esistente; aggiornamento dei documenti già trasmessi; collegamento esplicito alla versione precedente |
| D5 | Matrice campo × adempimento definita in un solo punto del codice | Regole duplicate in interfaccia e PDF; annotazioni lette con la reflection |
| D6 | Soglie di significatività come dati in una sola riga | Costanti nel codice; parametri versionati |
| D7 | Immutabilità dei documenti garantita dallo stato | Impronta SHA-256 e callback JPA |
| D8 | Deploy con Dockerfile multi-stage su Render | Rilevamento automatico dell'ambiente; server virtuale gestito manualmente |

## D1 – Architettura: a livelli

**Contesto.** Il prototipo serve un solo soggetto notificante, con pochi operatori, e viene pubblicato su
un'istanza gratuita.

**Decisione.** Un'unica applicazione Spring Boot organizzata in controller, servizi e repository, con le
regole di dominio (significatività, scadenze, matrice, tassonomia) in Java puro. L'interfaccia è composta da
HTML, CSS e JavaScript senza framework, serviti dalla stessa applicazione.

**Alternative scartate.**
- *Microservizi* (valutazione, documenti, tassonomia separati): avrebbero introdotto comunicazione di rete,
  più deploy e transazioni distribuite, senza un beneficio per il carico previsto.
- *Frontend separato con un framework SPA* (React o Angular): un secondo progetto con la sua build e il suo
  deploy, più la configurazione CORS, per un'interfaccia composta da poche schermate.

**Conseguenze.** Un solo artefatto da compilare e pubblicare e una sola origine per interfaccia e API. La
separazione tra le parti è affidata ai package e al verso delle dipendenze, sempre verso il basso.

## D2 – Database H2 in memoria

**Contesto.** I dati dello scenario sono simulati e il prototipo deve poter essere avviato con un solo
comando, senza installare un database.

**Decisione.** H2 in memoria, con lo schema generato da Hibernate e i dati di riferimento caricati all'avvio.

**Alternative scartate.**
- *PostgreSQL già nel prototipo*: richiede un servizio di database separato, sia in locale sia su Render, e
  la gestione delle credenziali di accesso.
- *H2 su file*: sull'istanza gratuita di Render il filesystem non è persistente, quindi i dati si
  perderebbero comunque a ogni nuovo deploy.

**Conseguenze.** I dati inseriti si perdono a ogni riavvio (limite dichiarato nel README). L'accesso ai dati
passa da JPA, quindi il passaggio a PostgreSQL richiede di cambiare driver e configurazione, non il codice.

## D3 – Tassonomia caricata come dati

**Contesto.** La traccia richiede la tassonomia vigente, con la data di acquisizione, caricata come dati e
non scritta nel codice.

**Decisione.** Uno script Python estrae le voci dal PDF ufficiale in un CSV. Un manifest (`versioni.csv`)
registra per ogni versione URL, date e impronta SHA-256 del PDF. All'avvio i file vengono validati e caricati
nel database.

**Alternative scartate.**
- *Enum Java con i valori della tassonomia*: ogni nuova versione avrebbe richiesto di modificare e ricompilare
  il codice, contro il requisito.
- *Inserimento manuale in `data.sql`*: 244 voci da trascrivere a mano, con alto rischio di errori e senza un
  collegamento verificabile alla fonte.

**Conseguenze.** Una nuova versione si aggiunge con un CSV e una riga nel manifest; le classificazioni restano
legate alla versione con cui sono state fatte.

## D4 – Riclassificazione e immutabilità dei documenti

**Contesto.** Dopo la notifica delle 72 ore la classificazione può cambiare; i documenti già inviati non
devono cambiare e la relazione finale deve riportare il cambiamento.

**Decisione.** Ogni cambiamento crea una nuova versione numerata della classificazione, con momento e
motivazione. Alla trasmissione il documento fissa la versione in vigore e non è più modificabile. La relazione
finale elenca i cambiamenti successivi alla notifica, confrontando ogni versione con quella che la precede.

**Alternative scartate.**
- *Modificare la classificazione esistente*: si perderebbe lo storico richiesto dalla traccia.
- *Aggiornare anche i documenti già trasmessi*: renderebbe i documenti diversi da quanto inviato al CSIRT.
- *Collegare ogni versione alla precedente con una chiave esterna* (adottato nella prima versione): un dato
  ridondante, perché le versioni sono numerate in sequenza e la precedente della versione *n* è la *n − 1*.

**Conseguenze.** Lo stato prima e dopo la riclassificazione è sempre ricostruibile, come mostra la
[traccia di esecuzione](traccia-riclassificazione.md).

## D5 – Matrice dei campi in un solo punto

**Contesto.** I tre adempimenti hanno campi obbligatori diversi, che servono alla validazione, al PDF,
all'interfaccia e alla documentazione.

**Decisione.** Una tabella in `RegoleDocumento` indica, per ogni campo, se è obbligatorio, facoltativo o non
previsto in ciascun adempimento. La usano la trasmissione (errore 422 con i campi mancanti), il PDF e
l'interfaccia, che la riceve da `/api/documenti/matrice`. Un test verifica che `matrice-campi.md` coincida con
quella generata dal codice.

**Alternative scartate.**
- *Regole ripetute nel JavaScript e nel generatore del PDF*: le copie avrebbero potuto divergere senza che
  nessun test se ne accorgesse.
- *Annotazioni personalizzate sui campi dell'entità, lette con la reflection* (adottato nella prima versione):
  più compatto, ma meno leggibile e più difficile da verificare; sostituito da una tabella esplicita.

**Conseguenze.** Un nuovo campo richiede la colonna nell'entità, una riga nella tabella e la rigenerazione di
`matrice-campi.md`.

## D6 – Soglie di significatività come dati

**Contesto.** Le soglie del Regolamento (UE) 2024/2690 dipendono anche dal fatturato del soggetto.

**Decisione.** Le soglie sono in una riga della tabella `parametri_significativita`, caricata da `data.sql`;
il motore delle regole R1–R10 le riceve come parametro.

**Alternative scartate.**
- *Costanti nel codice*: ogni variazione, ad esempio del fatturato, avrebbe richiesto una nuova versione
  dell'applicazione.
- *Parametri versionati, con ogni valutazione collegata alla versione applicata* (adottato nella prima
  versione): utile quando le soglie cambiano nel tempo, ma superfluo nel perimetro del prototipo, dove i
  parametri sono fissi.

**Conseguenze.** Per aggiornare una soglia basta modificare il dato. Lo storico dei parametri resta un
possibile sviluppo futuro.

## D7 – Integrità dei documenti trasmessi

**Contesto.** Un documento trasmesso non deve più cambiare.

**Decisione.** L'entità `Documento` accetta modifiche solo in stato di bozza; dopo la trasmissione ogni
tentativo viene rifiutato e l'API risponde 409.

**Alternative scartate.**
- *Impronta SHA-256 del contenuto calcolata alla trasmissione e controlli aggiuntivi con le callback JPA*
  (adottato nella prima versione): senza un canale reale verso il CSIRT con cui confrontare l'impronta,
  aggiungeva complessità senza un uso effettivo.

**Conseguenze.** L'immutabilità è verificata dai test; l'impronta potrà essere reintrodotta insieme
all'integrazione con i canali ufficiali di notifica.

## D8 – Deploy con Docker su Render

**Contesto.** L'applicazione deve essere raggiungibile online senza costi.

**Decisione.** Un Dockerfile multi-stage: il primo stadio compila con Maven, il secondo esegue il JAR con il
solo JRE. Render costruisce l'immagine a ogni push sul branch `main`.

**Alternative scartate.**
- *Rilevamento automatico dell'ambiente da parte di Render*: il progetto veniva riconosciuto come
  applicazione Node e la build non andava a buon fine.
- *Server virtuale gestito manualmente*: costi e manutenzione del sistema operativo non giustificati per un
  prototipo.

**Conseguenze.** Lo stesso ambiente (JDK 25) in locale e in cloud; sull'istanza gratuita il primo accesso dopo
un periodo di inattività è lento.
