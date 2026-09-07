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

/** Uma linha da tabela de uma {@link CobrancaPeriodo}: neste lugar paga-se isto. */
@Entity
@Table(name = "cobranca_valor")
public class CobrancaValor extends EntidadeBase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cobranca_id", nullable = false)
    private CobrancaPeriodo cobranca;

    @Column(nullable = false)
    private int posicao;

    @Column(nullable = false)
    private BigDecimal valor;

    protected CobrancaValor() {
        // exigido pelo Hibernate
    }

    public CobrancaValor(UUID id, CobrancaPeriodo cobranca, int posicao, BigDecimal valor) {
        this.id = id;
        this.cobranca = cobranca;
        this.posicao = posicao;
        this.valor = valor;
    }

    @Override
    public UUID getId() {
        return id;
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
        if (!(o instanceof CobrancaValor outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
