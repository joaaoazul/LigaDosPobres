package com.ligarecord.service;

import com.ligarecord.domain.ContaTreinador;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.ContaTreinadorRepository;
import com.ligarecord.repository.ContaTreinadorRepositoryImpl;
import com.ligarecord.repository.ConviteTreinadorRepositoryImpl;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.GestorRepositoryImpl;
import com.ligarecord.web.ConviteInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaTreinadorServiceTest {

    private ContaTreinadorRepository contaRepository;
    private GestorRepository gestorRepository;
    private ConviteTreinadorService conviteService;
    private ContaTreinadorService contaService;
    private PasswordEncoder passwordEncoder;
    private Gestor gestor;
    private Treinador treinador;

    @BeforeEach
    void setUp() {
        contaRepository = new ContaTreinadorRepositoryImpl();
        gestorRepository = new GestorRepositoryImpl();
        passwordEncoder = new BCryptPasswordEncoder();
        conviteService = new ConviteTreinadorService(
                new ConviteTreinadorRepositoryImpl(), contaRepository);
        contaService = new ContaTreinadorService(
                contaRepository, gestorRepository, conviteService, passwordEncoder);

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        gestorRepository.guardar(gestor);
        treinador = new Treinador(UUID.randomUUID(), "João Azul");
    }

    private String convite() {
        return conviteService.criar(gestor, treinador, null).getCodigo();
    }

    @Test
    void deveRegistarComConviteValido() {
        ContaTreinador conta = contaService.registar(
                "Joao@Exemplo.PT", "passwordsegura1", "João", convite());

        assertEquals("joao@exemplo.pt", conta.getEmail());
        assertTrue(conta.isAtivo());
    }

    /** O treinador vem do convite: quem se regista não escolhe que equipas vai ver. */
    @Test
    void aContaFicaLigadaAoTreinadorDoConvite() {
        ContaTreinador conta = contaService.registar(
                "joao@exemplo.pt", "passwordsegura1", "João", convite());

        assertEquals(treinador, conta.getTreinador());
    }

    @Test
    void naoDeveGuardarAPasswordEmClaro() {
        ContaTreinador conta = contaService.registar(
                "joao@exemplo.pt", "passwordsegura1", "João", convite());

        assertNotEquals("passwordsegura1", conta.getPasswordHash());
        assertTrue(passwordEncoder.matches("passwordsegura1", conta.getPasswordHash()));
    }

    @Test
    void conviteSoServeUmaVez() {
        String codigo = convite();
        contaService.registar("joao@exemplo.pt", "passwordsegura1", "João", codigo);

        assertThrows(
                ConviteInvalidoException.class,
                () -> contaService.registar("outro@exemplo.pt", "passwordsegura1", "Outro", codigo));
    }

    @Test
    void naoDeveRegistarSemConvite() {
        assertThrows(
                ConviteInvalidoException.class,
                () -> contaService.registar("joao@exemplo.pt", "passwordsegura1", "João", null));
    }

    @Test
    void naoDeveRegistarPasswordCurta() {
        assertThrows(
                IllegalArgumentException.class,
                () -> contaService.registar("joao@exemplo.pt", "curta", "João", convite()));
    }

    /**
     * Gestores e treinadores partilham o espaço de emails porque a autenticação
     * procura nas duas tabelas; um email repetido deixaria uma conta sem entrar.
     */
    @Test
    void naoDeveRegistarEmailJaUsadoPorUmGestor() {
        assertThrows(
                IllegalStateException.class,
                () -> contaService.registar("GESTOR@teste.pt", "passwordsegura1", "João", convite()));
    }

    @Test
    void naoDeveRegistarEmailJaUsadoPorOutroTreinador() {
        contaService.registar("joao@exemplo.pt", "passwordsegura1", "João", convite());

        Treinador outro = new Treinador(UUID.randomUUID(), "Outro");
        String codigo = conviteService.criar(gestor, outro, null).getCodigo();

        assertThrows(
                IllegalStateException.class,
                () -> contaService.registar("JOAO@exemplo.pt", "passwordsegura2", "Outro", codigo));
    }

    /** Um convite gasto não pode ficar gasto se a conta não chegou a ser criada. */
    @Test
    void conviteContinuaUtilizavelSeORegistoFalhar() {
        String codigo = convite();

        assertThrows(
                IllegalStateException.class,
                () -> contaService.registar("GESTOR@teste.pt", "passwordsegura1", "João", codigo));

        ContaTreinador conta = contaService.registar(
                "joao@exemplo.pt", "passwordsegura1", "João", codigo);
        assertEquals(treinador, conta.getTreinador());
    }

    @Test
    void deveAlterarPasswordComAActualCorrecta() {
        ContaTreinador conta = contaService.registar(
                "joao@exemplo.pt", "passwordsegura1", "João", convite());

        contaService.alterarPassword(conta.getId(), "passwordsegura1", "outrapasswordsegura");

        assertTrue(passwordEncoder.matches("outrapasswordsegura", conta.getPasswordHash()));
        assertFalse(passwordEncoder.matches("passwordsegura1", conta.getPasswordHash()));
    }

    @Test
    void naoDeveAlterarPasswordSemAActual() {
        ContaTreinador conta = contaService.registar(
                "joao@exemplo.pt", "passwordsegura1", "João", convite());

        assertThrows(
                IllegalArgumentException.class,
                () -> contaService.alterarPassword(conta.getId(), "errada", "outrapasswordsegura"));
    }
}
