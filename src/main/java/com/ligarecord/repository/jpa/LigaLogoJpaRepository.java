package com.ligarecord.repository.jpa;

import com.ligarecord.domain.LigaLogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LigaLogoJpaRepository extends JpaRepository<LigaLogo, UUID> {
}
