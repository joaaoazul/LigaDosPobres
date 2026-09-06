package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Divida;
import com.ligarecord.domain.enums.EstadoDivida;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DividaJpaRepository extends JpaRepository<Divida, UUID> {

    @EntityGraph(attributePaths = {"blocos"})
    Optional<Divida> findByEquipaId(UUID equipaId);

    @EntityGraph(attributePaths = {"blocos", "equipa"})
    List<Divida> findByEquipaLigaIdAndEstado(UUID ligaId, EstadoDivida estado);

    /** Os blocos vêm no mesmo pedido: é sobre eles que o pote da liga é somado. */
    @EntityGraph(attributePaths = {"blocos"})
    List<Divida> findByEquipaLigaId(UUID ligaId);

    @EntityGraph(attributePaths = {"blocos", "equipa", "equipa.liga"})
    List<Divida> findByEquipaTreinadorContaIdOrderByEquipaLigaNomeAscEquipaNomeAsc(UUID contaId);
}
