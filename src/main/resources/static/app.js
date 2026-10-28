'use strict';

/*
 * Interfaccia web delle notifiche NIS2.
 * Soglie, campi obbligatori e matrice arrivano dal server.
 */

const ADEMPIMENTI = [
    { tipo: 'PRE_NOTIFICA', etichetta: 'Pre-notifica', termine: 'entro 24 ore dalla conoscenza' },
    { tipo: 'NOTIFICA', etichetta: 'Notifica dell\'incidente', termine: 'entro 72 ore dalla conoscenza' },
    { tipo: 'RELAZIONE_FINALE', etichetta: 'Relazione finale', termine: 'entro un mese dalla notifica' },
];

/** Tipo di controllo per i campi del contenuto. */
const CONTROLLI = {
    referenteNome: 'testo',
    referenteEmail: 'email',
    sospettoMalevolo: 'booleano',
    impattoTransfrontaliero: 'booleano',
    gravita: 'gravita',
};
const GRAVITA = ['BASSA', 'MEDIA', 'ALTA', 'CRITICA'];

const stato = {
    servizi: [],
    matrice: [],
    tassonomia: null,
    incidenti: [],
    correnteId: null,
};

// ---------------------------------------------------------------- utilità

class ErroreApi extends Error {
    constructor(status, problema) {
        super((problema && (problema.detail || problema.title)) || `Errore HTTP ${status}`);
        this.status = status;
        this.problema = problema || {};
    }
}

async function api(metodo, percorso, corpo) {
    const opzioni = { method: metodo, headers: {} };
    if (corpo !== undefined) {
        opzioni.headers['Content-Type'] = 'application/json';
        opzioni.body = JSON.stringify(corpo);
    }
    const risposta = await fetch('/api' + percorso, opzioni);
    const testo = await risposta.text();
    let dati = null;
    if (testo) {
        try { dati = JSON.parse(testo); } catch { dati = { detail: testo }; }
    }
    if (!risposta.ok) {
        throw new ErroreApi(risposta.status, dati);
    }
    return dati;
}


function el(tag, attributi = {}, ...figli) {
    const elemento = document.createElement(tag);
    for (const [nome, valore] of Object.entries(attributi)) {
        if (valore === null || valore === undefined || valore === false) continue;
        if (nome === 'class') elemento.className = valore;
        else if (nome.startsWith('on')) elemento.addEventListener(nome.slice(2), valore);
        else if (valore === true) elemento.setAttribute(nome, '');
        else elemento.setAttribute(nome, valore);
    }
    for (const figlio of figli.flat()) {
        if (figlio === null || figlio === undefined || figlio === false) continue;
        elemento.append(figlio instanceof Node ? figlio : String(figlio));
    }
    return elemento;
}

