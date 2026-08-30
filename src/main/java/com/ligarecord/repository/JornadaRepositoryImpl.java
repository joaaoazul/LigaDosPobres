package com.ligarecord.repository;

import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JornadaRepositoryImpl implements JornadaRepository {

    private final List<Jornada> jornadas = new ArrayList<>();

    @Override
    public Jornada guardar(Jornada jornada) {
        for (int i = 0; i < jornadas.size(); i++) {
            if (jornadas.get(i).getId().equals(jornada.getId())) {
                jornadas.set(i, jornada);
                return jornada;
            }
        }
        jornadas.add(jornada);
        return jornada;
    }

    @Override
    public List<Jornada> listarJornadas(Liga liga) {
        List<Jornada> resultado = new ArrayList<>();
        if (liga == null) {
            return resultado;
        }
        for (Jornada jornada : jornadas) {
            if (jornada.getLiga() != null && jornada.getLiga().getId().equals(liga.getId())) {
                resultado.add(jornada);
            }
        }
        return resultado;
    }

    @Override
    public Optional<Jornada> buscarPorIdEGestor(UUID id, UUID gestorId) {
        for (Jornada jornada : jornadas) {
            if (jornada.getId().equals(id) && pertenceAoGestor(jornada, gestorId)) {
                return Optional.of(jornada);
            }
        }
        return Optional.empty();
    }

    private boolean pertenceAoGestor(Jornada jornada, UUID gestorId) {
        return jornada.getLiga() != null
                && jornada.getLiga().getGestor() != null
                && jornada.getLiga().getGestor().getId().equals(gestorId);
    }
}
