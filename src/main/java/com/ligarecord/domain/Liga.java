package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoLiga;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Liga {

    private UUID id;
    private String nome;
    private List<Equipa> equipas;
    private List<Jornada> jornadas;
    private EstadoLiga estado;
    private int maxEquipas;

    public Liga(UUID id, String nome, int maxEquipas, EstadoLiga estado){
        this.id = id;
        this.nome = nome;
        this.equipas = new ArrayList<>();
        this.jornadas = new ArrayList<>();
        this.maxEquipas = maxEquipas;
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

    public List<Equipa> getEquipas() {
        return equipas;
    }

    public void setEquipas(List<Equipa> equipas) {
        this.equipas = equipas;
    }

    public List<Jornada> getJornadas() {
        return jornadas;
    }

    public void setJornadas(List<Jornada> jornadas) {
        this.jornadas = jornadas;
    }

    public EstadoLiga getEstado() {
        return estado;
    }

    public void setEstado(EstadoLiga estado) {
        this.estado = estado;
    }

    public int getMaxEquipas() {
        return maxEquipas;
    }

    public void setMaxEquipas(int maxEquipas) {
        this.maxEquipas = maxEquipas;
    }
}
