package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Treinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TreinadorJpaRepository extends JpaRepository<Treinador, UUID> {
}
