package com.csirt.notifiche.model;

/**
 * Livelli della TC-ACN: macrocategoria (es. BC), predicato (es. BC:IM), valore (es. BC:IM_AV).
 * La traccia parla di categorie e sottocategorie: categoria = predicato, sottocategoria = valore;
 * la macrocategoria è un livello di raggruppamento in più (vedi tabella delle discrepanze).
 */
public enum TipoVoce {
    MACROCATEGORIA,
    PREDICATO,
    VALORE
}