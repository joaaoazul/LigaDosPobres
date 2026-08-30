package com.ligarecord.config;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.enums.PapelGestor;
import com.ligarecord.repository.GestorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cria o primeiro administrador numa base de dados vazia.
 *
 * <p>Sem isto haveria um impasse no primeiro arranque: não há convites porque
 * não há quem os crie, e não há administrador porque não há como registar-se.
 *
 * <p>Só age quando não existe nenhum administrador ativo. Definir as variáveis
 * numa instalação que já tem administradores não faz nada, portanto não serve
 * para repor acessos nem para criar contas às escondidas.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final GestorRepository gestorRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(GestorRepository gestorRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.gestorRepository = gestorRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.password = password == null ? "" : password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (gestorRepository.contarAdminsAtivos() > 0) {
            return;
        }

        if (email.isEmpty() || password.isEmpty()) {
            log.warn("Não existe nenhum administrador e ADMIN_EMAIL/ADMIN_PASSWORD não estão "
                    + "definidos. Ninguém consegue criar convites nem registar-se.");
            return;
        }

        if (password.length() < 10) {
            log.error("ADMIN_PASSWORD tem menos de 10 caracteres. O administrador não foi criado.");
            return;
        }

        // Se a conta já existe mas não é administradora, é promovida em vez de duplicada.
        Gestor gestor = gestorRepository.buscarPorEmail(email)
                .map(existente -> {
                    existente.setPapel(PapelGestor.ADMIN);
                    existente.setAtivo(true);
                    log.info("Conta existente promovida a administrador.");
                    return existente;
                })
                .orElseGet(() -> {
                    log.info("Criado o primeiro administrador a partir de ADMIN_EMAIL.");
                    return new Gestor(UUID.randomUUID(), email,
                            passwordEncoder.encode(password), "Administrador", PapelGestor.ADMIN);
                });

        gestorRepository.guardar(gestor);
        log.warn("Muda a password do administrador depois de entrares, e remove ADMIN_PASSWORD "
                + "do ambiente.");
    }
}
