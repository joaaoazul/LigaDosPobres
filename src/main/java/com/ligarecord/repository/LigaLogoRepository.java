package com.ligarecord.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Os bytes do logo de cada liga. Separado de {@link LigaRepository} de propósito:
 * a listagem de ligas nunca precisa dos bytes, e assim não os arrasta.
 */
public interface LigaLogoRepository {

    /** Grava (ou substitui) o logo desta liga. */
    void guardar(UUID ligaId, byte[] dados);

    Optional<byte[]> buscar(UUID ligaId);

    void apagar(UUID ligaId);
}
