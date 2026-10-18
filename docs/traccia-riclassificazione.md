# Traccia di esecuzione – riclassificazione tardiva

Generata dal test `ScenarioRiclassificazioneTardivaTest` con orologio simulato; orari in ora italiana. Incidente 1: console di orchestrazione SD-WAN irraggiungibile (servizio S1).

## Classificazioni

| Versione | Valori TC-ACN | Momento | Motivazione | Attiva |
| --- | --- | --- | --- | --- |
| v1 | BC:IM_AV, BC:RO_SY, BC:SE_ME, TT:AV_OU | 16/10/2026 10:20 | – | no |
| v2 | AC:VE_EV, BC:IM_AV, BC:IM_SY, BC:RO_MA, BC:SE_HI, TT:VU_N- | 21/10/2026 13:00 | Analisi forense del 21/10: l'indisponibilità è stata causata dallo sfruttamento di una vulnerabilità nota (n-day) della console di orchestrazione SD-WAN, non da un guasto. Gravità rivalutata ad alta. | sì |

## Prima della riclassificazione (21/10/2026 12:55)

Stato dell'incidente: SIGNIFICATIVO – classificazione attiva: v1

| Id | Tipo | Versione | Stato | Classificazione | SHA-256 (inizio) | Integro |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | PRE_NOTIFICA | 1 | TRASMESSO | v1 | `570dd9fceb8d8708…` | true |
| 2 | NOTIFICA | 1 | TRASMESSO | v1 | `7d737a3e30b7a133…` | true |

## Dopo la riclassificazione (21/10/2026 13:05)

Stato dell'incidente: SIGNIFICATIVO – classificazione attiva: v2

| Id | Tipo | Versione | Stato | Classificazione | SHA-256 (inizio) | Integro |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | PRE_NOTIFICA | 1 | TRASMESSO | v1 | `570dd9fceb8d8708…` | true |
| 2 | NOTIFICA | 1 | TRASMESSO | v1 | `7d737a3e30b7a133…` | true |

Verifica: l'elenco dei documenti prima e dopo è identico (stessi id, versioni, stato, classificazione fissata e impronta).

## Relazione finale

- **Classificazione precedente** (v1): BC:IM_AV, BC:RO_SY, BC:SE_ME, TT:AV_OU
- **Classificazione nuova** (v2): AC:VE_EV, BC:IM_AV, BC:IM_SY, BC:RO_MA, BC:SE_HI, TT:VU_N-
- **Momento del cambiamento**: 21/10/2026 13:00
- **Motivazione**: Analisi forense del 21/10: l'indisponibilità è stata causata dallo sfruttamento di una vulnerabilità nota (n-day) della console di orchestrazione SD-WAN, non da un guasto. Gravità rivalutata ad alta.

Relazione finale (documento 3, versione 1) trasmessa il 12/11/2026 16:00 con classificazione v2; impronta SHA-256 `9073b1ce973c96a2f5601562851b01548cb1ca8369e05d5704cba55befe84f3f`, integra: true.
