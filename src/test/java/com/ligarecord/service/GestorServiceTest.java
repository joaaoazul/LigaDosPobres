package com.ligarecord.service;

import com.ligarecord.domain.Convite;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.enums.PapelGestor;
import com.ligarecord.repository.ContaTreinadorRepository;
import com.ligarecord.repository.ContaTreinadorRepositoryImpl;
import com.ligarecord.repository.ConviteRepository;
import com.ligarecord.repository.ConviteRepositoryImpl;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.GestorRepositoryImpl;
import com.ligarecord.web.ConviteInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GestorServiceTest {

    private GestorRepository gestorRepository;
    private ContaTreinadorRepository contaTreinadorRepository;
    private ConviteRepository conviteRepository;
    private PasswordEncoder passwordEncoder;
    private ConviteService conviteService;
    private GestorService gestorService;
    private Gestor admin;

    @BeforeEach
    void setUp() {
        gestorRepository = new GestorRepositoryImpl();
        contaTreinadorRepository = new ContaTreinadorRepositoryImpl();
        conviteRepository = new ConviteRepositoryImpl();
        passwordEncoder = new BCryptPasswordEncoder();
        conviteService = new ConviteService(conviteRepository);
        gestorService = new GestorService(
                gestorRepository, contaTreinadorRepository, conviteService, passwordEncoder);

        admin = new Gestor(UUID.randomUUID(), "admin@teste.pt", "hash", "Admin", PapelGestor.ADMIN);
        gestorRepository.guardar(admin);
    }

    private String convite() {
        return conviteService.criar(admin, null, null).getCodigo();
    }

    @Test
    void deveRegistarComConviteValido() {
        Gestor gestor = gestorService.registar("Joao@Exemplo.PT", "passwordsegura1", "João", convite());

        assertNotNull(gestor.getId());
        // o email é normalizado para evitar contas duplicadas com maiúsculas diferentes
        assertEquals("joao@exemplo.pt", gestor.getEmail());
        assertEquals(PapelGestor.GESTOR, gestor.getPapel());
        assertTrue(gestor.isAtivo());
    }

    @Test
    void naoDeveGuardarAPasswordEmClaro() {
        Gestor gestor = gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", convite());

        assertNotEquals("passwordsegura1", gestor.getPasswordHash());
        assertTrue(passwordEncoder.matches("passwordsegura1", gestor.getPasswordHash()));
    }

    @Test
    void conviteSoServeUmaVez() {
        String codigo = convite();
        gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", codigo);

        assertThrows(
                ConviteInvalidoException.class,
                () -> gestorService.registar("outro@exemplo.pt", "passwordsegura1", "Outro", codigo)
        );
    }

    @Test
    void naoDeveRegistarComConviteInexistente() {
        assertThrows(
                ConviteInvalidoException.class,
                () -> gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", "inventado")
        );
    }

    @Test
    void naoDeveRegistarSemConvite() {
        assertThrows(
                ConviteInvalidoException.class,
                () -> gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", null)
        );
    }

    @Test
    void naoDeveRegistarComConviteRevogado() {
        Convite convite = conviteService.criar(admin, "para ninguém", null);
        conviteService.revogar(convite.getId());

        assertThrows(
                ConviteInvalidoException.class,
                () -> gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", convite.getCodigo())
        );
    }

    @Test
    void conviteInexistenteEUsadoDaoAMesmaResposta() {
        String codigo = convite();
        gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", codigo);

        ConviteInvalidoException usado = assertThrows(ConviteInvalidoException.class,
                () -> gestorService.registar("a@exemplo.pt", "passwordsegura1", "A", codigo));
        ConviteInvalidoException inexistente = assertThrows(ConviteInvalidoException.class,
                () -> gestorService.registar("b@exemplo.pt", "passwordsegura1", "B", "nao-existe"));

        assertEquals(usado.getMessage(), inexistente.getMessage());
    }

    @Test
    void naoDeveRegistarPasswordCurta() {
        assertThrows(
                IllegalArgumentException.class,
                () -> gestorService.registar("joao@exemplo.pt", "curta", "João", convite())
        );
    }

    @Test
    void naoDeveRegistarEmailRepetido() {
        gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", convite());

        assertThrows(
                IllegalStateException.class,
                () -> gestorService.registar("JOAO@exemplo.pt", "passwordsegura2", "Outro", convite())
        );
    }

    @Test
    void codigosDeConviteNaoSeRepetem() {
        assertNotEquals(convite(), convite());
    }
}