function data(iso) {
    if (!iso) return '–';
    return new Date(iso).toLocaleString('it-IT', {
        timeZone: 'Europe/Rome', day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
}

function durata(minuti) {
    if (minuti === null || minuti === undefined) return '';
    const assoluti = Math.abs(minuti);
    const giorni = Math.floor(assoluti / 1440);
    const ore = Math.floor((assoluti % 1440) / 60);
    const min = assoluti % 60;
    const parti = [];
    if (giorni) parti.push(`${giorni} g`);
    if (ore) parti.push(`${ore} h`);
    parti.push(`${min} min`);
    return parti.join(' ');
}

function distintivo(testo) {
    return el('span', { class: `distintivo ${testo}` }, testo.replaceAll('_', ' '));
}

function numeroOppureNull(valore) {
    if (valore === '' || valore === null || valore === undefined) return null;
    const numero = Number(valore);
    return Number.isNaN(numero) ? null : numero;
}


function messaggio(contenitore, testo, tipo = 'errore', voci = []) {
    contenitore.replaceChildren(el('div', { class: `messaggio ${tipo}` },
        testo,
        voci.length ? el('ul', {}, voci.map((v) => el('li', {}, v))) : null));
}

function mostraErrore(contenitore, errore) {
    const voci = [];
    const problema = errore.problema || {};
    if (problema.errori) {
        for (const [campo, testo] of Object.entries(problema.errori)) voci.push(`${campo}: ${testo}`);
    }
    if (problema.campiMancanti) {
        messaggio(contenitore, 'Documento incompleto: compilare i campi obbligatori evidenziati.', 'errore',
            problema.campiMancanti);
        return;
    }
    messaggio(contenitore, errore.message, 'errore', voci);
}

// ---------------------------------------------------------------- avvio

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('form-incidente').addEventListener('submit', registraIncidente);
    try {
        const [servizi, matrice, tassonomia] = await Promise.all([
            api('GET', '/servizi'), api('GET', '/documenti/matrice'), api('GET', '/tassonomia'),
        ]);
        stato.servizi = servizi;
        stato.matrice = matrice;
        stato.tassonomia = tassonomia;
        document.getElementById('versione-tassonomia').textContent =
            `Tassonomia ${tassonomia.versione.codice} · acquisita il ${tassonomia.versione.dataAcquisizione}`;
        await aggiornaElenco();
        const daIndirizzo = Number(location.hash.replace('#incidente-', ''));
        if (daIndirizzo) await apriIncidente(daIndirizzo);
    } catch (errore) {
        messaggio(document.getElementById('dettaglio'), 'Impossibile contattare il server: ' + errore.message);
    }
});

// ---------------------------------------------------------------- elenco e registrazione

async function aggiornaElenco() {
    stato.incidenti = await api('GET', '/incidenti');
    const elenco = document.getElementById('elenco-incidenti');
    if (!stato.incidenti.length) {
        elenco.replaceChildren(el('li', { class: 'nota' }, 'Nessun incidente registrato.'));
        return;
    }
    elenco.replaceChildren(...stato.incidenti.map((inc) => el('li',
        { class: inc.id === stato.correnteId ? 'attivo' : null, onclick: () => apriIncidente(inc.id) },
        el('span', { class: 'titolo-incidente' }, `#${inc.id} ${inc.titolo}`),
        distintivo(inc.stato), ' ',
        el('small', {}, `rilevato ${data(inc.dataRilevamento)}`))));
}

async function registraIncidente(evento) {
    evento.preventDefault();
    const modulo = evento.target;
    const box = document.getElementById('messaggio-incidente');
    const valori = new FormData(modulo);
    try {
        const nuovo = await api('POST', '/incidenti', {
            titolo: valori.get('titolo'),
            descrizione: valori.get('descrizione') || null,
            dataRilevamento: valori.get('dataRilevamento') ? new Date(valori.get('dataRilevamento')).toISOString() : null,
        });
        modulo.reset();
        messaggio(box, `Incidente #${nuovo.id} registrato.`, 'successo');
        await apriIncidente(nuovo.id);
    } catch (errore) {
        mostraErrore(box, errore);
    }
}

// ---------------------------------------------------------------- dettaglio

async function apriIncidente(id) {
    stato.correnteId = id;
    location.hash = `incidente-${id}`;
    const dettaglio = document.getElementById('dettaglio');
    try {
        const [incidente, scadenze, valutazioni, classificazioni, documenti] = await Promise.all([
            api('GET', `/incidenti/${id}`),
            api('GET', `/incidenti/${id}/scadenze`),
            api('GET', `/incidenti/${id}/valutazioni`),
            api('GET', `/incidenti/${id}/classificazioni`),
            api('GET', `/incidenti/${id}/documenti`),
        ]);
        const attiva = classificazioni.find((c) => c.attiva) || null;
        dettaglio.replaceChildren(
            sezioneRiepilogo(incidente, scadenze),
            sezioneValutazione(incidente, valutazioni),
            sezioneClassificazione(incidente, classificazioni, attiva),
            sezioneAdempimenti(incidente, documenti, scadenze, attiva),
            sezioneTraccia(incidente));
        await aggiornaElenco();
    } catch (errore) {
        dettaglio.replaceChildren(el('div', { class: 'pannello' }));
        mostraErrore(dettaglio.firstChild, errore);
    }
}

