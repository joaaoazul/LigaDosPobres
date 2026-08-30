package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Convite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteJpaRepository extends JpaRepository<Convite, UUID> {

    Optional<Convite> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"criadoPor", "usadoPor"})
    List<Convite> findAllByOrderByCriadoEmDesc();
}
