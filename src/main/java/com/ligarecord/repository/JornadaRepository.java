package com.ligarecord.repository;

import com.ligarecord.domain.Jornada;

import java.util.Optional;
import java.util.UUID;

public interface JornadaRepository {

    Jornada guardar(Jornada jornada);

    /** Filtrada pelo gestor dono da liga a que a jornada pertence. */
    Optional<Jornada> buscarPorIdEGestor(UUID id, UUID gestorId);
}
