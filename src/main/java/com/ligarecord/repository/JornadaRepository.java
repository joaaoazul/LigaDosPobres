package com.ligarecord.repository;

import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JornadaRepository {
    Jornada guardar(Jornada jornada);
    List<Jornada> listarJornadas(Liga liga);
    Optional<Jornada> buscarPorId(UUID id);
}
