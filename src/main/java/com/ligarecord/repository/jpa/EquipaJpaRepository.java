package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Treinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipaJpaRepository extends JpaRepository<Equipa, UUID> {

    List<Equipa> findByTreinador(Treinador treinador);

    /** O caminho liga.gestor.id faz a autorização acontecer dentro do SQL. */
    Optional<Equipa> findByIdAndLigaGestorId(UUID id, UUID gestorId);
}
