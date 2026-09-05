package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoDivida;
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

    /** Acrescenta um novo bloco, numerado sequencialmente. */
    public BlocoDivida registarBloco(BigDecimal valor) {
        BlocoDivida bloco = new BlocoDivida(UUID.randomUUID(), proximoNumeroBloco, valor);
        proximoNumeroBloco++;
        bloco.setDivida(this);
        blocos.add(bloco);
        estado = EstadoDivida.PENDENTE;
        return bloco;
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