function sezioneRiepilogo(inc, scadenze) {
    return el('section', { class: 'pannello' },
        el('div', { class: 'intestazione-incidente' },
            el('div', {},
                el('h2', {}, `#${inc.id} ${inc.titolo}`),
                inc.descrizione ? el('p', {}, inc.descrizione) : null,
                el('p', { class: 'dati' },
                    `Rilevato il ${data(inc.dataRilevamento)} · registrato il ${data(inc.dataRegistrazione)}`,
                    inc.dataConoscenza ? ` · significatività accertata il ${data(inc.dataConoscenza)}` : '')),
            distintivo(inc.stato)),
        el('div', { class: 'scadenze' }, scadenze.map(schedaScadenza)));
}

function schedaScadenza(s) {
    const adempimento = ADEMPIMENTI.find((a) => a.tipo === s.tipo);
    let dettaglio;
    switch (s.stato) {
        case 'NON_ATTIVA': dettaglio = 'Nessun obbligo: incidente non significativo.'; break;
        case 'IN_ATTESA': dettaglio = 'Decorre dalla trasmissione della notifica.'; break;
        case 'IN_CORSO': dettaglio = `Scade il ${data(s.termine)} · mancano ${durata(s.minutiResidui)}`; break;
        case 'SCADUTA': dettaglio = `Scaduta il ${data(s.termine)} · ritardo ${durata(s.minutiResidui)}`; break;
        case 'RISPETTATA': dettaglio = `Trasmessa il ${data(s.trasmissione)} (termine ${data(s.termine)})`; break;
        case 'TRASMESSA_IN_RITARDO': dettaglio = `Trasmessa il ${data(s.trasmissione)}, ${durata(s.minutiResidui)} oltre il termine`; break;
        default: dettaglio = '';
    }
    return el('div', { class: `scadenza ${s.stato}` },
        el('strong', {}, adempimento.etichetta), distintivo(s.stato), el('p', {}, dettaglio),
        el('p', { class: 'nota' }, adempimento.termine));
}

function titoloPasso(numero, testo) {
    return el('div', { class: 'passo-titolo' }, el('span', { class: 'numero' }, numero), el('h3', {}, testo));
}

// ---------------------------------------------------------------- 1. valutazione

function sezioneValutazione(inc, valutazioni) {
    const sezione = el('section', { class: 'pannello' }, titoloPasso(1, 'Valutazione di significatività'));
    const valutabile = inc.stato === 'REGISTRATO' || inc.stato === 'NON_SIGNIFICATIVO';
    if (valutabile) sezione.append(moduloValutazione(inc));
    else sezione.append(el('p', { class: 'nota' }, 'Incidente significativo: non è più rivalutabile.'));

    if (valutazioni.length) {
        sezione.append(el('h4', {}, 'Storico delle valutazioni'));
        valutazioni.forEach((v) => sezione.append(esitoValutazione(v)));
    }
    return sezione;
}

function esitoValutazione(v) {
    return el('div', { class: `esito ${v.significativo ? 'positivo' : 'negativo'}` },
        el('strong', {}, v.significativo ? 'SIGNIFICATIVO' : 'Non significativo'),
        ` · ${data(v.dataValutazione)} · parametri v${v.versioneParametri}`,
        v.motivazioni.length
            ? el('ul', {}, v.motivazioni.map((m) => el('li', {}, m)))
            : el('p', { class: 'nota' }, 'Nessuna regola soddisfatta.'));
}

