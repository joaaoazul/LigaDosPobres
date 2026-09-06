package com.ligarecord.repository;

import com.ligarecord.domain.Gestor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GestorRepositoryImpl extends RepositorioEmMemoria<Gestor> implements GestorRepository {

    @Override
    public Gestor guardar(Gestor gestor) {
        return super.guardar(gestor);
    }

    @Override
    public Optional<Gestor> buscarPorEmail(String email) {
        return entidades.stream().filter(gestor -> gestor.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    @Override
    public List<Gestor> listarTodos() {
        // Mesma ordem que a consulta JPA real (findAllByOrderByCriadoEmAsc).
        return entidades.stream().sorted(Comparator.comparing(Gestor::getCriadoEm)).toList();
    }

    @Override
    public long contarAdminsAtivos() {
        return entidades.stream().filter(g -> g.isAdmin() && g.isAtivo()).count();
    }

    @Override
    public Optional<Gestor> buscarPorId(UUID id) {
        return super.buscarPorId(id);
    }
}
