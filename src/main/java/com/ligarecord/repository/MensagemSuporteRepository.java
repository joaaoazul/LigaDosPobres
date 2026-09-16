package com.ligarecord.repository;

import com.ligarecord.domain.MensagemSuporte;

import java.time.Instant;

public interface MensagemSuporteRepository {

    MensagemSuporte guardar(MensagemSuporte mensagem);

    /**
     * Quantas mensagens vieram desta origem desde um dado momento. Serve para
     * travar quem despeje o formulário de contacto em cima da caixa de correio
     * do suporte.
     */
    long contarDoIpDesde(String ip, Instant desde);
}
