package com.ligarecord.repository;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Treinador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EquipaRepositoryImpl extends RepositorioEmMemoria<Equipa> implements EquipaRepository {

    @Override
    public Equipa guardar(Equipa equipa) {
        return super.guardar(equipa);
    }

    @Override
    public List<Equipa> buscarPorTreinador(Treinador treinador) {
        return entidades.stream()
                .filter(equipa -> equipa.getTreinador() != null && equipa.getTreinador().equals(treinador))
                .toList();
    }

    @Override
    public Optional<Equipa> buscarPorIdEGestor(UUID id, UUID gestorId) {
        return entidades.stream()
                .filter(equipa -> equipa.getId().equals(id) && pertenceAoGestor(equipa, gestorId))
                .findFirst();
    }

    private boolean pertenceAoGestor(Equipa equipa, UUID gestorId) {
        return equipa.getLiga() != null
                && equipa.getLiga().getGestor() != null
                && equipa.getLiga().getGestor().getId().equals(gestorId);
    }
}
