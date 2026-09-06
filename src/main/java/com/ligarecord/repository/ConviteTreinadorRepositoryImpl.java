package com.ligarecord.repository;

import com.ligarecord.domain.ConviteTreinador;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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
    public Optional<ConviteTreinador> buscarPorIdEEquipa(UUID id, UUID equipaId) {
        return entidades.stream()
                .filter(convite -> convite.getId().equals(id) && convite.getEquipa().getId().equals(equipaId))
                .findFirst();
    }

    @Override
    public List<ConviteTreinador> listarPendentesPorTreinador(UUID treinadorId) {
        return pendentes()
                .filter(convite -> convite.getTreinador().getId().equals(treinadorId))
                .toList();
    }

    @Override
    public List<ConviteTreinador> listarPendentesPorLiga(UUID ligaId) {
        return pendentes()
                .filter(convite -> convite.getEquipa().getLiga() != null
                        && convite.getEquipa().getLiga().getId().equals(ligaId))
                .toList();
    }

    /** Mesma ordem e mesmo critério das consultas JPA: do mais recente para o mais antigo. */
    private Stream<ConviteTreinador> pendentes() {
        return entidades.stream()
                .filter(convite -> !convite.estaUsado() && !convite.estaRevogado())
                .sorted(Comparator.comparing(ConviteTreinador::getCriadoEm).reversed());
    }
}
