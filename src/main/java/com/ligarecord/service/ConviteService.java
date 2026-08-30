package com.ligarecord.service;

import com.ligarecord.domain.Convite;
import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.ConviteRepository;
import com.ligarecord.web.ConviteInvalidoException;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ConviteService {

    /** 24 bytes: demasiado grande para ser adivinhado por tentativa e erro. */
    private static final int BYTES_CODIGO = 24;

    private static final int VALIDADE_MAXIMA_DIAS = 365;

    private final SecureRandom aleatorio = new SecureRandom();
    private final ConviteRepository conviteRepository;

    public ConviteService(ConviteRepository conviteRepository) {
        this.conviteRepository = conviteRepository;
    }

    @Transactional
    public Convite criar(Gestor admin, String nota, Integer diasValidade) {
        if (admin == null) {
            throw new IllegalArgumentException("O convite tem de ter um autor.");
        }
        if (diasValidade != null && (diasValidade < 1 || diasValidade > VALIDADE_MAXIMA_DIAS)) {
            throw new IllegalArgumentException(
                    "A validade tem de estar entre 1 e " + VALIDADE_MAXIMA_DIAS + " dias.");
        }

        Instant expiraEm = diasValidade == null
                ? null
                : Instant.now().plus(diasValidade, ChronoUnit.DAYS);

        Convite convite = new Convite(
                UUID.randomUUID(),
                gerarCodigo(),
                nota == null || nota.isBlank() ? null : nota.trim(),
                admin,
                expiraEm
        );

        return conviteRepository.guardar(convite);
    }

    @Transactional(readOnly = true)
    public List<Convite> listar() {
        return conviteRepository.listarTodos();
    }

    @Transactional
    public Convite revogar(UUID id) {
        Convite convite = conviteRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado."));

        if (convite.estaUsado()) {
            throw new IllegalStateException("Este convite já foi usado e não pode ser revogado.");
        }
        if (convite.estaRevogado()) {
            throw new IllegalStateException("Este convite já estava revogado.");
        }

        convite.revogar();
        return conviteRepository.guardar(convite);
    }

    /**
     * Valida e consome o convite. Chamado durante o registo, dentro da mesma
     * transação que cria a conta: se a criação falhar, o convite não fica gasto.
     */
    @Transactional
    public Convite consumir(String codigo, Gestor gestor) {
        if (codigo == null || codigo.isBlank()) {
            throw new ConviteInvalidoException("Código de convite inválido.");
        }

        Convite convite = conviteRepository.buscarPorCodigo(codigo.trim())
                // mensagem igual à de um convite gasto: não distingue "não existe" de "já usado"
                .orElseThrow(() -> new ConviteInvalidoException("Código de convite inválido."));

        if (!convite.estaDisponivel()) {
            throw new ConviteInvalidoException("Código de convite inválido.");
        }

        convite.marcarUsado(gestor);
        return conviteRepository.guardar(convite);
    }

    private String gerarCodigo() {
        byte[] bytes = new byte[BYTES_CODIGO];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
