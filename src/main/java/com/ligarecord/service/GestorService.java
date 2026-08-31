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

    /**
     * Muda a password do próprio gestor.
     *
     * <p>Exige a password actual mesmo estando a sessão aberta: sem isso, quem
     * apanhasse um computador destrancado trocava a password e ficava dono da
     * conta. É também a razão para não haver aqui um caminho de administrador —
     * ver a nota no fim.
     */
    @Transactional
    public void alterarPassword(UUID gestorId, String atual, String nova) {
        Gestor gestor = gestorRepository.buscarPorId(gestorId)
                .orElseThrow(() -> new IllegalArgumentException("A conta não existe."));

        if (atual == null || !passwordEncoder.matches(atual, gestor.getPasswordHash())) {
            // Mensagem própria: aqui já sabemos quem é o utilizador, por isso não
            // há nada a proteger em ser-se vago, e ser-se vago só o confundiria.
            throw new IllegalArgumentException("A password actual não está correcta.");
        }
        if (nova == null || nova.length() < MINIMO_PASSWORD) {
            throw new IllegalArgumentException(
                    "A password nova tem de ter pelo menos " + MINIMO_PASSWORD + " caracteres.");
        }
        if (passwordEncoder.matches(nova, gestor.getPasswordHash())) {
            throw new IllegalArgumentException("A password nova tem de ser diferente da actual.");
        }

        gestor.setPasswordHash(passwordEncoder.encode(nova));
        gestorRepository.guardar(gestor);
    }
}
