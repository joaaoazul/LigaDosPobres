package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.enums.PapelGestor;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.GestorRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {

    private GestorRepository gestorRepository;
    private AdminService adminService;
    private Gestor admin;
    private Gestor gestor;

    @BeforeEach
    void setUp() {
        gestorRepository = new GestorRepositoryImpl();
        adminService = new AdminService(gestorRepository);

        admin = new Gestor(UUID.randomUUID(), "admin@teste.pt", "hash", "Admin", PapelGestor.ADMIN);
        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor", PapelGestor.GESTOR);
        gestorRepository.guardar(admin);
        gestorRepository.guardar(gestor);
    }

    @Test
    void deveDesativarGestor() {
        Gestor resultado = adminService.alterarEstado(admin.getId(), gestor.getId(), false);

        assertFalse(resultado.isAtivo());
    }

    @Test
    void deveReativarGestor() {
        adminService.alterarEstado(admin.getId(), gestor.getId(), false);
        Gestor resultado = adminService.alterarEstado(admin.getId(), gestor.getId(), true);

        assertTrue(resultado.isAtivo());
    }

    @Test
    void devePromoverGestorAAdmin() {
        Gestor resultado = adminService.alterarPapel(admin.getId(), gestor.getId(), PapelGestor.ADMIN);

        assertEquals(PapelGestor.ADMIN, resultado.getPapel());
    }

    @Test
    void naoDeveAlterarAPropriaConta() {
        assertThrows(
                IllegalStateException.class,
                () -> adminService.alterarEstado(admin.getId(), admin.getId(), false)
        );
        assertThrows(
                IllegalStateException.class,
                () -> adminService.alterarPapel(admin.getId(), admin.getId(), PapelGestor.GESTOR)
        );
    }

    /**
     * O que garante que nunca ficamos sem administrador não é a contagem de
     * admins, é a regra de não se poder mexer na própria conta: quem ficar
     * sozinho não se consegue desativar a si mesmo.
     */
    @Test
    void sobraSempreUmAdministradorAtivo() {
        Gestor segundo = new Gestor(UUID.randomUUID(), "outro@teste.pt", "hash", "Outro", PapelGestor.ADMIN);
        gestorRepository.guardar(segundo);

        adminService.alterarEstado(admin.getId(), segundo.getId(), false);
        assertEquals(1, gestorRepository.contarAdminsAtivos());

        assertThrows(
                IllegalStateException.class,
                () -> adminService.alterarEstado(admin.getId(), admin.getId(), false)
        );
        assertEquals(1, gestorRepository.contarAdminsAtivos());
    }

    @Test
    void despromoverOOutroAdminDeixaSempreUmAtivo() {
        Gestor segundo = new Gestor(UUID.randomUUID(), "outro@teste.pt", "hash", "Outro", PapelGestor.ADMIN);
        gestorRepository.guardar(segundo);

        adminService.alterarPapel(admin.getId(), segundo.getId(), PapelGestor.GESTOR);

        assertEquals(1, gestorRepository.contarAdminsAtivos());
        assertThrows(
                IllegalStateException.class,
                () -> adminService.alterarPapel(admin.getId(), admin.getId(), PapelGestor.GESTOR)
        );
    }

    @Test
    void deveListarTodosOsGestores() {
        assertEquals(2, adminService.listarGestores().size());
    }

    @Test
    void deveBloquearECederAPermissaoDeCriarLigas() {
        assertTrue(gestor.isPodeCriarLigas());

        Gestor bloqueado = adminService.alterarPermissaoCriarLigas(admin.getId(), gestor.getId(), false);
        assertFalse(bloqueado.isPodeCriarLigas());

        Gestor liberado = adminService.alterarPermissaoCriarLigas(admin.getId(), gestor.getId(), true);
        assertTrue(liberado.isPodeCriarLigas());
    }

    @Test
    void naoDeveAlterarAPropriaPermissaoDeCriarLigas() {
        assertThrows(
                IllegalStateException.class,
                () -> adminService.alterarPermissaoCriarLigas(admin.getId(), admin.getId(), false)
        );
    }

    /* ---------------------------------------------------- correcao de email --- */

    @Test
    void deveCorrigirOEmailDeOutraConta() {
        Gestor resultado = adminService.alterarEmail(admin.getId(), gestor.getId(), "  Novo@Exemplo.PT ");

        // normalizado, como em todo o lado: sem espacos e em minusculas
        assertEquals("novo@exemplo.pt", resultado.getEmail());
    }

    /**
     * O email e metade das credenciais. Muda-lo a outra pessoa e mudar por onde
     * ela entra, e deixar sessoes vivas seria deixar meia porta aberta.
     */
    @Test
    void corrigirOEmailDeveCortarAsSessoesAbertas() {
        assertNull(gestor.getSessoesValidasDesde());

        Gestor resultado = adminService.alterarEmail(admin.getId(), gestor.getId(), "novo@exemplo.pt");

        assertNotNull(resultado.getSessoesValidasDesde());
    }

    @Test
    void naoDeveCorrigirParaUmEmailQueJaEDeOutraConta() {
        assertThrows(IllegalStateException.class,
                () -> adminService.alterarEmail(admin.getId(), gestor.getId(), "admin@teste.pt"));
    }

    @Test
    void naoDeveCorrigirParaUmEmailInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> adminService.alterarEmail(admin.getId(), gestor.getId(), "isto-nao-e-um-email"));
    }

    @Test
    void naoDeveCorrigirParaOMesmoEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> adminService.alterarEmail(admin.getId(), gestor.getId(), "GESTOR@teste.pt"));
    }

    /** Mexer na propria conta continua vedado, tal como no estado e no papel. */
    @Test
    void naoDeveCorrigirOEmailDaPropriaConta() {
        assertThrows(IllegalStateException.class,
                () -> adminService.alterarEmail(admin.getId(), admin.getId(), "outro@exemplo.pt"));
    }
}
