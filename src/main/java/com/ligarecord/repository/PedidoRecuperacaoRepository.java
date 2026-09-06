package com.ligarecord.repository;

import com.ligarecord.domain.PedidoRecuperacao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRecuperacaoRepository {

    PedidoRecuperacao guardar(PedidoRecuperacao pedido);

    /** Sem filtro por dono: quem recupera a password não tem sessão nenhuma. */
    Optional<PedidoRecuperacao> buscarPorCodigoHash(String codigoHash);

    /**
     * Quantos pedidos foram criados para esta conta desde um dado momento.
     * Serve para travar quem carregue no botão de recuperar dezenas de vezes e
     * encha a caixa de correio de outra pessoa.
     */
    long contarDoGestorDesde(UUID gestorId, Instant desde);
}
