package com.ligarecord.repository;

import com.ligarecord.domain.Convite;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConviteRepositoryImpl extends RepositorioEmMemoria<Convite> implements ConviteRepository {

    @Override
    public Convite guardar(Convite convite) {
        return super.guardar(convite);
    }

    @Override
    public Optional<Convite> buscarPorCodigo(String codigo) {
        return entidades.stream().filter(convite -> convite.getCodigo().equals(codigo)).findFirst();
    }

    @Override
    public Optional<Convite> buscarPorId(UUID id) {
        return super.buscarPorId(id);
    }

    @Override
    public List<Convite> listarTodos() {
        // Mesma ordem que a consulta JPA real (findAllByOrderByCriadoEmDesc).
        return entidades.stream()
                .sorted(Comparator.comparing(Convite::getCriadoEm).reversed())
                .toList();
    }
}
