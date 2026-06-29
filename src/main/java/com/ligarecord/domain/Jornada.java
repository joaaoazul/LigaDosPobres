package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoJornada;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Jornada {
    private UUID id;
    private int numJornada;
    private EstadoJornada estadoJ;
    private boolean eTreino;
    private List<ResultadoJornada> resultadoJ;

    public Jornada(UUID id, int numJornada, EstadoJornada estadoJ, boolean eTreino){
        this.id = id;
        this.numJornada = numJornada;
        this.estadoJ = estadoJ;
        this.eTreino = eTreino;
        this.resultadoJ = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getNumJornada() {
        return numJornada;
    }

    public void setNumJornada(int numJornada) {
        this.numJornada = numJornada;
    }

    public EstadoJornada getEstadoJ() {
        return estadoJ;
    }

    public void setEstadoJ(EstadoJornada estadoJ) {
        this.estadoJ = estadoJ;
    }

    public boolean iseTreino() {
        return eTreino;
    }

    public void seteTreino(boolean eTreino) {
        this.eTreino = eTreino;
    }

    public List<ResultadoJornada> getResultadoJ() {
        return resultadoJ;
    }

    public void setResultadoJ(List<ResultadoJornada> resultadoJ) {
        this.resultadoJ = resultadoJ;
    }
}
