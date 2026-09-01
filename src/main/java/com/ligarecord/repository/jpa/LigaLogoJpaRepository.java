package com.ligarecord.repository.jpa;

import com.ligarecord.domain.LigaLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LigaLogoJpaRepository extends JpaRepository<LigaLogo, UUID> {

    /**
     * Gravar pelo {@code save()} herdado seria um {@code merge}: como o id vem
     * sempre preenchido, o Spring Data conclui que a entidade não é nova e faz
     * primeiro um SELECT — que traz o logo anterior (até 1 MB) para memória só
     * para o substituir a seguir. Este upsert é uma instrução e não lê nada.
     */
    @Modifying
    @Query(value = """
            insert into liga_logo (liga_id, dados) values (:ligaId, :dados)
            on conflict (liga_id) do update set dados = excluded.dados
            """, nativeQuery = true)
    void guardar(@Param("ligaId") UUID ligaId, @Param("dados") byte[] dados);

    /**
     * Pela mesma razão, apagar pelo {@code deleteById()} herdado faria
     * {@code findById(...).ifPresent(this::delete)} — outra leitura do blob
     * inteiro só para o apagar.
     */
    @Modifying
    @Query(value = "delete from liga_logo where liga_id = :ligaId", nativeQuery = true)
    void apagarPorLiga(@Param("ligaId") UUID ligaId);
}