function moduloValutazione(inc) {
    const box = el('div');
    const righe = stato.servizi.map((s) => el('tr', { 'data-servizio': s.codice },
        el('td', {}, el('strong', {}, s.codice), ` ${s.nome}`, el('br'), el('small', { class: 'nota' }, `base ${s.baseUtenti} · soglia ${s.sogliaUtenti} utenti`)),
        el('td', {}, el('input', { type: 'number', min: 0, name: 'minutiIndisponibilitaTotale', 'aria-label': 'Minuti di indisponibilità totale' })),
        el('td', {}, el('input', { type: 'number', min: 0, name: 'minutiDisponibilitaLimitata', 'aria-label': 'Minuti di disponibilità limitata' })),
        el('td', {}, el('input', { type: 'number', min: 0, name: 'utentiDisponibilitaLimitata', 'aria-label': 'Utenti con disponibilità limitata' })),
        el('td', {}, el('input', { type: 'checkbox', name: 'compromissioneMalevola', 'aria-label': 'Compromissione malevola' })),
        el('td', {}, el('input', { type: 'number', min: 0, name: 'utentiCompromissione', 'aria-label': 'Utenti colpiti da compromissione' }))));

    const criterio = (nome, testo) => el('label', { class: 'in-linea' }, el('input', { type: 'checkbox', name: nome }), testo);

    const modulo = el('form', { class: 'modulo', onsubmit: (e) => inviaValutazione(e, inc, box) },
        el('p', { class: 'nota' }, 'Indicare gli effetti noti. Le soglie applicate sono quelle in vigore sul server (Reg. UE 2024/2690, artt. 3 e 10).'),
        el('table', {},
            el('thead', {}, el('tr', {},
                el('th', {}, 'Servizio critico'), el('th', {}, 'Indisp. totale (min)'), el('th', {}, 'Disp. limitata (min)'),
                el('th', {}, 'Utenti disp. limitata'), el('th', {}, 'Compr. malevola'), el('th', {}, 'Utenti compromessi'))),
            el('tbody', {}, righe)),
        el('div', { class: 'griglia-due' },
            el('label', {}, 'Perdita economica stimata (€)', el('input', { type: 'number', min: 0, step: '0.01', name: 'perditaEconomica' })),
            el('div', {},
                criterio('esfiltrazioneSegretiCommerciali', 'Esfiltrazione di segreti commerciali'),
                criterio('decesso', 'Decesso di una persona'),
                criterio('dannoConsiderevoleSalute', 'Danno considerevole alla salute'),
                criterio('accessoMalevoloConGravePerturbazione', 'Accesso malevolo con possibile grave perturbazione'))),
        el('div', { class: 'pulsanti' }, el('button', { type: 'submit' }, 'Valuta significatività')));
    return el('div', {}, modulo, box);
}

async function inviaValutazione(evento, inc, box) {
    evento.preventDefault();
    const modulo = evento.target;
    const spuntato = (nome) => modulo.querySelector(`[name="${nome}"]`).checked;
    const impatti = [...modulo.querySelectorAll('tr[data-servizio]')].map((riga) => {
        const valore = (nome) => riga.querySelector(`[name="${nome}"]`);
        return {
            codiceServizio: riga.dataset.servizio,
            minutiIndisponibilitaTotale: numeroOppureNull(valore('minutiIndisponibilitaTotale').value),
            minutiDisponibilitaLimitata: numeroOppureNull(valore('minutiDisponibilitaLimitata').value),
            utentiDisponibilitaLimitata: numeroOppureNull(valore('utentiDisponibilitaLimitata').value),
            compromissioneMalevola: valore('compromissioneMalevola').checked,
            utentiCompromissione: numeroOppureNull(valore('utentiCompromissione').value),
        };
    }).filter((i) => i.minutiIndisponibilitaTotale || i.minutiDisponibilitaLimitata || i.utentiDisponibilitaLimitata
        || i.compromissioneMalevola || i.utentiCompromissione);
    try {
        await api('POST', `/incidenti/${inc.id}/valutazioni`, {
            perditaEconomica: numeroOppureNull(modulo.querySelector('[name="perditaEconomica"]').value),
            esfiltrazioneSegretiCommerciali: spuntato('esfiltrazioneSegretiCommerciali'),
            decesso: spuntato('decesso'),
            dannoConsiderevoleSalute: spuntato('dannoConsiderevoleSalute'),
            accessoMalevoloConGravePerturbazione: spuntato('accessoMalevoloConGravePerturbazione'),
            impatti,
        });
        await apriIncidente(inc.id);
    } catch (errore) {
        mostraErrore(box, errore);
    }
}

