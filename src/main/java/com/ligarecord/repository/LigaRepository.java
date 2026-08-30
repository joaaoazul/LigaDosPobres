package com.ligarecord.repository;

import com.ligarecord.domain.Liga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigaRepository {

    Liga guardarLiga(Liga liga);
    List<Liga> listarLigas();
    Optional<Liga> buscarPorId(UUID id);

}
