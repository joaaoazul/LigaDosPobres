package com.ligarecord.repository.jpa;

import com.ligarecord.domain.ConviteTreinador;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteTreinadorJpaRepository extends JpaRepository<ConviteTreinador, UUID> {

    @EntityGraph(attributePaths = {"treinador", "equipa", "criadoPor"})
    Optional<ConviteTreinador> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"treinador", "equipa", "criadoPor"})
    Optional<ConviteTreinador> findWithDadosById(UUID id);

    /** O caminho equipa.id faz a autorização acontecer dentro do SQL. */
    Optional<ConviteTreinador> findByIdAndEquipaId(UUID id, UUID equipaId);

    @EntityGraph(attributePaths = {"treinador", "equipa"})
    List<ConviteTreinador> findByTreinadorIdAndUsadoEmIsNullAndRevogadoEmIsNullOrderByCriadoEmDesc(
            UUID treinadorId);

    @EntityGraph(attributePaths = {"treinador", "equipa"})
    List<ConviteTreinador> findByEquipaLigaIdAndUsadoEmIsNullAndRevogadoEmIsNullOrderByCriadoEmDesc(
            UUID ligaId);
}
