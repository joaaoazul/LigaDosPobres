package com.ligarecord.domain.enums;

public enum TipoBloco {
    /** Cobrada uma única vez, quando a equipa entra na liga. */
    INSCRICAO,
    /** Cobrada periodicamente, pela posição em cada jornada do bloco. */
    PERIODO,
    /**
     * Cobrada numa jornada combinada, pela posição na classificação geral —
     * o "Inverno" e o "Verão" de algumas ligas. Leva o nome da cobrança, para
     * na lista de dívidas se ler "Inverno" e não "Bloco 12".
     */
    CLASSIFICACAO
}
