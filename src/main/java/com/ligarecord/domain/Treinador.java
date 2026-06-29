package com.ligarecord.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Treinador {

   private UUID id;
    private String nome;
    private List<Divida> dividas;

    public Treinador(UUID id, String nome){
        this.id = id;
        this.nome = nome;
        this.dividas = new ArrayList<>();
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

    public List<Divida> getDividas() {
        return dividas;
    }

    public void setDividas(List<Divida> dividas) {
        this.dividas = dividas;
    }
}
