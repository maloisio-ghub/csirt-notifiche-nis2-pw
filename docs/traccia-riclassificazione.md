# Traccia di esecuzione – riclassificazione tardiva

Generata dal test `ScenarioRiclassificazioneTardivaTest`; orari in ora italiana. Incidente 1: console di orchestrazione SD-WAN irraggiungibile (servizio S1).

## Classificazioni

| Versione | Valori TC-ACN | Momento | Motivazione | Attiva |
| --- | --- | --- | --- | --- |
| v1 | BC:IM_AV, BC:RO_SY, BC:SE_ME, TT:AV_OU | 28/10/2026 22:36:16 | – | no |
| v2 | AC:VE_EV, BC:IM_AV, BC:IM_SY, BC:RO_MA, BC:SE_HI, TT:VU_N- | 28/10/2026 22:36:16 | Analisi forense del 21/10: l'indisponibilità è stata causata dallo sfruttamento di una vulnerabilità nota (n-day) della console di orchestrazione SD-WAN, non da un guasto. Gravità rivalutata ad alta. | sì |

## Prima della riclassificazione (28/10/2026 22:36:16)

Stato dell'incidente: SIGNIFICATIVO – classificazione attiva: v1

| Id | Tipo | Versione | Stato | Classificazione |
| --- | --- | --- | --- | --- |
| 1 | PRE_NOTIFICA | 1 | TRASMESSO | v1 |
| 2 | NOTIFICA | 1 | TRASMESSO | v1 |

## Dopo la riclassificazione (28/10/2026 22:36:17)

Stato dell'incidente: SIGNIFICATIVO – classificazione attiva: v2

| Id | Tipo | Versione | Stato | Classificazione |
| --- | --- | --- | --- | --- |
| 1 | PRE_NOTIFICA | 1 | TRASMESSO | v1 |
| 2 | NOTIFICA | 1 | TRASMESSO | v1 |

Verifica: l'elenco dei documenti prima e dopo è identico (stessi id, versioni, stato e classificazione fissata).

## Relazione finale

- **Classificazione precedente** (v1): BC:IM_AV, BC:RO_SY, BC:SE_ME, TT:AV_OU
- **Classificazione nuova** (v2): AC:VE_EV, BC:IM_AV, BC:IM_SY, BC:RO_MA, BC:SE_HI, TT:VU_N-
- **Momento del cambiamento**: 28/10/2026 22:36:16
- **Motivazione**: Analisi forense del 21/10: l'indisponibilità è stata causata dallo sfruttamento di una vulnerabilità nota (n-day) della console di orchestrazione SD-WAN, non da un guasto. Gravità rivalutata ad alta.

Relazione finale (documento 3, versione 1) trasmessa il 28/10/2026 22:36:17 con classificazione v2.
