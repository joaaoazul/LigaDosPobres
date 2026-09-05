package com.ligarecord.repository;

import com.ligarecord.domain.ContaTreinador;

import java.util.Optional;
import java.util.UUID;

public interface ContaTreinadorRepository {

    ContaTreinador guardar(ContaTreinador conta);

    /** Usado na autenticação. O email é comparado sem distinguir maiúsculas. */
    Optional<ContaTreinador> buscarPorEmail(String email);

    Optional<ContaTreinador> buscarPorId(UUID id);

    /**
     * Um treinador tem no máximo uma conta. A base de dados também o impõe;
     * isto existe para a recusa ser uma mensagem clara e não uma violação de
     * restrição a chegar ao utilizador como erro interno.
     */
    boolean existePorTreinador(UUID treinadorId);
}
