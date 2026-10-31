# NIS2 Notifier – Notifiche di incidente al CSIRT Italia

Web application full stack in Java per la redazione e la gestione delle notifiche di incidente significativo
da trasmettere al CSIRT Italia (direttiva NIS2, D.Lgs. 138/2024), con classificazione secondo la Tassonomia
Cyber dell'ACN.

Project Work n. 29 – CdS Informatica per le Aziende Digitali (L-31), Università Telematica Pegaso.
Autore: Matteo Aloisio.

Applicazione online: <https://csirt-notifiche-nis2-pw-maloisio.onrender.com>

## Contesto

**Alfa Managed Services S.r.l.**, fornitore fittizio di servizi gestiti e di sicurezza gestita (MSP/MSSP), con
tre servizi critici:

| Codice | Servizio | Base utenti | Soglia (5%) |
| --- | --- | --- | --- |
| S1 | Managed Network | 12.000 | 600 |
| S2 | SOC/MDR | 8.000 | 400 |
| S3 | Backup-as-a-Service | 5.000 | 250 |

I criteri di significatività (regole R1–R10) traducono gli artt. 3 e 10 del Regolamento di esecuzione (UE)
2024/2690; la soglia di perdita economica è di 500.000 € (tra 500.000 € e il 5% del fatturato di 24 M€).
Tabella delle regole, soglie e diagramma del flusso di valutazione: [docs/regole-significativita.md](docs/regole-significativita.md).

## Funzionalità

- Registrazione degli incidenti e **valutazione di significatività** con le regole R1–R10 e soglie lette dal database.
- **Scadenze** di pre-notifica (24 h), notifica (72 h) e relazione finale (un mese dalla notifica), con esito.
- **Classificazione** con la TC-ACN 2.0 caricata come dati versionati; **riclassificazione** motivata.
- **Tre adempimenti** con salvataggio in bozza, riuso dei dati, validazione differenziata (422 con i campi
  mancanti), ordine vincolato e documenti trasmessi **immutabili**.
- Relazione finale con le **riclassificazioni successive alla notifica**.
- Export in **PDF** (stampabile) e **JSON** (strutturato); **traccia di esecuzione**.
- **Interfaccia web** guidata servita dalla stessa applicazione.

## Requisiti

- JDK 25
- Nessuna installazione di Maven: si usa il Maven Wrapper incluso
- Facoltativo, solo per rigenerare la tassonomia: Python 3 e `pdftotext` (Poppler)
- Facoltativo, solo per la prova con container: Docker

## Avvio

Dalla cartella del progetto (quella che contiene `pom.xml`):

```bash
./mvnw spring-boot:run      # avvia l'applicazione
./mvnw test                 # esegue i test automatici
./mvnw clean package        # produce target/notifiche-0.0.1-SNAPSHOT.jar
```

Applicazione avviata: interfaccia web su <http://localhost:8080>, API su `http://localhost:8080/api/...`.

Il database H2 è **in memoria**: a ogni avvio viene ricreato e popolato automaticamente con servizi critici,
parametri di significatività e tassonomia; gli incidenti inseriti non vengono conservati tra un avvio e l'altro.

## Percorso dimostrativo

Lo scenario completo è percorribile dall'interfaccia senza accedere a sistemi o dati reali:

1. registrare un incidente (es. "Console SD-WAN irraggiungibile");
2. valutarlo: S1 con 31 minuti di indisponibilità totale → regola R7, incidente **significativo**;
3. classificarlo con la TC-ACN (es. `BC:IM_AV`, `BC:SE_HI`, `TT:AV_OU`);
4. creare e trasmettere pre-notifica e notifica (una trasmissione incompleta mostra i campi mancanti);
5. riclassificare con motivazione dopo la notifica;
6. creare e trasmettere la relazione finale, che riporta la riclassificazione; scaricare PDF e JSON.

Lo stesso scenario è automatizzato dal test `ScenarioRiclassificazioneTardivaTest`,
che produce [la traccia di esecuzione](docs/traccia-riclassificazione.md). Le schermate dello scenario
completato sono in `docs/screenshot/`.

## Struttura

| Percorso | Contenuto |
| --- | --- |
| `src/main/java/.../controller`, `dto` | API REST, validazione dell'input, errori RFC 9457 |
| `src/main/java/.../service` | Logica applicativa e transazioni |
| `src/main/java/.../significativita`, `scadenze`, `documenti`, `tassonomia` | Regole di dominio in Java puro |
| `src/main/java/.../model`, `repository` | Entità JPA e repository Spring Data |
| `src/main/resources/static` | Interfaccia web (HTML, CSS, JavaScript) |
| `src/main/resources/data.sql` | Servizi critici e parametri di significatività |
| `src/main/resources/tassonomia` | Manifest delle versioni e CSV della TC-ACN |
| `tools/estrai_tc_acn.py` | Script di estrazione della tassonomia dal PDF ufficiale |
| `docs/` | Documentazione (vedi sotto) |
| `Dockerfile` | Immagine per il deploy |


