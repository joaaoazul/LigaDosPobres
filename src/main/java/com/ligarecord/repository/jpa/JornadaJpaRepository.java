package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JornadaJpaRepository extends JpaRepository<Jornada, UUID> {

    List<Jornada> findByLigaOrderByNumJornada(Liga liga);

    @EntityGraph(attributePaths = {"resultadoJ", "resultadoJ.equipa"})
    Optional<Jornada> findByIdAndLigaGestorId(UUID id, UUID gestorId);
}
