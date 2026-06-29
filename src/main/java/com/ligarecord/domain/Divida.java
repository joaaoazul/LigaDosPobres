package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoDivida;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Divida {

    private UUID id;
    private Treinador treinador;
    private List<BlocoDivida> blocos;
    private EstadoDivida estado;

    public Divida(UUID id, Treinador treinador, EstadoDivida estado){
        this.id = id;
        this.treinador = treinador;
        this.blocos = new ArrayList<>();
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Treinador getTreinador() {
        return treinador;
    }

    public void setTreinador(Treinador treinador) {
        this.treinador = treinador;
    }

    public List<BlocoDivida> getBlocos() {
        return blocos;
    }

    public void setBlocos(List<BlocoDivida> blocos) {
        this.blocos = blocos;
    }

    public EstadoDivida getEstado() {
        return estado;
    }

    public void setEstado(EstadoDivida estado) {
        this.estado = estado;
    }
}
