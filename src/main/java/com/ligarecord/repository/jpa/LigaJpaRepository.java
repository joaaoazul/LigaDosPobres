package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Liga;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigaJpaRepository extends JpaRepository<Liga, UUID> {

    @EntityGraph(attributePaths = {"equipas", "equipas.treinador"})
    List<Liga> findByGestorIdOrderByNome(UUID gestorId);

    /*
     * Sem @EntityGraph: o Hibernate não consegue carregar "equipas" e "jornadas"
     * na mesma consulta (MultipleBagFetchException). As coleções são carregadas
     * dentro da transação de leitura do controller.
     */
    Optional<Liga> findByIdAndGestorId(UUID id, UUID gestorId);
}
