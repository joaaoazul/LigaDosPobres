package com.ligarecord.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Desempate {

    private UUID id;
    private Jornada jornada;
    private List<Equipa> equipasEmp;
    private List<Equipa> ordemAdmin;

    public Desempate(UUID id, Jornada jornada, List<Equipa> equipasEmp){
        this.id = id;
        this.jornada = jornada;
        this.equipasEmp = equipasEmp;
        this.ordemAdmin = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Jornada getJornada() {
        return jornada;
    }

    public void setJornada(Jornada jornada) {
        this.jornada = jornada;
    }

    public List<Equipa> getEquipasEmp() {
        return equipasEmp;
    }

    public void setEquipasEmp(List<Equipa> equipasEmp) {
        this.equipasEmp = equipasEmp;
    }

    public List<Equipa> getOrdemAdmin() {
        return ordemAdmin;
    }

    public void setOrdemAdmin(List<Equipa> ordemAdmin) {
        this.ordemAdmin = ordemAdmin;
    }
}
