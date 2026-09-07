package com.ligarecord.domain.enums;

/** De onde sai o valor que uma equipa paga pela posição em que ficou. */
public enum EscalaDivida {
    /** Do cálculo: valor inicial + incremento por escalão, travado no máximo. */
    FORMULA,
    /** De uma tabela escrita à mão, uma linha por posição. */
    TABELA
}
