package com.ligarecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uma linha da tabela de valores de uma {@link RegraDivida}: quem ficar nesta
 * posição paga isto.
 *
 * <p>Existe para as ligas cuja tabela não é uma rampa regular. A da liga que
 * motivou isto sobe 0,20€ por lugar até ao 7º e 0,10€ daí em diante — não há
 * fórmula que diga aquilo, e obrigar a que houvesse era torcer a liga para
 * caber no programa em vez do contrário.
 */
@Entity
@Table(name = "escala_valor")
public class EscalaValor extends EntidadeBase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regra_id", nullable = false)
    private RegraDivida regra;

    @Column(nullable = false)
    private int posicao;

    @Column(nullable = false)
    private BigDecimal valor;

    protected EscalaValor() {
        // exigido pelo Hibernate
    }

    public EscalaValor(UUID id, RegraDivida regra, int posicao, BigDecimal valor) {
        this.id = id;
        this.regra = regra;
        this.posicao = posicao;
        this.valor = valor;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public RegraDivida getRegra() {
        return regra;
    }

    public int getPosicao() {
        return posicao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    void definirValor(BigDecimal valor) {
        this.valor = valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EscalaValor outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
