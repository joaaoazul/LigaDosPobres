package com.ligarecord.repository;

import com.ligarecord.domain.ConviteTreinador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConviteTreinadorRepositoryImpl implements ConviteTreinadorRepository {

    private final List<ConviteTreinador> convites = new ArrayList<>();

    @Override
    public ConviteTreinador guardar(ConviteTreinador convite) {
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
    public Optional<ConviteTreinador> buscarPorCodigo(String codigo) {
        for (ConviteTreinador convite : convites) {
            if (convite.getCodigo().equals(codigo)) {
                return Optional.of(convite);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ConviteTreinador> buscarPorIdEGestor(UUID id, UUID gestorId) {
        for (ConviteTreinador convite : convites) {
            if (convite.getId().equals(id) && convite.getCriadoPor().getId().equals(gestorId)) {
                return Optional.of(convite);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ConviteTreinador> listarPorGestor(UUID gestorId) {
        List<ConviteTreinador> resultado = new ArrayList<>();
        for (ConviteTreinador convite : convites) {
            if (convite.getCriadoPor().getId().equals(gestorId)) {
                resultado.add(convite);
            }
        }
        return resultado;
    }
}
