package com.ligarecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O registo de uma tentativa de login que falhou, para travar quem vai
 * adivinhando passwords.
 *
 * <p>Guarda-se o email, não a conta: uma tentativa contra um email sem conta
 * nenhuma tem de contar tanto como uma contra um email que existe, senão o
 * próprio bloqueio passava a dizer quais os emails com conta — o mesmo cuidado
 * de {@link com.ligarecord.service.RecuperacaoService}.
 *
 * <p>Nunca é apagada: veja-se {@link com.ligarecord.domain.PedidoRecuperacao},
 * cujo Javadoc explica a mesma decisão para os pedidos de recuperação.
 */
@Entity
@Table(name = "tentativa_login_falhada")
public class TentativaLoginFalhada extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected TentativaLoginFalhada() {
        // exigido pelo Hibernate
    }

    public TentativaLoginFalhada(UUID id, String email) {
        this.id = id;
        this.email = email;
        this.criadoEm = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TentativaLoginFalhada outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
