package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoLiga;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "liga")
public class Liga extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @OneToMany(mappedBy = "liga", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("nome")
    private List<Equipa> equipas = new ArrayList<>();

    @OneToMany(mappedBy = "liga", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numJornada")
    private List<Jornada> jornadas = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLiga estado;

    @Column(name = "max_equipas", nullable = false)
    private int maxEquipas;

    /**
     * O gestor que criou e administra esta liga. Toda a autorização da aplicação
     * assenta neste campo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Gestor gestor;

    protected Liga() {
        // exigido pelo Hibernate
    }

    public Liga(UUID id, String nome, int maxEquipas, EstadoLiga estado, Gestor gestor){
        this.id = id;
        this.nome = nome;
        this.equipas = new ArrayList<>();
        this.jornadas = new ArrayList<>();
        this.maxEquipas = maxEquipas;
        this.estado = estado;
        this.gestor = gestor;
    }

    @Override
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

    public Gestor getGestor() {
        return gestor;
    }

    public void setGestor(Gestor gestor) {
        this.gestor = gestor;
    }

    public void adicionarEquipa(Equipa equipa){
        this.equipas.add(equipa);
    }

    public void adicionarJornada(Jornada jornada){
        this.jornadas.add(jornada);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Liga outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
