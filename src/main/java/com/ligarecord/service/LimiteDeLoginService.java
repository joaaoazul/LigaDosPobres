package com.ligarecord.service;

import com.ligarecord.repository.TentativaLoginRepository;
import com.ligarecord.domain.TentativaLoginFalhada;
import com.ligarecord.web.LoginBloqueadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Trava quem vai adivinhando a password de uma conta a golpes de tentativa.
 *
 * <p>Conta por email, não por conta — a mesma decisão do
 * {@link RecuperacaoService}: um email sem conta nenhuma tem de esgotar as
 * tentativas tão depressa como um que existe, senão o próprio bloqueio virava
 * uma forma de descobrir quais os emails com conta aqui.
 */
@Service
public class LimiteDeLoginService {

    private static final Logger LOG = LoggerFactory.getLogger(LimiteDeLoginService.class);

    static final Duration JANELA_LIMITE = Duration.ofMinutes(15);
    static final int MAXIMO_POR_JANELA = 5;

    private final TentativaLoginRepository tentativaRepository;

    public LimiteDeLoginService(TentativaLoginRepository tentativaRepository) {
        this.tentativaRepository = tentativaRepository;
    }

    /**
     * Chamado antes de sequer tentar autenticar. Lança
     * {@link LoginBloqueadoException} se este email já esgotou as tentativas
     * na janela — sem chegar a gastar um BCrypt com a password que veio.
     */
    @Transactional(readOnly = true)
    public void garantirNaoBloqueado(String email) {
        String normalizado = normalizado(email);
        Instant desde = Instant.now().minus(JANELA_LIMITE);
        if (tentativaRepository.contarDoEmailDesde(normalizado, desde) >= MAXIMO_POR_JANELA) {
            LOG.warn("Login bloqueado por excesso de tentativas para {}.", normalizado);
            throw new LoginBloqueadoException(
                    "Demasiadas tentativas com este email. Espera uns minutos e tenta outra vez.");
        }
    }

    /** Chamado depois de uma tentativa de login falhada, seja qual for o motivo. */
    @Transactional
    public void registarFalha(String email) {
        tentativaRepository.guardar(new TentativaLoginFalhada(UUID.randomUUID(), normalizado(email)));
    }

    /**
     * O mesmo limite das colunas de email de gestor e de treinador
     * ({@link RegrasDeConta#MAXIMO_EMAIL}). Cortado aqui, não só na coluna: um
     * email gigante sem isto ainda chegava à base de dados antes de esbarrar
     * no limite, e o gestor via "Isto foi alterado por outro pedido ao mesmo
     * tempo" — uma queixa sobre uma corrida, para quem só mandou um campo
     * grande de mais.
     */
    private String normalizado(String email) {
        String limpo = email == null ? "" : email.trim().toLowerCase();
        return limpo.length() > RegrasDeConta.MAXIMO_EMAIL ? limpo.substring(0, RegrasDeConta.MAXIMO_EMAIL) : limpo;
    }
}
