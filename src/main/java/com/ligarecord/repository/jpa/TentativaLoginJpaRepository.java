package com.ligarecord.repository.jpa;

import com.ligarecord.domain.TentativaLoginFalhada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface TentativaLoginJpaRepository extends JpaRepository<TentativaLoginFalhada, UUID> {

    long countByEmailAndCriadoEmAfter(String email, Instant desde);
}
