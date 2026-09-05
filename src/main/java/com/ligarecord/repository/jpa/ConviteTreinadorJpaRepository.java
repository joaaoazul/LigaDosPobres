package com.ligarecord.repository.jpa;

import com.ligarecord.domain.ConviteTreinador;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteTreinadorJpaRepository extends JpaRepository<ConviteTreinador, UUID> {

    @EntityGraph(attributePaths = {"treinador"})
    Optional<ConviteTreinador> findByCodigo(String codigo);

    /** O caminho criadoPor.id faz a autorização acontecer dentro do SQL. */
    Optional<ConviteTreinador> findByIdAndCriadoPorId(UUID id, UUID gestorId);

    @EntityGraph(attributePaths = {"treinador", "criadoPor", "usadoPor"})
    List<ConviteTreinador> findByCriadoPorIdOrderByCriadoEmDesc(UUID gestorId);
}
