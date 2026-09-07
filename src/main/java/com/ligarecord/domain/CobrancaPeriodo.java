package com.ligarecord.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Uma cobrança que acontece numa jornada combinada, pela posição na
 * <b>classificação geral</b> — e não pela posição naquela jornada, como os
 * blocos de período.
 *
 * <p>É o "Inverno" e o "Verão" de algumas ligas: a meio da época e no fim, cada
 * equipa paga conforme o lugar em que está. Tem a sua própria tabela, que não é
 * a da jornada — pode ir de 0€ a 10€ enquanto a semanal vai de 0€ a 2,50€.
 *
 * <p>A jornada é indicada pelo <b>número oficial</b> e não pela contagem corrida
 * desde o início: as de treino podem ser mais ou menos do que se espera, e uma
 * cobrança que escorregue uma jornada é dinheiro cobrado no sítio errado.
 */
@Entity
@Table(name = "cobranca_periodo")
public class CobrancaPeriodo extends EntidadeBase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regra_id", nullable = false)
    private RegraDivida regra;

    @Column(nullable = false)
    private String nome;

    @Column(name = "jornada_oficial", nullable = false)
    private int jornadaOficial;

    @Column(name = "cobrada_em")
    private Instant cobradaEm;

    @OneToMany(mappedBy = "cobranca", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicao")
    private List<CobrancaValor> tabela = new ArrayList<>();

    protected CobrancaPeriodo() {
        // exigido pelo Hibernate
    }

    public CobrancaPeriodo(UUID id, RegraDivida regra, String nome, int jornadaOficial) {
        this.id = id;
        this.regra = regra;
        this.nome = nome;
        this.jornadaOficial = jornadaOficial;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public RegraDivida getRegra() {
        return regra;
    }

    public String getNome() {
        return nome;
    }

    public int getJornadaOficial() {
        return jornadaOficial;
    }

    public void setJornadaOficial(int jornadaOficial) {
        this.jornadaOficial = jornadaOficial;
    }

    public Instant getCobradaEm() {
        return cobradaEm;
    }

    public boolean estaCobrada() {
        return cobradaEm != null;
    }

    public void marcarCobrada() {
        this.cobradaEm = Instant.now();
    }

    public List<CobrancaValor> getTabela() {
        return tabela;
    }

    /** Reaproveita as linhas, pela mesma razão que a {@link RegraDivida#substituirTabela}. */
    public void substituirTabela(List<BigDecimal> valoresPorPosicao) {
        for (int i = 0; i < valoresPorPosicao.size(); i++) {
            if (i < tabela.size()) {
                tabela.get(i).definirValor(valoresPorPosicao.get(i));
            } else {
                tabela.add(new CobrancaValor(UUID.randomUUID(), this, i + 1, valoresPorPosicao.get(i)));
            }
        }
        while (tabela.size() > valoresPorPosicao.size()) {
            tabela.remove(tabela.size() - 1);
        }
    }

    /** Abaixo do fim da tabela repete-se a última linha, como na escala da regra. */
    public BigDecimal valorDaPosicao(int posicao) {
        if (tabela.isEmpty()) {
            throw new IllegalStateException("A cobrança \"" + nome + "\" não tem tabela de valores.");
        }
        return tabela.stream()
                .filter(linha -> linha.getPosicao() == posicao)
                .map(CobrancaValor::getValor)
                .findFirst()
                .orElseGet(() -> tabela.stream()
                        .max(Comparator.comparingInt(CobrancaValor::getPosicao))
                        .orElseThrow()
                        .getValor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CobrancaPeriodo outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
