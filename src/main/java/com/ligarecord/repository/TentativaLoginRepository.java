package com.ligarecord.repository;

import com.ligarecord.domain.TentativaLoginFalhada;

import java.time.Instant;

public interface TentativaLoginRepository {

    TentativaLoginFalhada guardar(TentativaLoginFalhada tentativa);

    /**
     * Quantas tentativas falhadas houve para este email desde um dado
     * momento. Serve para travar quem vai adivinhando passwords.
     */
    long contarDoEmailDesde(String email, Instant desde);
}
