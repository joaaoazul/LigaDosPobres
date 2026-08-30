package com.ligarecord.repository;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação em memória. Continua a existir para os testes de serviço, que
 * assim correm sem base de dados nenhuma.
 */
public class LigaRepositoryImpl implements LigaRepository {

    private final List<Liga> ligas = new ArrayList<>();

    @Override
    public Liga guardarLiga(Liga liga) {
        for (int i = 0; i < ligas.size(); i++) {
            if (ligas.get(i).getId().equals(liga.getId())) {
                ligas.set(i, liga);
                return liga;
            }
        }
        ligas.add(liga);
        return liga;
    }

    @Override
    public List<Liga> listarLigas(Gestor gestor) {
        List<Liga> resultado = new ArrayList<>();
        for (Liga liga : ligas) {
            if (gestor != null && liga.getGestor() != null && gestor.getId().equals(liga.getGestor().getId())) {
                resultado.add(liga);
            }
        }
        return resultado;
    }

    @Override
    public Optional<Liga> buscarPorIdEGestor(UUID id, UUID gestorId) {
        for (Liga liga : ligas) {
            if (liga.getId().equals(id)
                    && liga.getGestor() != null
                    && liga.getGestor().getId().equals(gestorId)) {
                return Optional.of(liga);
            }
        }
        return Optional.empty();
    }
}
