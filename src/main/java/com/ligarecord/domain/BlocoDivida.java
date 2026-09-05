package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoDivida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Um encargo de um período (bloco de jornadas) sobre a dívida de uma equipa.
 *
 * <p>O valor é hoje escrito à mão pelo gestor ao fechar o bloco. O cálculo
 * automático a partir da classificação — por escalões de equipas, configurável
 * por liga — fica para depois; nada aqui o impede de vir a substituir esta
 * escrita manual mais tarde sem mudar o esquema.
 */
@Entity
@Table(name = "bloco_divida")
public class BlocoDivida extends EntidadeBase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "divida_id", nullable = false)
    private Divida divida;

    @Column(name = "numero_bloco", nullable = false)
    private int numeroBloco;

    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDivida estado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "resolvido_em")
    private Instant resolvidoEm;

    protected BlocoDivida() {
        // exigido pelo Hibernate
    }

    public BlocoDivida(UUID id, int numeroBloco, BigDecimal valor) {
        this.id = id;
        this.numeroBloco = numeroBloco;
        this.valor = valor;
        this.estado = EstadoDivida.PENDENTE;
        this.criadoEm = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Divida getDivida() {
        return divida;
    }

    void setDivida(Divida divida) {
        this.divida = divida;
    }

    public int getNumeroBloco() {
        return numeroBloco;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public EstadoDivida getEstado() {
        return estado;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getResolvidoEm() {
        return resolvidoEm;
    }

    public boolean estaResolvido() {
        return estado == EstadoDivida.RESOLVIDA;
    }

    public void marcarResolvido() {
        this.estado = EstadoDivida.RESOLVIDA;
        this.resolvidoEm = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlocoDivida outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
