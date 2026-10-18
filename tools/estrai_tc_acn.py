#!/usr/bin/env python3
"""
Estrae la Tassonomia Cyber dell'ACN (TC-ACN) dal PDF ufficiale e produce il CSV caricato
dall'applicazione (src/main/resources/tassonomia/<versione>.csv).

Uso:
    pdftotext -layout ACN_Tassonomia_Cyber_CLEAR.pdf tc-acn.txt
    python3 tools/estrai_tc_acn.py tc-acn.txt src/main/resources/tassonomia/tc-acn-2.0.csv

Lo script legge i titoli del capitolo 3, che riportano per ogni voce nome e codice ufficiale:
    3.1 BASELINE CHARACTERIZATION (BC)              -> macrocategoria
    3.1.1 Predicato: Impact (IM)                     -> predicato (codice BC:IM)
    3.1.1.1 Account Compromise (BC:IM_AC)            -> valore
    Figura NN: Sotto-predicato "Hardware - OT"       -> gruppo dei valori di Involved Asset
La descrizione di ogni voce e' il testo che segue il titolo fino al titolo successivo.
I codici sono riportati esattamente come nel documento, senza correzioni.
"""

import csv
import re
import sys

MACRO = re.compile(r"^\s*3\.(\d)\s+([A-Z][A-Z ]+?)\s+\(([A-Z]{2})\)\s*$")
PREDICATO = re.compile(r"^\s*3\.\d\.\d+\s+Predicato:\s+(.+?)\s+\(([A-Z]{2})\)\s*$")
VALORE = re.compile(r"^\s*3\.\d\.\d+\.\d+\s+(.+?)\s+\(([A-Z]{2}:[A-Z]{2}_[A-Za-z0-9_-]+)\)\s*$")
SOTTOPREDICATO = re.compile(r'Figura\s+\d+:\s+Sotto-predicato\s+"(.+?)"')
RUMORE = re.compile(r"^\s*(La Tassonomia Cyber dell.ACN\s+\d+|Figura\s+\d+:.*)\s*$")

# Predicati a selezione singola: un incidente ha una sola gravita' e una sola tendenza.
# E' una scelta progettuale, non un'indicazione del documento ACN (vedi tabella delle discrepanze).
PREDICATI_SELEZIONE_SINGOLA = {"BC:SE", "AC:OU"}


def estrai(righe):
    voci = []
    corrente = None
    macro = predicato = gruppo = None
    for riga in righe:
        m_sotto = SOTTOPREDICATO.search(riga)
        if m_sotto:
            gruppo = re.sub(r"\s+[–-]\s+", " - ", m_sotto.group(1).strip())
            continue
        m = MACRO.match(riga)
        if m and "...." not in riga:
            macro = m.group(3)
            corrente = nuova(voci, "MACROCATEGORIA", macro, m.group(2).title(), "", False, "")
            predicato = gruppo = None
            continue
        m = PREDICATO.match(riga)
        if m:
            predicato = f"{macro}:{m.group(2)}"
            multiplo = predicato not in PREDICATI_SELEZIONE_SINGOLA
            corrente = nuova(voci, "PREDICATO", predicato, m.group(1), macro, multiplo, "")
            gruppo = None
            continue
        m = VALORE.match(riga)
        if m:
            codice = m.group(2)
            if not codice.startswith(predicato + "_"):
                raise ValueError(f"Il valore {codice} non appartiene al predicato {predicato}")
            corrente = nuova(voci, "VALORE", codice, m.group(1), predicato, False,
                             gruppo if predicato == "AC:IN" else "")
            continue
        if riga.strip().startswith("Il sotto-predicato"):
            # Introduzione del sotto-predicato successivo: non fa parte della voce precedente.
            corrente = None
            continue
        if corrente is not None and not RUMORE.match(riga):
            testo = riga.strip()
            if testo:
                corrente["descrizione"].append(testo)
    for voce in voci:
        voce["descrizione"] = " ".join(voce["descrizione"])[:2000]
    return voci


def nuova(voci, tipo, codice, nome, padre, multiplo, gruppo):
    voce = {"tipo": tipo, "codice": codice, "nome": nome.strip(), "padre": padre,
            "multiplo": "S" if multiplo else "N", "gruppo": gruppo, "descrizione": []}
    voci.append(voce)
    return voce


def main():
    if len(sys.argv) != 3:
        sys.exit("Uso: estrai_tc_acn.py <testo-pdftotext> <csv-di-uscita>")
    with open(sys.argv[1], encoding="utf-8") as sorgente:
        righe = sorgente.read().splitlines()
    # Si parte dal capitolo 3 (dopo il sommario) e ci si ferma all'appendice.
    inizio = next(i for i, r in enumerate(righe) if MACRO.match(r) and "...." not in r)
    fine = next((i for i in range(inizio, len(righe)) if righe[i].strip() == "APPENDICE"), len(righe))
    voci = estrai(righe[inizio:fine])
    with open(sys.argv[2], "w", encoding="utf-8", newline="") as uscita:
        scrittore = csv.writer(uscita, delimiter=";", quoting=csv.QUOTE_MINIMAL)
        scrittore.writerow(["tipo", "codice", "nome", "padre", "multiplo", "gruppo", "descrizione"])
        for v in voci:
            scrittore.writerow([v["tipo"], v["codice"], v["nome"], v["padre"], v["multiplo"], v["gruppo"],
                                v["descrizione"]])
    conteggi = {t: sum(1 for v in voci if v["tipo"] == t) for t in ("MACROCATEGORIA", "PREDICATO", "VALORE")}
    print(conteggi)


if __name__ == "__main__":
    main()
    