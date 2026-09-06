package com.ligarecord.repository;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação em memória. Continua a existir para os testes de serviço, que
 * assim correm sem base de dados nenhuma.
 */
public class LigaRepositoryImpl extends RepositorioEmMemoria<Liga> implements LigaRepository {

    @Override
    public Liga guardarLiga(Liga liga) {
        return guardar(liga);
    }

    @Override
    public List<Liga> listarLigas(Gestor gestor) {
        // Mesma ordem que a consulta JPA real (findByGestorIdOrderByNome).
        return entidades.stream()
                .filter(liga -> gestor != null && liga.getGestor() != null
                        && gestor.getId().equals(liga.getGestor().getId()))
                .sorted(Comparator.comparing(Liga::getNome))
                .toList();
    }

    @Override
    public Optional<Liga> buscarPorIdEGestor(UUID id, UUID gestorId) {
        return entidades.stream()
                .filter(liga -> liga.getId().equals(id)
                        && liga.getGestor() != null
                        && liga.getGestor().getId().equals(gestorId))
                .findFirst();
    }
}