// ---------------------------------------------------------------- 2. classificazione

function sezioneClassificazione(inc, classificazioni, attiva) {
    const sezione = el('section', { class: 'pannello' }, titoloPasso(2, 'Classificazione TC-ACN'));
    if (attiva) {
        sezione.append(el('p', {}, el('strong', {}, `Classificazione attiva: v${attiva.versione}`),
            ` (${data(attiva.dataClassificazione)})`),
        el('div', { class: 'selezionati' }, attiva.valori.map((v) => el('code', { title: v.nome }, v.codice))));
    }
    if (inc.stato === 'SIGNIFICATIVO') {
        sezione.append(moduloClassificazione(inc, attiva));
    } else if (!attiva) {
        sezione.append(el('p', { class: 'nota' }, 'Disponibile quando l\'incidente risulta significativo.'));
    }
    if (classificazioni.length > 1) {
        sezione.append(el('h4', {}, 'Storico delle classificazioni'), el('table', {},
            el('thead', {}, el('tr', {}, el('th', {}, 'Versione'), el('th', {}, 'Valori'), el('th', {}, 'Momento'), el('th', {}, 'Motivazione'))),
            el('tbody', {}, classificazioni.map((c) => el('tr', {},
                el('td', {}, `v${c.versione}`, c.attiva ? ' (attiva)' : ''),
                el('td', {}, c.valori.map((v) => v.codice).join(', ')),
                el('td', {}, data(c.dataClassificazione)),
                el('td', {}, c.motivazione || '–'))))));
    }
    return sezione;
}

function moduloClassificazione(inc, attiva) {
    const voci = stato.tassonomia.voci;
    const scelti = new Set(attiva ? attiva.valori.map((v) => v.codice) : []);
    const box = el('div');
    const riepilogo = el('div', { class: 'selezionati' });

    const macrocategorie = voci.filter((v) => v.tipo === 'MACROCATEGORIA').map((macro, indice) => {
        const predicati = voci.filter((v) => v.tipo === 'PREDICATO' && v.padre === macro.codice).map((predicato) => {
            const valori = voci.filter((v) => v.tipo === 'VALORE' && v.padre === predicato.codice);
            const contenuto = [];
            let gruppoCorrente = null;
            for (const valore of valori) {
                if (valore.gruppo && valore.gruppo !== gruppoCorrente) {
                    gruppoCorrente = valore.gruppo;
                    contenuto.push(el('div', { class: 'gruppo', style: 'grid-column: 1 / -1' }, gruppoCorrente));
                }
                contenuto.push(el('label', { title: valore.descrizione || '' },
                    el('input', {
                        type: predicato.selezioneMultipla ? 'checkbox' : 'radio',
                        name: predicato.selezioneMultipla ? valore.codice : `singolo-${predicato.codice}`,
                        value: valore.codice,
                        checked: scelti.has(valore.codice),
                        onchange: aggiornaRiepilogo,
                    }),
                    el('span', {}, valore.nome, ' ', el('code', {}, valore.codice))));
            }
            if (!predicato.selezioneMultipla) {
                contenuto.unshift(el('label', {}, el('input', {
                    type: 'radio', name: `singolo-${predicato.codice}`, value: '',
                    checked: !valori.some((v) => scelti.has(v.codice)), onchange: aggiornaRiepilogo,
                }), el('span', { class: 'nota' }, 'nessuno')));
            }
            return el('div', { class: 'predicato' },
                el('h4', {}, `${predicato.nome} `, el('small', {}, `${predicato.codice} · ${predicato.selezioneMultipla ? 'più valori' : 'un solo valore'}`)),
                el('div', { class: 'valori' }, contenuto));
        });
        return el('details', { class: 'macro', open: indice === 0 },
            el('summary', {}, `${macro.codice} – ${macro.nome}`), predicati);
    });

    const motivazione = el('textarea', { name: 'motivazione', rows: 3, maxlength: 2000, required: !!attiva });
    const modulo = el('form', { class: 'modulo', onsubmit: (e) => inviaClassificazione(e, inc, box) },
        el('p', { class: 'nota' }, 'Selezionare i valori della tassonomia ACN. Passare il mouse su un valore per leggerne la descrizione.'),
        macrocategorie,
        riepilogo,
        attiva ? el('label', {}, 'Motivazione della riclassificazione (obbligatoria)', motivazione) : null,
        el('div', { class: 'pulsanti' }, el('button', { type: 'submit' }, attiva ? 'Riclassifica' : 'Salva classificazione')));

    function aggiornaRiepilogo() {
        const codici = codiciSelezionati(modulo);
        riepilogo.replaceChildren(...(codici.length
            ? [`Selezionati (${codici.length}): `, ...codici.map((c) => el('code', {}, c))]
            : [el('span', { class: 'nota' }, 'Nessun valore selezionato.')]));
    }
    aggiornaRiepilogo();
    if (!attiva) {
        return el('div', {}, modulo, box);
    }
 
    return el('details', { class: 'macro' }, el('summary', {}, 'Riclassifica l\'incidente'),
        el('div', { style: 'padding: .7rem' }, modulo, box));
}

