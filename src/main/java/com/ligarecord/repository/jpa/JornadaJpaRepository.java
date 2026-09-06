package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Jornada;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JornadaJpaRepository extends JpaRepository<Jornada, UUID> {

    @EntityGraph(attributePaths = {"resultadoJ", "resultadoJ.equipa"})
    Optional<Jornada> findByIdAndLigaGestorId(UUID id, UUID gestorId);
}
