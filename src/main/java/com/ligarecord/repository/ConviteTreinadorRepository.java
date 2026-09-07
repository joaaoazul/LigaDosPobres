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
     * Sem filtro nenhum: só é chamado com um id que a transação anterior
     * acabou de emitir, para o reler já noutra — a que envia o email.
     */
    Optional<ConviteTreinador> buscarPorId(UUID id);

    /**
     * Filtrado pela equipa a que o convite pertence, e não pelo gestor que o
     * criou: o convite é do lugar, e quem manda nele é quem manda na equipa
     * hoje — que pode não ser quem o emitiu, se a liga entretanto mudou de
     * gestor. Quem se esquecer de verificar a autorização obtém um resultado
     * vazio, não o convite de outra equipa.
     */
    Optional<ConviteTreinador> buscarPorIdEEquipa(UUID id, UUID equipaId);

    /** Por usar, por revogar — a expiração é decidida em código, com o relógio da aplicação. */
    List<ConviteTreinador> listarPendentesPorTreinador(UUID treinadorId);

    /** Os pendentes de uma liga inteira, para a tabela de equipas do gestor. */
    List<ConviteTreinador> listarPendentesPorLiga(UUID ligaId);
}
