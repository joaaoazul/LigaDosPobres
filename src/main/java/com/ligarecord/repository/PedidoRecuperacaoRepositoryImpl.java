package com.ligarecord.repository;

import com.ligarecord.domain.PedidoRecuperacao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class PedidoRecuperacaoRepositoryImpl extends RepositorioEmMemoria<PedidoRecuperacao>
        implements PedidoRecuperacaoRepository {

    @Override
    public PedidoRecuperacao guardar(PedidoRecuperacao pedido) {
        return super.guardar(pedido);
    }

    @Override
    public Optional<PedidoRecuperacao> buscarPorCodigoHash(String codigoHash) {
        return entidades.stream()
                .filter(pedido -> pedido.getCodigoHash().equals(codigoHash))
                .findFirst();
    }

    @Override
    public long contarDoGestorDesde(UUID gestorId, Instant desde) {
        return entidades.stream()
                .filter(pedido -> pedido.getGestor().getId().equals(gestorId))
                .filter(pedido -> pedido.getCriadoEm().isAfter(desde))
                .count();
    }
}
