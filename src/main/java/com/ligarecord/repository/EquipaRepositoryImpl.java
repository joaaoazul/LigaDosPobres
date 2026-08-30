package com.ligarecord.repository;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Treinador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EquipaRepositoryImpl implements EquipaRepository {

    private final List<Equipa> equipas = new ArrayList<>();

    @Override
    public Equipa guardar(Equipa equipa) {
        for (int i = 0; i < equipas.size(); i++) {
            if (equipas.get(i).getId().equals(equipa.getId())) {
                equipas.set(i, equipa);
                return equipa;
            }
        }
        equipas.add(equipa);
        return equipa;
    }

    @Override
    public List<Equipa> buscarPorTreinador(Treinador treinador) {
        List<Equipa> resultado = new ArrayList<>();
        for (Equipa equipa : equipas) {
            if (equipa.getTreinador() != null && equipa.getTreinador().equals(treinador)) {
                resultado.add(equipa);
            }
        }
        return resultado;
    }

    @Override
    public Optional<Equipa> buscarPorIdEGestor(UUID id, UUID gestorId) {
        for (Equipa equipa : equipas) {
            if (equipa.getId().equals(id) && pertenceAoGestor(equipa, gestorId)) {
                return Optional.of(equipa);
            }
        }
        return Optional.empty();
    }

    private boolean pertenceAoGestor(Equipa equipa, UUID gestorId) {
        return equipa.getLiga() != null
                && equipa.getLiga().getGestor() != null
                && equipa.getLiga().getGestor().getId().equals(gestorId);
    }
}
