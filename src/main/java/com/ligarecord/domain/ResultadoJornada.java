package com.ligarecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "resultado_jornada")
public class ResultadoJornada extends EntidadeBase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipa_id", nullable = false)
    private Equipa equipa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jornada_id", nullable = false)
    private Jornada jornada;

    @Column(nullable = false)
    private int pontuacao;

    @Column(nullable = false)
    private int posicao;

    @Column(name = "desempate_manual", nullable = false)
    private boolean desempateManual;

    protected ResultadoJornada() {
        // exigido pelo Hibernate
    }

    public ResultadoJornada(UUID id, Equipa equipa, Jornada jornada, int pontuacao, int posicao, boolean desempateManual){
        this.id = id;
        this.equipa = equipa;
        this.jornada = jornada;
        this.pontuacao = pontuacao;
        this.posicao = posicao;
        this.desempateManual = desempateManual;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Equipa getEquipa() {
        return equipa;
    }

    public void setEquipa(Equipa equipa) {
        this.equipa = equipa;
    }

    public Jornada getJornada() {
        return jornada;
    }

    public void setJornada(Jornada jornada) {
        this.jornada = jornada;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(int pontuacao) {
        this.pontuacao = pontuacao;
    }

    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    public boolean isDesempateManual() {
        return desempateManual;
    }

    public void setDesempateManual(boolean desempateManual) {
        this.desempateManual = desempateManual;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResultadoJornada outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
