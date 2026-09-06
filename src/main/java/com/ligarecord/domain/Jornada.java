package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoJornada;
import com.ligarecord.domain.enums.EstadoJornadaTreino;
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
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jornada")
public class Jornada extends EntidadeBase {

    /**
     * A ordem real em que as jornadas acontecem: todas as de treino antes de
     * todas as oficiais (garantido por {@code JornadaService.abrirJornada}),
     * e dentro de cada tipo por número. {@code numJornada} sozinho não chega
     * — reinicia em 1 quando as oficiais começam — e o {@code @OrderBy} da
     * coleção em {@link Liga} não sabe disso, por isso quem precisar da
     * ordem verdadeira (mostrar a lista de jornadas, por exemplo) tem de
     * ordenar explicitamente com isto em vez de confiar na coleção tal como
     * vem.
     */
    public static final Comparator<Jornada> ORDEM_CRONOLOGICA = Comparator
            .comparing((Jornada j) -> j.tipoJornada == EstadoJornadaTreino.OFICIAL)
            .thenComparingInt(j -> j.numJornada);

    @Id
    private UUID id;

    @Column(name = "num_jornada", nullable = false)
    private int numJornada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoJornada estadoJ;

    /**
     * Única fonte de verdade sobre o tipo da jornada. O antigo campo booleano
     * {@code eTreino} foi removido: guardava a mesma informação e as duas
     * colunas podiam contradizer-se.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private EstadoJornadaTreino tipoJornada;

    /**
     * Marca que esta jornada já foi somada num bloco de dívida fechado. Sem
     * isto, mudar {@code jornadasPorBloco} a meio da época (RegraDividaService
     * é idempotente de propósito) fazia o próximo fecho olhar outra vez para
     * jornadas já cobradas e somá-las a dobro.
     */
    @Column(name = "incluida_em_bloco", nullable = false)
    private boolean incluidaEmBloco = false;

    @OneToMany(mappedBy = "jornada", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResultadoJornada> resultadoJ = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liga_id", nullable = false)
    private Liga liga;

    protected Jornada() {
        // exigido pelo Hibernate
    }

    public Jornada(UUID id, int numJornada, EstadoJornada estadoJ, EstadoJornadaTreino tipoJornada, Liga liga){
        this.id = id;
        this.numJornada = numJornada;
        this.estadoJ = estadoJ;
        this.tipoJornada = tipoJornada;
        this.liga = liga;
        this.resultadoJ = new ArrayList<>();
    }

    @Override
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

    public EstadoJornadaTreino getTipoJornada() {
        return tipoJornada;
    }

    public void setTipoJornada(EstadoJornadaTreino tipoJornada) {
        this.tipoJornada = tipoJornada;
    }

    public boolean isIncluidaEmBloco() {
        return incluidaEmBloco;
    }

    public void setIncluidaEmBloco(boolean incluidaEmBloco) {
        this.incluidaEmBloco = incluidaEmBloco;
    }

    /** Derivado do tipo: não há estado duplicado para se desencontrar. */
    public boolean iseTreino() {
        return tipoJornada == EstadoJornadaTreino.TREINO;
    }

    public List<ResultadoJornada> getResultadoJ() {
        return resultadoJ;
    }

    public void setResultadoJ(List<ResultadoJornada> resultadoJ) {
        this.resultadoJ = resultadoJ;
    }

    public Liga getLiga() {
        return liga;
    }

    public void setLiga(Liga liga) {
        this.liga = liga;
    }

    public void adicionarResultado(ResultadoJornada resultado) {
        this.resultadoJ.add(resultado);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Jornada outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
