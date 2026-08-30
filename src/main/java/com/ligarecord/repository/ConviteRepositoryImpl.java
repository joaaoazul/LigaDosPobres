package com.ligarecord.repository;

import com.ligarecord.domain.Convite;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConviteRepositoryImpl implements ConviteRepository {

    private final List<Convite> convites = new ArrayList<>();

    @Override
    public Convite guardar(Convite convite) {
        for (int i = 0; i < convites.size(); i++) {
            if (convites.get(i).getId().equals(convite.getId())) {
                convites.set(i, convite);
                return convite;
            }
        }
        convites.add(convite);
        return convite;
    }

    @Override
    public Optional<Convite> buscarPorCodigo(String codigo) {
        for (Convite convite : convites) {
            if (convite.getCodigo().equals(codigo)) {
                return Optional.of(convite);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Convite> buscarPorId(UUID id) {
        for (Convite convite : convites) {
            if (convite.getId().equals(id)) {
                return Optional.of(convite);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Convite> listarTodos() {
        return new ArrayList<>(convites);
    }
}
