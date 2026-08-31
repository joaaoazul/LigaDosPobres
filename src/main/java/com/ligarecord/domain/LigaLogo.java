package com.ligarecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Os bytes do logo de uma liga, numa tabela à parte da {@link Liga} para não
 * serem carregados em cada listagem. A chave primária é o próprio id da liga
 * (relação um-para-um), pelo que gravar o logo de uma liga que já tem um
 * substitui o anterior.
 */
@Entity
@Table(name = "liga_logo")
public class LigaLogo {

    @Id
    @Column(name = "liga_id")
    private UUID ligaId;

    @Column(nullable = false)
    private byte[] dados;

    protected LigaLogo() {
        // exigido pelo Hibernate
    }

    public LigaLogo(UUID ligaId, byte[] dados) {
        this.ligaId = ligaId;
        this.dados = dados;
    }

    public UUID getLigaId() {
        return ligaId;
    }

    public byte[] getDados() {
        return dados;
    }

    public void setDados(byte[] dados) {
        this.dados = dados;
    }
}
