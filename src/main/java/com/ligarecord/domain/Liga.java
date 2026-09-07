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

    /**
     * {@code numJornada} reinicia em 1 quando as jornadas de treino terminam
     * e começam as oficiais, por isso esta ordem NÃO é a ordem cronológica
     * real assim que a liga tiver as duas — quem precisar dessa ordem (por
     * exemplo, para mostrar a lista de jornadas) tem de ordenar de novo com
     * {@link Jornada#ORDEM_CRONOLOGICA}, não confiar nesta coleção tal como
     * vem.
     */
    @OneToMany(mappedBy = "liga", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numJornada")
    private List<Jornada> jornadas = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLiga estado;

    @Column(name = "max_equipas", nullable = false)
    private int maxEquipas;

    /**
     * Tipo de conteúdo do logo desta liga (ex.: {@code image/png}), ou
     * {@code null} se a liga não tiver logo. Os bytes vivem na tabela
     * {@code liga_logo}, à parte, para não serem carregados em cada listagem.
     */
    @Column(name = "logo_tipo")
    private String logoTipo;

    /**
     * O gestor que criou e administra esta liga. Toda a autorização da aplicação
     * assenta neste campo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Gestor gestor;

    /**
     * Há ligas em que as jornadas de treino são um aquecimento: quando as
     * oficiais começam, a tabela recomeça do zero. Nas ligas que já existiam os
     * pontos contam — o manual diz "para efeitos de dinheiro e de
     * classificação, as duas contam igual" — e é esse o valor por omissão.
     *
     * <p>Vive aqui e não na regra de dívida porque é do formato da prova, não
     * da cobrança: uma liga sem regra nenhuma continua a ter classificação.
     */
    @Column(name = "pontos_treino_contam", nullable = false)
    private boolean pontosTreinoContam = true;

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

    public boolean isPontosTreinoContam() {
        return pontosTreinoContam;
    }

    public void setPontosTreinoContam(boolean pontosTreinoContam) {
        this.pontosTreinoContam = pontosTreinoContam;
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

    public String getLogoTipo() {
        return logoTipo;
    }

    public void setLogoTipo(String logoTipo) {
        this.logoTipo = logoTipo;
    }

    public boolean temLogo() {
        return logoTipo != null;
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
