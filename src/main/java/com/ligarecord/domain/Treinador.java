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
 * O lugar de treinador de uma equipa: o nome que o gestor lhe deu e, quando
 * existe, a conta da pessoa que o ocupa. Uma linha por equipa — a coluna
 * {@code equipa.treinador_id} é única desde a V9.
 *
 * <p><b>Isto não é a pessoa.</b> A pessoa é a {@code conta} ({@link Gestor}),
 * e é ela que se repete: quem treina três equipas tem três linhas aqui,
 * todas a apontar à mesma conta, e é por conta que se pergunta "que equipas
 * treino eu" ({@code TreinadorRepository.buscarPorConta}). Distinto de
 * {@link Gestor} também no resto: um treinador é criado pelo gestor dono da
 * liga, sem credenciais próprias.
 *
 * <p>O {@code nome} é o rótulo que o gestor escreveu e continua a ser o nome
 * mostrado mesmo depois de haver conta ligada: é o gestor que gere a lista de
 * equipas da liga dele, e o nome da conta é outra coisa — o da pessoa.
 *
 * <p>A ligação a {@code conta} é opcional — nem todo o treinador tem email ou
 * quer entrar na aplicação. Sem conta, o gestor continua a gerir a equipa e as
 * dívidas do treinador normalmente.
 */
@Entity
@Table(name = "treinador")
public class Treinador extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    /**
     * Contacto do lugar, para lhe enviar o convite. Opcional, e distinto do
     * email da conta: este é o que o gestor conhece, o outro é o que a pessoa
     * escolheu para entrar. Podem ser diferentes, e nada aqui os liga.
     */
    @Column(name = "email")
    private String email;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean temEmail() {
        return email != null && !email.isBlank();
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
