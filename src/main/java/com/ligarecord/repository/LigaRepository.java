package com.ligarecord.repository;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigaRepository {

    Liga guardarLiga(Liga liga);

    /** Apenas as ligas deste gestor. */
    List<Liga> listarLigas(Gestor gestor);

    /**
     * Procura filtrada pelo dono. Quem se esquecer de verificar a autorização
     * obtém um resultado vazio, não os dados de outro gestor.
     */
    Optional<Liga> buscarPorIdEGestor(UUID id, UUID gestorId);
}