## Documentazione

- [Architettura](docs/architettura.md): componenti, livelli, flusso principale, sicurezza
- [Modello dati](docs/modello-dati.md): diagramma ER e data dictionary, coerenti con lo schema del database
- [Schema del database](docs/schema.sql): DDL effettivo esportato dal database
- [Regole di significatività](docs/regole-significativita.md): regole R1–R10, soglie e diagramma del flusso di valutazione
- [Tassonomia ACN](docs/tassonomia-acn.md): fonte, caricamento, conteggi, discrepanze
- [Matrice campo × adempimento](docs/matrice-campi.md): generata dal codice
- [Traccia di riclassificazione](docs/traccia-riclassificazione.md): scenario con stato prima e dopo
- [Registro delle decisioni](docs/decisioni.md): scelte progettuali e alternative scartate
- Documento ufficiale della tassonomia: `docs/fonti/ACN_Tassonomia_Cyber_CLEAR.pdf`

## API principali

| Metodo | Percorso | Descrizione |
| --- | --- | --- |
| `POST`/`GET` | `/api/incidenti` | Registra / elenca gli incidenti |
| `GET` | `/api/incidenti/{id}` | Dettaglio |
| `GET` | `/api/incidenti/{id}/scadenze` | Termini e loro stato |
| `POST`/`GET` | `/api/incidenti/{id}/valutazioni` | Valutazione di significatività / storico |
| `POST`/`GET` | `/api/incidenti/{id}/classificazioni` | Classificazione o riclassificazione / storico |
| `POST`/`GET` | `/api/incidenti/{id}/documenti` | Nuova bozza precompilata / documenti |
| `PUT` | `/api/documenti/{id}` | Salvataggio in bozza |
| `POST` | `/api/documenti/{id}/trasmissione` | Trasmissione (422 se incompleto) |
| `GET` | `/api/documenti/{id}/pdf`, `/json` | Export |
| `GET` | `/api/documenti/matrice`, `/matrice.md` | Matrice generata dal codice |
| `GET` | `/api/incidenti/{id}/traccia` | Traccia di esecuzione |
| `GET` | `/api/tassonomia`, `/api/tassonomia/versioni/{codice}/conteggi` | Tassonomia attiva, conteggi |
| `GET` | `/api/servizi`, `/api/parametri` | Dati di riferimento |

## Limiti del prototipo e sviluppi futuri

| Limite | Sviluppo previsto |
| --- | --- |
| Database in memoria: i dati inseriti si perdono a ogni riavvio | Database persistente (PostgreSQL) con migrazioni versionate dello schema |
| Nessuna autenticazione né ruoli | Spring Security con OAuth2/OIDC; ruoli di operatore e di responsabile della notifica |
| Trasmissione al CSIRT simulata | Integrazione con i canali di notifica ufficiali, se resi disponibili via API |
| Regola R6 (incidenti ricorrenti) non implementata | Analisi dello storico degli incidenti con la stessa causa nei sei mesi precedenti |
| Un solo soggetto notificante | Gestione di più clienti per i fornitori di servizi gestiti |

## Deploy

L'applicazione è pubblicata su Render: <https://csirt-notifiche-nis2-pw-maloisio.onrender.com>

**Immagine Docker.** Il `Dockerfile` è multi-stage: il primo stadio (`maven:3.9-eclipse-temurin-25`) scarica le
dipendenze e produce il JAR; il secondo (`eclipse-temurin:25-jre`) contiene solo il JRE e il JAR e avvia
l'applicazione limitando la memoria usata da Java. Prova locale:

```bash
docker build -t notifiche-nis2 .
docker run --rm -p 8080:8080 notifiche-nis2
```

**Render.** Web Service di tipo Docker collegato al repository GitHub, branch `main`, istanza gratuita,
regione Frankfurt; ogni push su `main` avvia un nuovo deploy. La porta è letta dalla variabile `PORT`
impostata da Render (`server.port=${PORT:8080}`); in locale resta 8080. Nessun segreto o variabile obbligatoria.

**Limiti dell'ambiente gratuito.** Dopo un periodo di inattività il servizio viene sospeso e il primo accesso
successivo richiede circa un minuto. Poiché il database è in memoria, a ogni riavvio o nuovo deploy gli
incidenti inseriti si perdono, mentre servizi critici, parametri e tassonomia vengono ricaricati
automaticamente. In un ambiente di produzione si userebbe un database persistente.

## Uso di strumenti di intelligenza artificiale

Nello sviluppo è stato usato un assistente basato su intelligenza artificiale come strumento di supporto:
esempi di codice, diagnosi degli errori e contributo correttivo alla stesura dei testi.
