package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoEquipa;

import java.util.UUID;

public class Equipa {
    private UUID id;
    private String nome;
    private Treinador treinador;
    private Liga liga;
    private EstadoEquipa estado;

    public Equipa(UUID id, String nome, Treinador treinador, Liga liga, EstadoEquipa estado){
        this.id = id;
        this.nome = nome;
        this.treinador = treinador;
        this.liga = liga;
        this.estado = estado;
    }

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

    public Treinador getTreinador() {
        return treinador;
    }

    public void setTreinador(Treinador treinador) {
        this.treinador = treinador;
    }

    public Liga getLiga() {
        return liga;
    }

    public void setLiga(Liga liga) {
        this.liga = liga;
    }

    public EstadoEquipa getEstado() {
        return estado;
    }

    public void setEstado(EstadoEquipa estado) {
        this.estado = estado;
    }
}
