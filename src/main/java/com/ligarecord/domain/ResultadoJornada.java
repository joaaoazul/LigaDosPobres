package com.ligarecord.domain;

import java.util.UUID;

public class ResultadoJornada {

    private UUID id;
    private Equipa equipa;
    private Jornada jornada;
    private int pontuacao;
    private int posicao;
    private boolean desempateManual;

    public ResultadoJornada(UUID id, Equipa equipa, Jornada jornada, int pontuacao, int posicao, boolean desempateManual){
        this.id = id;
        this.equipa = equipa;
        this.jornada = jornada;
        this.pontuacao = pontuacao;
        this.posicao = posicao;
        this.desempateManual = desempateManual;
    }

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
}
