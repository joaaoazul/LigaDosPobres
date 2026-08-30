package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.web.ConviteInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class GestorService {

    private static final Logger log = LoggerFactory.getLogger(GestorService.class);

    /** Validação deliberadamente permissiva: só rejeita o que é obviamente inválido. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final int MINIMO_PASSWORD = 10;

    private final GestorRepository gestorRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Código de convite exigido para criar conta. Vazio significa registo
     * fechado — ninguém se regista até ser configurado.
     */
    private final String codigoConvite;

    public GestorService(GestorRepository gestorRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.registo.codigo:}") String codigoConvite) {
        this.gestorRepository = gestorRepository;
        this.passwordEncoder = passwordEncoder;
        this.codigoConvite = codigoConvite == null ? "" : codigoConvite.trim();

        if (this.codigoConvite.isEmpty()) {
            log.warn("REGISTO_CODIGO não está definido: o registo de novos gestores está fechado.");
        }
    }

    @Transactional
    public Gestor registar(String email, String password, String nome, String codigo) {
        verificarConvite(codigo);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O email é obrigatório.");
        }
        String emailNormalizado = email.trim().toLowerCase();
        if (!EMAIL.matcher(emailNormalizado).matches()) {
            throw new IllegalArgumentException("O email não é válido.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }
        if (password == null || password.length() < MINIMO_PASSWORD) {
            throw new IllegalArgumentException(
                    "A password tem de ter pelo menos " + MINIMO_PASSWORD + " caracteres.");
        }
        if (gestorRepository.buscarPorEmail(emailNormalizado).isPresent()) {
            throw new IllegalStateException("Já existe uma conta com este email.");
        }

        Gestor gestor = new Gestor(
                UUID.randomUUID(),
                emailNormalizado,
                passwordEncoder.encode(password),
                nome.trim()
        );

        return gestorRepository.guardar(gestor);
    }

    /**
     * A verificação é a primeira coisa a acontecer: sem convite válido, nem
     * chegamos a dizer se o email já está registado. Caso contrário o endpoint
     * de registo serviria para descobrir que endereços têm conta.
     */
    private void verificarConvite(String codigo) {
        if (codigoConvite.isEmpty()) {
            throw new ConviteInvalidoException("O registo de novas contas está fechado.");
        }
        if (codigo == null || !iguais(codigo.trim(), codigoConvite)) {
            throw new ConviteInvalidoException("Código de convite inválido.");
        }
    }

    /** Comparação de tempo constante: não revela quantos caracteres acertaram. */
    private boolean iguais(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
