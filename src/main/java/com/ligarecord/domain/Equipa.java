package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoEquipa;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "equipa")
public class Equipa extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    /** Único desde a V9: uma linha de {@link Treinador} é o lugar desta equipa
     *  e de mais nenhuma. Partilhada, um convite ligava duas equipas de uma vez. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "treinador_id", nullable = false, unique = true)
    private Treinador treinador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liga_id")
    private Liga liga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEquipa estado;

    /**
     * A ordem que o gestor deu a esta equipa da última vez que a desempatou
     * na classificação geral, contra outras que tinham os mesmos pontos.
     * {@code null} enquanto nunca tiver entrado num desempate resolvido — aí
     * é o nome que decide, como sempre decidiu (ver
     * {@link com.ligarecord.service.ClassificacaoService}).
     *
     * <p>Só vale enquanto os pontos da equipa forem os mesmos de quando foi
     * fixado — ver {@link #ordemDesempatePontos}. Sem essa condição, um
     * desempate resolvido a 10 pontos ficava a decidir também um empate
     * posterior e diferente, a 15 pontos, entre equipas que nunca chegaram a
     * ser comparadas.
     */
    @Column(name = "ordem_desempate")
    private Integer ordemDesempate;

    /**
     * Os pontos acumulados da equipa no momento em que {@link #ordemDesempate}
     * foi fixado. {@link com.ligarecord.service.ClassificacaoService} só usa
     * a ordem enquanto os pontos de agora ainda forem estes; assim que a
     * equipa ganhar ou perder pontos, o valor antigo deixa de se aplicar por
     * si só, sem ser preciso limpá-lo.
     */
    @Column(name = "ordem_desempate_pontos")
    private Integer ordemDesempatePontos;

    protected Equipa() {
        // exigido pelo Hibernate
    }

    public Equipa(UUID id, String nome, Treinador treinador, Liga liga, EstadoEquipa estado){
        this.id = id;
        this.nome = nome;
        this.treinador = treinador;
        this.liga = liga;
        this.estado = estado;
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

    public Integer getOrdemDesempate() {
        return ordemDesempate;
    }

    public void setOrdemDesempate(Integer ordemDesempate) {
        this.ordemDesempate = ordemDesempate;
    }

    public Integer getOrdemDesempatePontos() {
        return ordemDesempatePontos;
    }

    public void setOrdemDesempatePontos(Integer ordemDesempatePontos) {
        this.ordemDesempatePontos = ordemDesempatePontos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Equipa outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
