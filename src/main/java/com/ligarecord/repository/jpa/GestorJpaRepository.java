package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Gestor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GestorJpaRepository extends JpaRepository<Gestor, UUID> {
    Optional<Gestor> findByEmailIgnoreCase(String email);
}
