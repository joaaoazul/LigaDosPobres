package com.ligarecord.repository;

import com.ligarecord.domain.ConviteTreinador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteTreinadorRepository {

    ConviteTreinador guardar(ConviteTreinador convite);

    /** Sem filtro por dono de propósito: quem se regista não tem sessão nenhuma. */
    Optional<ConviteTreinador> buscarPorCodigo(String codigo);

    /**
     * Filtrado pelo gestor que criou o convite. Quem se esquecer de verificar a
     * autorização obtém um resultado vazio, não o convite de outro gestor.
     */
    Optional<ConviteTreinador> buscarPorIdEGestor(UUID id, UUID gestorId);

    List<ConviteTreinador> listarPorGestor(UUID gestorId);
}
