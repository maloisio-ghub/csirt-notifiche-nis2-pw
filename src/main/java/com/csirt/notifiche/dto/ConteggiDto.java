package com.csirt.notifiche.dto;

import java.util.List;

public record ConteggiDto(String versione, List<ConteggioTipo> perTipo, List<ConteggioMacrocategoria> perMacrocategoria) {
}