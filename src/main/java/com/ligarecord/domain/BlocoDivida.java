package com.ligarecord.domain;

import java.math.BigDecimal;
import java.util.*;

public class BlocoDivida {

    private UUID id;
    private int numeroBloco;
    private List<Jornada> jornadas;
    private Map<Equipa, BigDecimal> valorPorEquipa;

    public BlocoDivida(UUID id, int numeroBloco){
        this.id = id;
        this.numeroBloco = numeroBloco;
        this.jornadas = new ArrayList<>();
        this.valorPorEquipa = new HashMap<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getNumeroBloco() {
        return numeroBloco;
    }

    public void setNumeroBloco(int numeroBloco) {
        this.numeroBloco = numeroBloco;
    }

    public List<Jornada> getJornadas() {
        return jornadas;
    }

    public void setJornadas(List<Jornada> jornadas) {
        this.jornadas = jornadas;
    }

    public Map<Equipa, BigDecimal> getValorPorEquipa() {
        return valorPorEquipa;
    }

    public void setValorPorEquipa(Map<Equipa, BigDecimal> valorPorEquipa) {
        this.valorPorEquipa = valorPorEquipa;
    }

}
