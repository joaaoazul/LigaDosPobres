package com.ligarecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Como uma liga cobra as suas equipas: uma inscrição única na entrada, e
 * depois um valor por período de jornadas que sobe por escalão de
 * classificação — quem vai melhor paga menos. Cada liga tem a sua própria
 * regra, porque o valor e os escalões variam de liga para liga.
 *
 * <p>Uma liga sem regra definida não tem cobrança automática nenhuma: o
 * gestor continua a fechar blocos e a cobrar a inscrição à mão.
 */
@Entity
@Table(name = "regra_divida")
public class RegraDivida extends EntidadeBase {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liga_id", nullable = false, unique = true)
    private Liga liga;

    @Column(name = "valor_inscricao", nullable = false)
    private BigDecimal valorInscricao;

    @Column(name = "valor_inicial", nullable = false)
    private BigDecimal valorInicial;

    @Column(nullable = false)
    private BigDecimal incremento;

    @Column(name = "equipas_por_escalao", nullable = false)
    private int equipasPorEscalao;

    @Column(name = "valor_maximo", nullable = false)
    private BigDecimal valorMaximo;

    @Column(name = "jornadas_por_bloco", nullable = false)
    private int jornadasPorBloco;

    protected RegraDivida() {
        // exigido pelo Hibernate
    }

    public RegraDivida(UUID id, Liga liga, BigDecimal valorInscricao, BigDecimal valorInicial,
                       BigDecimal incremento, int equipasPorEscalao, BigDecimal valorMaximo,
                       int jornadasPorBloco) {
        this.id = id;
        this.liga = liga;
        this.valorInscricao = valorInscricao;
        this.valorInicial = valorInicial;
        this.incremento = incremento;
        this.equipasPorEscalao = equipasPorEscalao;
        this.valorMaximo = valorMaximo;
        this.jornadasPorBloco = jornadasPorBloco;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Liga getLiga() {
        return liga;
    }

    public BigDecimal getValorInscricao() {
        return valorInscricao;
    }

    public void setValorInscricao(BigDecimal valorInscricao) {
        this.valorInscricao = valorInscricao;
    }

    public BigDecimal getValorInicial() {
        return valorInicial;
    }

    public void setValorInicial(BigDecimal valorInicial) {
        this.valorInicial = valorInicial;
    }

    public BigDecimal getIncremento() {
        return incremento;
    }

    public void setIncremento(BigDecimal incremento) {
        this.incremento = incremento;
    }

    public int getEquipasPorEscalao() {
        return equipasPorEscalao;
    }

    public void setEquipasPorEscalao(int equipasPorEscalao) {
        this.equipasPorEscalao = equipasPorEscalao;
    }

    public BigDecimal getValorMaximo() {
        return valorMaximo;
    }

    public void setValorMaximo(BigDecimal valorMaximo) {
        this.valorMaximo = valorMaximo;
    }

    public int getJornadasPorBloco() {
        return jornadasPorBloco;
    }

    public void setJornadasPorBloco(int jornadasPorBloco) {
        this.jornadasPorBloco = jornadasPorBloco;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegraDivida outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
