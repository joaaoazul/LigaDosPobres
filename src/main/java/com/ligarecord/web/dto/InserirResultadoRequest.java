package com.ligarecord.web.dto;

import java.util.UUID;

public record InserirResultadoRequest(UUID equipaId, int pontuacao) {
}
