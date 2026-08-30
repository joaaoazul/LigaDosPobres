package com.ligarecord.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * Base das entidades cujo identificador é atribuído pela aplicação
 * ({@code UUID.randomUUID()}) em vez de ser gerado pela base de dados.
 *
 * <p>Sem isto, o {@code save()} do Spring Data vê um id já preenchido, conclui
 * que a entidade não é nova e faz {@code merge} em vez de {@code persist}. As
 * consequências são silenciosas e difíceis de diagnosticar: relações em cascata
 * não são gravadas (violações de chave estrangeira) e a mesma entidade pode
 * aparecer duas vezes na sessão.
 */
@MappedSuperclass
public abstract class EntidadeBase implements Persistable<UUID> {

    @Transient
    private boolean novo = true;

    @Override
    public boolean isNew() {
        return novo;
    }

    @PostPersist
    @PostLoad
    void marcarComoGuardado() {
        this.novo = false;
    }
}
