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
 * Conta de acesso de um treinador, ligada ao {@link Treinador} que já existe
 * no domínio (nome, sem credenciais). Um treinador tem no máximo uma conta.
 */
@Entity
@Table(name = "conta_treinador")
public class ContaTreinador extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt. A password em claro nunca é guardada nem registada em log. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nome;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private boolean ativo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treinador_id", nullable = false, unique = true)
    private Treinador treinador;

    protected ContaTreinador() {
        // exigido pelo Hibernate
    }

    public ContaTreinador(UUID id, String email, String passwordHash, String nome, Treinador treinador) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.treinador = treinador;
        this.criadoEm = Instant.now();
        this.ativo = true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Treinador getTreinador() {
        return treinador;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContaTreinador outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Nunca incluir o hash nem o email completo em logs. */
    @Override
    public String toString() {
        return "ContaTreinador{id=" + id + "}";
    }
}
