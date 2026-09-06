package com.ligarecord.repository.jpa;

import com.ligarecord.domain.PedidoRecuperacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRecuperacaoJpaRepository extends JpaRepository<PedidoRecuperacao, UUID> {

    /** O gestor vem junto: quem valida o código precisa logo da conta a seguir. */
    @EntityGraph(attributePaths = {"gestor"})
    Optional<PedidoRecuperacao> findByCodigoHash(String codigoHash);

    long countByGestorIdAndCriadoEmAfter(UUID gestorId, Instant desde);
}
