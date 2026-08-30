package com.ligarecord.repository;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Treinador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EquipaRepositoryImpl implements EquipaRepository {
    List<Equipa> equipas = new ArrayList<>();


    @Override
    public Equipa guardar(Equipa equipa) {
        boolean found = false;
        for (int i = 0; i < equipas.size(); i++) {
            if (equipas.get(i).getId().equals(equipa.getId())) {
                equipas.set(i, equipa);
                found = true;
                break;
            }
        }

        if (!found) {
            equipas.add(equipa);
        }
        return equipa;
    }

    @Override
    public List<Equipa> buscarPorTreinador(Treinador treinador){

        List<Equipa> resultado = new ArrayList<>();

        for (int i = 0; i < equipas.size(); i++) {
            if (equipas.get(i).getTreinador().equals(treinador)) {
                resultado.add(equipas.get(i));
            }
        }
        return resultado;
    }

    @Override
    public Optional<Equipa> buscarPorId(UUID id) {
        for (Equipa equipa : equipas) {
            if (equipa.getId().equals(id)) {
                return Optional.of(equipa);
            }
        }
        return Optional.empty();
    }
}
