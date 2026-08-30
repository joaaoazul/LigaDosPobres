package com.ligarecord.repository;

import com.ligarecord.domain.Gestor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GestorRepositoryImpl implements GestorRepository {

    private final List<Gestor> gestores = new ArrayList<>();

    @Override
    public Gestor guardar(Gestor gestor) {
        for (int i = 0; i < gestores.size(); i++) {
            if (gestores.get(i).getId().equals(gestor.getId())) {
                gestores.set(i, gestor);
                return gestor;
            }
        }
        gestores.add(gestor);
        return gestor;
    }

    @Override
    public Optional<Gestor> buscarPorEmail(String email) {
        for (Gestor gestor : gestores) {
            if (gestor.getEmail().equalsIgnoreCase(email)) {
                return Optional.of(gestor);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Gestor> buscarPorId(UUID id) {
        for (Gestor gestor : gestores) {
            if (gestor.getId().equals(id)) {
                return Optional.of(gestor);
            }
        }
        return Optional.empty();
    }
}
