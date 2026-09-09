package com.ligarecord.web.dto;

import java.util.List;
import java.util.UUID;

public record RemoverBlocosRequest(List<UUID> blocoIds) {
}