function codiciSelezionati(modulo) {
    return [...modulo.querySelectorAll('.valori input:checked')].map((i) => i.value).filter(Boolean).sort();
}

async function inviaClassificazione(evento, inc, box) {
    evento.preventDefault();
    const modulo = evento.target;
    const campoMotivazione = modulo.querySelector('[name="motivazione"]');
    try {
        await api('POST', `/incidenti/${inc.id}/classificazioni`, {
            codici: codiciSelezionati(modulo),
            motivazione: campoMotivazione ? campoMotivazione.value.trim() || null : null,
        });
        await apriIncidente(inc.id);
    } catch (errore) {
        mostraErrore(box, errore);
    }
}

// ---------------------------------------------------------------- 3. adempimenti

function sezioneAdempimenti(inc, documenti, scadenze, attiva) {
    const sezione = el('section', { class: 'pannello' }, titoloPasso(3, 'Adempimenti verso il CSIRT Italia'),
        el('p', { class: 'nota' }, 'Ogni nuova bozza parte dai dati del documento più recente. I campi con * sono obbligatori per trasmettere; la trasmissione rende il documento immutabile.'));
    for (const adempimento of ADEMPIMENTI) {
        const delTipo = documenti.filter((d) => d.tipo === adempimento.tipo).sort((a, b) => a.versione - b.versione);
        const scadenza = scadenze.find((s) => s.tipo === adempimento.tipo);
        sezione.append(schedaAdempimento(inc, adempimento, delTipo, scadenza, attiva));
    }
    return sezione;
}

function schedaAdempimento(inc, adempimento, documenti, scadenza, attiva) {
    const box = el('div');
    const bozza = documenti.find((d) => d.stato === 'BOZZA');
    const trasmessi = documenti.filter((d) => d.stato === 'TRASMESSO');
    const scheda = el('div', { class: 'adempimento' },
        el('div', { class: 'adempimento-testata' },
            el('h4', {}, adempimento.etichetta, ' ', el('small', { class: 'nota' }, adempimento.termine)),
            scadenza ? distintivo(scadenza.stato) : null));

    trasmessi.forEach((d) => scheda.append(riepilogoTrasmesso(d)));

    if (bozza) {
        scheda.append(moduloDocumento(inc, bozza, attiva, box));
    } else if (inc.stato === 'SIGNIFICATIVO') {
        scheda.append(el('div', { class: 'pulsanti' }, el('button', {
            class: trasmessi.length ? 'secondario' : null,
            onclick: () => creaBozza(inc, adempimento.tipo, box),
        }, trasmessi.length ? `Nuova versione (v${trasmessi.length + 1})` : 'Crea bozza')));
    } else if (!trasmessi.length) {
        scheda.append(el('p', { class: 'nota' }, inc.stato === 'CHIUSO' ? 'Incidente chiuso.' : 'Disponibile dopo una valutazione positiva.'));
    }
    scheda.append(box);
    return scheda;
}

