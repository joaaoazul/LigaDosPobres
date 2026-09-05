package com.ligarecord.repository.jpa;

import com.ligarecord.domain.RegraDivida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegraDividaJpaRepository extends JpaRepository<RegraDivida, UUID> {

    Optional<RegraDivida> findByLigaId(UUID ligaId);
}
