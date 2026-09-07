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
 * Convite de uso único a um lugar: "treinas esta equipa". Criado pelo gestor
 * dono da liga a que a equipa pertence — nunca pelo treinador, que nesse
 * momento ainda não tem conta nenhuma.
 *
 * <p>Guarda a {@link Equipa} e não só o {@link Treinador} porque é isso que o
 * convite afirma, e é isso que quem o recebe precisa de ler antes de aceitar:
 * o nome da equipa e o da liga. O treinador vem da equipa e nunca é escolhido
 * à parte — um convite cujo treinador não fosse o daquela equipa não queria
 * dizer nada.
 *
 * <p>Quem aceita pode não ter conta nenhuma ainda (cria uma de raiz) ou já ser
 * gestor de outra liga, ou treinador de outra equipa (liga o convite à conta
 * que já tem, sem criar um segundo login). A identidade da pessoa é a conta;
 * cada {@link Treinador} é o lugar que ela ocupa numa equipa.
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
    @JoinColumn(name = "equipa_id", nullable = false)
    private Equipa equipa;

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

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(name = "enviado_para")
    private String enviadoPara;

    @Column(name = "envios", nullable = false)
    private int envios;

    protected ConviteTreinador() {
        // exigido pelo Hibernate
    }

    public ConviteTreinador(UUID id, String codigo, Equipa equipa, Gestor criadoPor, Instant expiraEm) {
        this.id = id;
        this.codigo = codigo;
        this.equipa = equipa;
        // Nunca recebido de fora: é sempre o treinador da equipa convidada.
        this.treinador = equipa.getTreinador();
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

    public Equipa getEquipa() {
        return equipa;
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

    public Instant getEnviadoEm() {
        return enviadoEm;
    }

    public String getEnviadoPara() {
        return enviadoPara;
    }

    public int getEnvios() {
        return envios;
    }

    /** Chamado só depois de o envio ter corrido bem: conta o que saiu, não o que se tentou. */
    public void marcarEnviado(String para) {
        this.enviadoEm = Instant.now();
        this.enviadoPara = para;
        this.envios++;
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

    public void marcarUsado(Gestor conta) {
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