function riepilogoTrasmesso(d) {
    return el('div', { class: 'versione-trasmessa' },
        distintivo('TRASMESSO'), ` v${d.versione} · ${data(d.dataTrasmissione)}`,
        d.versioneClassificazione ? ` · classificazione v${d.versioneClassificazione}` : '',
        el('br'),
        el('br'),
        el('a', { class: 'scarica', href: `/api/documenti/${d.id}/pdf` }, 'Scarica PDF'),
        el('a', { class: 'scarica', href: `/api/documenti/${d.id}/json` }, 'Scarica JSON'),
        d.riclassificazioni && d.riclassificazioni.length ? boxRiclassificazioni(d.riclassificazioni) : null);
}

function boxRiclassificazioni(riclassificazioni) {
    return el('div', { class: 'riclassificazioni' },
        el('strong', {}, 'Riclassificazioni successive alla notifica'),
        riclassificazioni.map((r) => el('p', {},
            `v${r.versionePrecedente} (${r.classificazionePrecedente}) → v${r.versioneNuova} (${r.classificazioneNuova}) `,
            `il ${data(r.momento)}. Motivazione: ${r.motivazione}`)));
}

async function creaBozza(inc, tipo, box) {
    try {
        await api('POST', `/incidenti/${inc.id}/documenti`, { tipo });
        await apriIncidente(inc.id);
    } catch (errore) {
        mostraErrore(box, errore);
    }
}

function moduloDocumento(inc, documento, attiva, box) {
    const campi = stato.matrice.filter((c) => c.requisiti[documento.tipo] !== 'NON_PREVISTO');
    const controlli = campi.filter((c) => c.nome !== 'classificazione').map((c) => campoModulo(c, documento));
    const rigaClassificazione = campi.find((c) => c.nome === 'classificazione');

    const modulo = el('form', { class: 'modulo', onsubmit: (e) => e.preventDefault() },
        el('p', {}, distintivo('BOZZA'), ` v${documento.versione} · ultimo salvataggio ${data(documento.dataUltimaModifica)}`),
        controlli,
        rigaClassificazione ? el('p', { class: 'nota' },
            el('span', { class: rigaClassificazione.requisiti[documento.tipo] === 'OBBLIGATORIO' ? 'obbligatorio' : null }, 'Classificazione TC-ACN'),
            ': ', attiva ? `v${attiva.versione} attiva (${attiva.valori.map((v) => v.codice).join(', ')}), sarà fissata alla trasmissione` : 'non ancora assegnata (passo 2)') : null,
        documento.riclassificazioni && documento.riclassificazioni.length ? boxRiclassificazioni(documento.riclassificazioni) : null,
        el('div', { class: 'pulsanti' },
            el('button', { type: 'button', class: 'secondario', onclick: () => salvaDocumento(inc, documento, modulo, box, false) }, 'Salva bozza'),
            el('button', { type: 'button', onclick: () => salvaDocumento(inc, documento, modulo, box, true) }, 'Trasmetti al CSIRT'),
            el('a', { class: 'scarica', href: `/api/documenti/${documento.id}/pdf` }, 'Anteprima PDF')));
    return modulo;
}

