package com.ligarecord.repository;

import com.ligarecord.domain.TentativaLoginFalhada;

import java.time.Instant;

public class TentativaLoginRepositoryImpl extends RepositorioEmMemoria<TentativaLoginFalhada>
        implements TentativaLoginRepository {

    @Override
    public TentativaLoginFalhada guardar(TentativaLoginFalhada tentativa) {
        return super.guardar(tentativa);
    }

    @Override
    public long contarDoEmailDesde(String email, Instant desde) {
        return entidades.stream()
                .filter(tentativa -> tentativa.getEmail().equals(email))
                .filter(tentativa -> tentativa.getCriadoEm().isAfter(desde))
                .count();
    }
}
