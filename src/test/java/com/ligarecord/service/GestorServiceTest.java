package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.GestorRepositoryImpl;
import com.ligarecord.web.ConviteInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class GestorServiceTest {

    private static final String CONVITE = "convite-de-teste-123";

    private GestorRepository gestorRepository;
    private PasswordEncoder passwordEncoder;
    private GestorService gestorService;

    @BeforeEach
    void setUp() {
        gestorRepository = new GestorRepositoryImpl();
        passwordEncoder = new BCryptPasswordEncoder();
        gestorService = new GestorService(gestorRepository, passwordEncoder, CONVITE);
    }

    @Test
    void deveRegistarComConviteValido() {
        Gestor gestor = gestorService.registar("Joao@Exemplo.PT", "passwordsegura1", "João", CONVITE);

        assertNotNull(gestor.getId());
        assertEquals("João", gestor.getNome());
        // o email é normalizado para evitar contas duplicadas com maiúsculas diferentes
        assertEquals("joao@exemplo.pt", gestor.getEmail());
    }

    @Test
    void naoDeveGuardarAPasswordEmClaro() {
        Gestor gestor = gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", CONVITE);

        assertNotEquals("passwordsegura1", gestor.getPasswordHash());
        assertTrue(gestor.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches("passwordsegura1", gestor.getPasswordHash()));
    }

    @Test
    void naoDeveRegistarSemConvite() {
        assertThrows(
                ConviteInvalidoException.class,
                () -> gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", null)
        );
    }

    @Test
    void naoDeveRegistarComConviteErrado() {
        assertThrows(
                ConviteInvalidoException.class,
                () -> gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", "outro-codigo")
        );
    }

    @Test
    void registoFicaFechadoQuandoNaoHaConviteConfigurado() {
        GestorService semConvite = new GestorService(gestorRepository, passwordEncoder, "");

        assertThrows(
                ConviteInvalidoException.class,
                () -> semConvite.registar("joao@exemplo.pt", "passwordsegura1", "João", "")
        );
    }

    @Test
    void conviteErradoNaoRevelaSeOEmailJaExiste() {
        gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", CONVITE);

        // com convite errado, a resposta é sempre a mesma quer o email exista quer não
        ConviteInvalidoException existente = assertThrows(
                ConviteInvalidoException.class,
                () -> gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", "errado")
        );
        ConviteInvalidoException novo = assertThrows(
                ConviteInvalidoException.class,
                () -> gestorService.registar("outro@exemplo.pt", "passwordsegura1", "Outro", "errado")
        );

        assertEquals(existente.getMessage(), novo.getMessage());
    }

    @Test
    void naoDeveRegistarPasswordCurta() {
        assertThrows(
                IllegalArgumentException.class,
                () -> gestorService.registar("joao@exemplo.pt", "curta", "João", CONVITE)
        );
    }

    @Test
    void naoDeveRegistarEmailRepetido() {
        gestorService.registar("joao@exemplo.pt", "passwordsegura1", "João", CONVITE);

        assertThrows(
                IllegalStateException.class,
                () -> gestorService.registar("JOAO@exemplo.pt", "passwordsegura2", "Outro João", CONVITE)
        );
    }
}