function campoModulo(campo, documento) {
    const valore = documento.contenuto[campo.nome];
    const obbligatorio = campo.requisiti[documento.tipo] === 'OBBLIGATORIO';
    const tipo = CONTROLLI[campo.nome] || 'testoLungo';
    let controllo;
    if (tipo === 'booleano') {
        controllo = el('select', { 'data-campo': campo.nome },
            el('option', { value: '', selected: valore === null || valore === undefined }, '— non indicato —'),
            el('option', { value: 'true', selected: valore === true }, 'Sì'),
            el('option', { value: 'false', selected: valore === false }, 'No'));
    } else if (tipo === 'gravita') {
        controllo = el('select', { 'data-campo': campo.nome },
            el('option', { value: '', selected: !valore }, '— non indicata —'),
            GRAVITA.map((g) => el('option', { value: g, selected: valore === g }, g)));
    } else if (tipo === 'testoLungo') {
        controllo = el('textarea', { 'data-campo': campo.nome, rows: 3 }, valore || '');
    } else {
        controllo = el('input', { 'data-campo': campo.nome, type: tipo === 'email' ? 'email' : 'text', value: valore || '' });
    }
    return el('label', { class: 'campo', 'data-etichetta': campo.etichetta },
        el('span', { class: obbligatorio ? 'obbligatorio' : null }, campo.etichetta),
        el('small', { class: 'nota' }, campo.riferimento ? ` (${campo.riferimento})` : ''),
        controllo);
}

/** Il PUT sostituisce tutto il contenuto. */
function contenutoDalModulo(documento, modulo) {
    const contenuto = { ...documento.contenuto };
    modulo.querySelectorAll('[data-campo]').forEach((controllo) => {
        const nome = controllo.dataset.campo;
        if (CONTROLLI[nome] === 'booleano') {
            contenuto[nome] = controllo.value === '' ? null : controllo.value === 'true';
        } else {
            contenuto[nome] = controllo.value.trim() === '' ? null : controllo.value.trim();
        }
    });
    return contenuto;
}

async function salvaDocumento(inc, documento, modulo, box, trasmetti) {
    modulo.querySelectorAll('.campo.mancante').forEach((c) => c.classList.remove('mancante'));
    if (trasmetti && !window.confirm('Trasmettere il documento al CSIRT? Dopo la trasmissione non sarà più modificabile.')) {
        return;
    }
    try {
        await api('PUT', `/documenti/${documento.id}`, contenutoDalModulo(documento, modulo));
        if (trasmetti) {
            await api('POST', `/documenti/${documento.id}/trasmissione`);
            await apriIncidente(inc.id);
        } else {
            messaggio(box, 'Bozza salvata.', 'successo');
        }
    } catch (errore) {
        mostraErrore(box, errore);
        const mancanti = (errore.problema && errore.problema.campiMancanti) || [];
        modulo.querySelectorAll('.campo').forEach((c) => {
            if (mancanti.includes(c.dataset.etichetta)) c.classList.add('mancante');
        });
    }
}

// ---------------------------------------------------------------- traccia

function sezioneTraccia(inc) {
    const contenitore = el('div');
    return el('section', { class: 'pannello' },
        el('h3', {}, 'Traccia di esecuzione'),
        el('p', { class: 'nota' }, 'Stato attuale di classificazioni e documenti, con versioni e impronte.'),
        el('div', { class: 'pulsanti' }, el('button', { class: 'secondario', onclick: () => mostraTraccia(inc, contenitore) }, 'Mostra traccia')),
        contenitore);
}

async function mostraTraccia(inc, contenitore) {
    try {
        const t = await api('GET', `/incidenti/${inc.id}/traccia`);
        contenitore.replaceChildren(
            el('p', { class: 'dati' }, `Istantanea del ${data(t.istante)} · stato ${t.statoIncidente} · classificazione attiva ${t.classificazioneAttiva ? 'v' + t.classificazioneAttiva : '–'}`),
            el('table', {},
                el('thead', {}, el('tr', {}, ['Id', 'Tipo', 'Versione', 'Stato', 'Classificazione'].map((h) => el('th', {}, h)))),
                el('tbody', {}, t.documenti.map((d) => el('tr', {},
                    el('td', {}, d.id), el('td', {}, d.tipo), el('td', {}, d.versione), el('td', {}, d.stato),
                    el('td', {}, d.versioneClassificazione ? `v${d.versioneClassificazione}` : '–')))))));
    } catch (errore) {
        mostraErrore(contenitore, errore);
    }
}
