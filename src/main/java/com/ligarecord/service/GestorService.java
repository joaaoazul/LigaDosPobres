package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.GestorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class GestorService {

    /** Validação deliberadamente permissiva: só rejeita o que é obviamente inválido. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final int MINIMO_PASSWORD = 10;

    private final GestorRepository gestorRepository;
    private final ConviteService conviteService;
    private final PasswordEncoder passwordEncoder;

    public GestorService(GestorRepository gestorRepository,
                         ConviteService conviteService,
                         PasswordEncoder passwordEncoder) {
        this.gestorRepository = gestorRepository;
        this.conviteService = conviteService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria uma conta a partir de um convite válido, que fica consumido.
     *
     * <p>Tudo corre na mesma transação: se a criação da conta falhar depois do
     * convite ser marcado, nada é gravado e o convite continua utilizável.
     */
    @Transactional
    public Gestor registar(String email, String password, String nome, String codigo) {
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

        conviteService.consumir(codigo, gestor);

        return gestorRepository.guardar(gestor);
    }
}
