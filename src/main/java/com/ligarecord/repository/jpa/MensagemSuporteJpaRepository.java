package com.ligarecord.repository.jpa;

import com.ligarecord.domain.MensagemSuporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface MensagemSuporteJpaRepository extends JpaRepository<MensagemSuporte, UUID> {

    long countByIpAndCriadoEmAfter(String ip, Instant desde);
}
