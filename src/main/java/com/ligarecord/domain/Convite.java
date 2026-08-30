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
 * Convite de uso único para criar uma conta de gestor.
 *
 * <p>Um convite pode estar pendente, usado, revogado ou expirado. Nunca é
 * apagado: o registo de quem entrou com que convite é o que permite perceber,
 * mais tarde, como é que uma conta apareceu.
 */
@Entity
@Table(name = "convite")
public class Convite extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String codigo;

    /** Para que serve este convite, à vista do administrador. */
    @Column
    private String nota;

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
    private Gestor usadoPor;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    protected Convite() {
        // exigido pelo Hibernate
    }

    public Convite(UUID id, String codigo, String nota, Gestor criadoPor, Instant expiraEm) {
        this.id = id;
        this.codigo = codigo;
        this.nota = nota;
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

    public String getNota() {
        return nota;
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

    public Gestor getUsadoPor() {
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

    public void marcarUsado(Gestor gestor) {
        this.usadoEm = Instant.now();
        this.usadoPor = gestor;
    }

    public void revogar() {
        this.revogadoEm = Instant.now();
    }

    /** Nunca incluir o código em logs: é uma credencial enquanto não for usado. */
    @Override
    public String toString() {
        return "Convite{id=" + id + "}";
    }
}
