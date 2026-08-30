package com.ligarecord.domain;

import com.ligarecord.domain.enums.PapelGestor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Quem entra na aplicação e administra ligas. Distinto de {@link Treinador}:
 * um treinador é alguém que joga, um gestor é alguém que tem credenciais.
 * Um gestor pode gerir várias ligas; cada liga tem um gestor.
 */
@Entity
@Table(name = "gestor")
public class Gestor extends EntidadeBase {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PapelGestor papel;

    /** Uma conta desativada não entra e não é apagada, para não levar as ligas atrás. */
    @Column(nullable = false)
    private boolean ativo;

    protected Gestor() {
        // exigido pelo Hibernate
    }

    public Gestor(UUID id, String email, String passwordHash, String nome) {
        this(id, email, passwordHash, nome, PapelGestor.GESTOR);
    }

    public Gestor(UUID id, String email, String passwordHash, String nome, PapelGestor papel) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.criadoEm = Instant.now();
        this.papel = papel;
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

    public PapelGestor getPapel() {
        return papel;
    }

    public void setPapel(PapelGestor papel) {
        this.papel = papel;
    }

    public boolean isAdmin() {
        return papel == PapelGestor.ADMIN;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Gestor outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Nunca incluir o hash nem o email completo em logs. */
    @Override
    public String toString() {
        return "Gestor{id=" + id + "}";
    }
}
