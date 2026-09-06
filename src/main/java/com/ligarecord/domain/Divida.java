package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoDivida;
import com.ligarecord.domain.enums.TipoBloco;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O que uma equipa deve à liga, acumulado ao longo de vários
 * {@link BlocoDivida}. Uma equipa tem no máximo uma dívida, que nunca é
 * apagada — só acrescenta blocos — por isso {@code estado} reflecte se há
 * algum bloco ainda pendente, e não um histórico de dívidas separadas.
 */
@Entity
@Table(name = "divida")
public class Divida extends EntidadeBase {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipa_id", nullable = false, unique = true)
    private Equipa equipa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDivida estado;

    /**
     * O número do próximo bloco. Vive aqui, e não é calculado a partir de
     * {@code blocos} na hora, para que o optimistic locking ({@code versao})
     * o proteja: duas transacções a acrescentar um bloco em simultâneo
     * disputam esta mesma linha, e uma delas falha em vez de as duas
     * gravarem um bloco com o mesmo número.
     */
    @Column(name = "proximo_numero_bloco", nullable = false)
    private int proximoNumeroBloco = 1;

    @Version
    @Column(nullable = false)
    private long versao;

    @OneToMany(mappedBy = "divida", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroBloco")
    private List<BlocoDivida> blocos = new ArrayList<>();

    protected Divida() {
        // exigido pelo Hibernate
    }

    public Divida(UUID id, Equipa equipa, EstadoDivida estado) {
        this.id = id;
        this.equipa = equipa;
        this.blocos = new ArrayList<>();
        this.estado = estado;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Equipa getEquipa() {
        return equipa;
    }

    public List<BlocoDivida> getBlocos() {
        return blocos;
    }

    public EstadoDivida getEstado() {
        return estado;
    }

    public void setEstado(EstadoDivida estado) {
        this.estado = estado;
    }

    /** Acrescenta um bloco de período (recorrente), numerado sequencialmente. */
    public BlocoDivida registarBloco(BigDecimal valor) {
        return registarBloco(TipoBloco.PERIODO, valor);
    }

    /**
     * Acrescenta o bloco de inscrição na liga — cobrado uma vez, isolado dos
     * blocos de período mas contando para o mesmo total em dívida.
     */
    public BlocoDivida registarInscricao(BigDecimal valor) {
        return registarBloco(TipoBloco.INSCRICAO, valor);
    }

    private BlocoDivida registarBloco(TipoBloco tipo, BigDecimal valor) {
        BlocoDivida bloco = new BlocoDivida(UUID.randomUUID(), proximoNumeroBloco, tipo, valor);
        proximoNumeroBloco++;
        bloco.setDivida(this);
        blocos.add(bloco);
        // Um bloco de 0.00€ (equipa no escalão mais barato) não tem nada para o
        // gestor cobrar; exigir que o marque como pago à mesma era só ruído.
        if (valor.signum() == 0) {
            bloco.marcarResolvido();
        }
        atualizarEstado();
        return bloco;
    }

    /**
     * PENDENTE enquanto sobrar um bloco por pagar, RESOLVIDA quando não sobra
     * nenhum. Recalculado a cada mudança em vez de assumido: uma dívida nova
     * cujo primeiro bloco é de 0.00€ nasceria PENDENTE sem nada pendente, e
     * ficava a pedir ao gestor uma cobrança que não existe.
     */
    public void atualizarEstado() {
        boolean aindaDeve = blocos.stream().anyMatch(bloco -> !bloco.estaResolvido());
        estado = aindaDeve ? EstadoDivida.PENDENTE : EstadoDivida.RESOLVIDA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Divida outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
