package com.ligarecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Alguém que treina uma ou mais equipas. Distinto de {@link Gestor}: um
 * treinador é criado pelo gestor dono da liga, sem credenciais próprias.
 *
 * <p>A ligação a {@code conta} é opcional — nem todo o treinador tem email ou
 * quer entrar na aplicação. Sem conta, o gestor continua a gerir a equipa e as
 * dívidas do treinador normalmente. A conta, quando existe, é a mesma tabela
 * do {@link Gestor}: a mesma pessoa pode gerir uma liga e treinar uma equipa
 * sem precisar de dois logins, e pode até treinar mais do que uma equipa (em
 * ligas diferentes) com essa única conta — daí não haver aqui unicidade.
 */
@Entity
@Table(name = "treinador")
public class Treinador extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id")
    private Gestor conta;

    protected Treinador() {
        // exigido pelo Hibernate
    }

    public Treinador(UUID id, String nome){
        this.id = id;
        this.nome = nome;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Gestor getConta() {
        return conta;
    }

    public void setConta(Gestor conta) {
        this.conta = conta;
    }

    public boolean temConta() {
        return conta != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Treinador outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
