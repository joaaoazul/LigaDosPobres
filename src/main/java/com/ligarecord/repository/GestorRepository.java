package com.ligarecord.repository;

import com.ligarecord.domain.Gestor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GestorRepository {
    Gestor guardar(Gestor gestor);
    Optional<Gestor> buscarPorEmail(String email);
    Optional<Gestor> buscarPorId(UUID id);
    List<Gestor> listarTodos();
    long contarAdminsAtivos();
}
