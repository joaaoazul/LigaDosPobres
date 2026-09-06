package com.ligarecord.repository;

import com.ligarecord.domain.Jornada;

import java.util.Optional;
import java.util.UUID;

public class JornadaRepositoryImpl extends RepositorioEmMemoria<Jornada> implements JornadaRepository {

    @Override
    public Jornada guardar(Jornada jornada) {
        return super.guardar(jornada);
    }

    @Override
    public Optional<Jornada> buscarPorIdEGestor(UUID id, UUID gestorId) {
        return entidades.stream()
                .filter(jornada -> jornada.getId().equals(id) && pertenceAoGestor(jornada, gestorId))
                .findFirst();
    }

    private boolean pertenceAoGestor(Jornada jornada, UUID gestorId) {
        return jornada.getLiga() != null
                && jornada.getLiga().getGestor() != null
                && jornada.getLiga().getGestor().getId().equals(gestorId);
    }
}
