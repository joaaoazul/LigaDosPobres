package com.ligarecord.repository;

import com.ligarecord.domain.ConviteTreinador;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConviteTreinadorRepositoryImpl extends RepositorioEmMemoria<ConviteTreinador>
        implements ConviteTreinadorRepository {

    @Override
    public ConviteTreinador guardar(ConviteTreinador convite) {
        return super.guardar(convite);
    }

    @Override
    public Optional<ConviteTreinador> buscarPorCodigo(String codigo) {
        return entidades.stream().filter(convite -> convite.getCodigo().equals(codigo)).findFirst();
    }

    @Override
    public Optional<ConviteTreinador> buscarPorIdEGestor(UUID id, UUID gestorId) {
        return entidades.stream()
                .filter(convite -> convite.getId().equals(id) && convite.getCriadoPor().getId().equals(gestorId))
                .findFirst();
    }

    @Override
    public List<ConviteTreinador> listarPorGestor(UUID gestorId) {
        // Mesma ordem que a consulta JPA real (findByCriadoPorIdOrderByCriadoEmDesc).
        return entidades.stream()
                .filter(convite -> convite.getCriadoPor().getId().equals(gestorId))
                .sorted(Comparator.comparing(ConviteTreinador::getCriadoEm).reversed())
                .toList();
    }
}
