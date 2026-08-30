package com.ligarecord.repository;

import com.ligarecord.domain.Convite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteRepository {
    Convite guardar(Convite convite);
    Optional<Convite> buscarPorCodigo(String codigo);
    Optional<Convite> buscarPorId(UUID id);
    List<Convite> listarTodos();
}
