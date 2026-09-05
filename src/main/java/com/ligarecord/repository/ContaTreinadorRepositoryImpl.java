package com.ligarecord.repository;

import com.ligarecord.domain.ContaTreinador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ContaTreinadorRepositoryImpl implements ContaTreinadorRepository {

    private final List<ContaTreinador> contas = new ArrayList<>();

    @Override
    public ContaTreinador guardar(ContaTreinador conta) {
        for (int i = 0; i < contas.size(); i++) {
            if (contas.get(i).getId().equals(conta.getId())) {
                contas.set(i, conta);
                return conta;
            }
        }
        contas.add(conta);
        return conta;
    }

    @Override
    public Optional<ContaTreinador> buscarPorEmail(String email) {
        for (ContaTreinador conta : contas) {
            if (conta.getEmail().equalsIgnoreCase(email)) {
                return Optional.of(conta);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ContaTreinador> buscarPorId(UUID id) {
        for (ContaTreinador conta : contas) {
            if (conta.getId().equals(id)) {
                return Optional.of(conta);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean existePorTreinador(UUID treinadorId) {
        for (ContaTreinador conta : contas) {
            if (conta.getTreinador().getId().equals(treinadorId)) {
                return true;
            }
        }
        return false;
    }
}
