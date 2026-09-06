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
 * Um pedido para redefinir a password, de uso único e com validade curta.
 *
 * <p>Ao contrário do {@link Convite} e do {@link ConviteTreinador}, aqui não é
 * guardado o código mas sim o seu resumo criptográfico. A diferença é
 * deliberada: um convite por usar dá uma conta nova, este código dá uma conta
 * que já existe, com as ligas e o dinheiro lá dentro. Quem conseguisse ler a
 * tabela levava as contas todas atrás; guardando o resumo, o que lá está não
 * serve para nada, porque o que vai no email nunca chega a ser escrito.
 *
 * <p>Nunca é apagado depois de usado: fica o registo de quando a password de
 * uma conta foi redefinida, que é exactamente o que se quer poder consultar se
 * um dia houver dúvidas sobre um acesso.
 */
@Entity
@Table(name = "pedido_recuperacao")
public class PedidoRecuperacao extends EntidadeBase {

    @Id
    private UUID id;

    /** SHA-256 do código, em hexadecimal. O código em si só existe no email. */
    @Column(name = "codigo_hash", nullable = false, unique = true)
    private String codigoHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Gestor gestor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    protected PedidoRecuperacao() {
        // exigido pelo Hibernate
    }

    public PedidoRecuperacao(UUID id, String codigoHash, Gestor gestor, Instant expiraEm) {
        this.id = id;
        this.codigoHash = codigoHash;
        this.gestor = gestor;
        this.criadoEm = Instant.now();
        this.expiraEm = expiraEm;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getCodigoHash() {
        return codigoHash;
    }

    public Gestor getGestor() {
        return gestor;
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

    public boolean estaUsado() {
        return usadoEm != null;
    }

    public boolean estaExpirado() {
        return Instant.now().isAfter(expiraEm);
    }

    public boolean estaDisponivel() {
        return !estaUsado() && !estaExpirado();
    }

    public void marcarUsado() {
        this.usadoEm = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PedidoRecuperacao outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Nem o resumo do código vai para logs: não há razão nenhuma para lá estar. */
    @Override
    public String toString() {
        return "PedidoRecuperacao{id=" + id + "}";
    }
}
