package com.ligarecord.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record MinhasDividasDto(List<DividaDto> equipas, BigDecimal totalGeral) {
}
