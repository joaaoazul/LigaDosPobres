package com.ligarecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Convite de uso único para ligar uma {@link ContaTreinador} a um
 * {@link Treinador} já existente. Criado pelo gestor dono da liga a que
 * pertence a equipa do treinador — nunca pelo treinador, que ainda não tem
 * conta nenhuma nesse momento.
 *
 * <p>Tal como o {@link Convite}, nunca é apagado: fica o registo de quem
 * convidou quem, e quando.
 */
@Entity
@Table(name = "convite_treinador")
public class ConviteTreinador extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treinador_id", nullable = false)
    private Treinador treinador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por", nullable = false)
    private Gestor criadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usado_por")
    private ContaTreinador usadoPor;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    protected ConviteTreinador() {
        // exigido pelo Hibernate
    }

    public ConviteTreinador(UUID id, String codigo, Treinador treinador, Gestor criadoPor, Instant expiraEm) {
        this.id = id;
        this.codigo = codigo;
        this.treinador = treinador;
        this.criadoPor = criadoPor;
        this.criadoEm = Instant.now();
        this.expiraEm = expiraEm;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Treinador getTreinador() {
        return treinador;
    }

    public Gestor getCriadoPor() {
        return criadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getUsadoEm() {
        return usadoEm;
    }

    public ContaTreinador getUsadoPor() {
        return usadoPor;
    }

    public Instant getRevogadoEm() {
        return revogadoEm;
    }

    public boolean estaUsado() {
        return usadoEm != null;
    }

    public boolean estaRevogado() {
        return revogadoEm != null;
    }

    public boolean estaExpirado() {
        return expiraEm != null && Instant.now().isAfter(expiraEm);
    }

    public boolean estaDisponivel() {
        return !estaUsado() && !estaRevogado() && !estaExpirado();
    }

    public void marcarUsado(ContaTreinador conta) {
        this.usadoEm = Instant.now();
        this.usadoPor = conta;
    }

    public void revogar() {
        this.revogadoEm = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConviteTreinador outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Nunca incluir o código em logs: é uma credencial enquanto não for usado. */
    @Override
    public String toString() {
        return "ConviteTreinador{id=" + id + "}";
    }
}
