package com.ligarecord.domain;

import java.util.UUID;

public class ClassificacaoGeral {
    private UUID id;
    private Equipa equipa;
    private int posicao;
    private int pontosAcumulados;

    public ClassificacaoGeral(UUID id, Equipa equipa, int posicao, int pontosAcumulados){
        this.id = id;
        this.equipa = equipa;
        this.posicao = posicao;
        this.pontosAcumulados = pontosAcumulados;
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

    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    public int getPontosAcumulados() {
        return pontosAcumulados;
    }

    public void setPontosAcumulados(int pontosAcumulados) {
        this.pontosAcumulados = pontosAcumulados;
    }
}

