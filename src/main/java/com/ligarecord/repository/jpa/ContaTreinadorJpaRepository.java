package com.ligarecord.repository.jpa;

import com.ligarecord.domain.ContaTreinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContaTreinadorJpaRepository extends JpaRepository<ContaTreinador, UUID> {

    Optional<ContaTreinador> findByEmailIgnoreCase(String email);

    boolean existsByTreinadorId(UUID treinadorId);
}
