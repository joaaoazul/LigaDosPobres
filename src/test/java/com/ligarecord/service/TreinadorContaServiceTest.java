package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.ConviteTreinadorRepositoryImpl;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.GestorRepositoryImpl;
import com.ligarecord.repository.TreinadorRepository;
import com.ligarecord.repository.TreinadorRepositoryImpl;
import com.ligarecord.web.ConviteInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreinadorContaServiceTest {

    private GestorRepository gestorRepository;
    private TreinadorRepository treinadorRepository;
    private ConviteTreinadorService conviteService;
    private TreinadorContaService contaService;
    private PasswordEncoder passwordEncoder;
    private Gestor gestor;
    private Treinador treinador;

    @BeforeEach
    void setUp() {
        gestorRepository = new GestorRepositoryImpl();
        treinadorRepository = new TreinadorRepositoryImpl();
        passwordEncoder = new BCryptPasswordEncoder();
        conviteService = new ConviteTreinadorService(new ConviteTreinadorRepositoryImpl());
        contaService = new TreinadorContaService(
                gestorRepository, treinadorRepository, conviteService, passwordEncoder);

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        gestorRepository.guardar(gestor);
        treinador = new Treinador(UUID.randomUUID(), "João Azul");
    }

    private String convite() {
        return conviteService.criar(gestor, treinador, null).getCodigo();
    }

    @Test
    void deveRegistarComConviteValido() {
        Gestor conta = contaService.registar("Joao@Exemplo.PT", "passwordsegura1", "João", convite());

        assertEquals("joao@exemplo.pt", conta.getEmail());
        assertTrue(conta.isAtivo());
    }

    /** O treinador vem do convite: quem se regista não escolhe que equipas vai ver. */
    @Test
    void aContaFicaLigadaAoTreinadorDoConvite() {
        Gestor conta = contaService.registar("joao@exemplo.pt", "passwordsegura1", "João", convite());

        assertEquals(conta, treinador.getConta());
    }

    @Test
    void naoDeveGuardarAPasswordEmClaro() {
        Gestor conta = contaService.registar("joao@exemplo.pt", "passwordsegura1", "João", convite());

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

    @Test
    void naoDeveRegistarEmailJaUsadoPorUmGestor() {
        assertThrows(
                IllegalStateException.class,
                () -> contaService.registar("GESTOR@teste.pt", "passwordsegura1", "João", convite()));
    }

    /** Um convite gasto não pode ficar gasto se a ligação ao treinador não chegou a ser feita. */
    @Test
    void conviteContinuaUtilizavelSeORegistoFalhar() {
        String codigo = convite();

        assertThrows(
                IllegalStateException.class,
                () -> contaService.registar("GESTOR@teste.pt", "passwordsegura1", "João", codigo));

        Gestor conta = contaService.registar("joao@exemplo.pt", "passwordsegura1", "João", codigo);
        assertEquals(conta, treinador.getConta());
    }

    /**
     * O caso que motivou fundir as contas: um gestor que também treina uma
     * equipa usa a mesma conta, sem criar um segundo login.
     */
    @Test
    void deveLigarTreinadorAUmaContaDeGestorJaExistente() {
        contaService.ligar(convite(), gestor);

        assertEquals(gestor, treinador.getConta());
        // nenhuma conta nova foi criada: continua a existir só a do gestor do setUp
        assertEquals(1, gestorRepository.listarTodos().size());
    }

    @Test
    void naoDeveLigarSemIndicarConta() {
        String codigo = convite();
        assertThrows(IllegalArgumentException.class, () -> contaService.ligar(codigo, null));
    }

    @Test
    void ligarTambemGastaOConvite() {
        String codigo = convite();
        contaService.ligar(codigo, gestor);

        assertThrows(ConviteInvalidoException.class, () -> conviteService.exigirDisponivel(codigo));
    }

    /** Sem convite aceite, o treinador simplesmente fica sem conta — e isso é válido. */
    @Test
    void treinadorSemConviteAceiteFicaSemConta() {
        convite();

        assertNotNull(treinador);
        assertFalse(treinador.temConta());
    }

    /**
     * O convite pode ter sido emitido antes de o treinador ganhar conta por
     * outra via (ex.: um segundo convite ainda válido para o mesmo treinador).
     * Sem esta verificação, ligar substituía a conta já ligada em silêncio.
     */
    @Test
    void naoDeveLigarTreinadorQueJaTemConta() {
        String codigoUm = convite();
        String codigoDois = conviteService.criar(gestor, treinador, null).getCodigo();

        contaService.ligar(codigoUm, gestor);

        Gestor outraConta = new Gestor(UUID.randomUUID(), "outro@teste.pt", "hash", "Outro");
        gestorRepository.guardar(outraConta);

        assertThrows(IllegalStateException.class, () -> contaService.ligar(codigoDois, outraConta));
    }

    @Test
    void naoDeveRegistarTreinadorQueJaTemConta() {
        String codigoUm = convite();
        String codigoDois = conviteService.criar(gestor, treinador, null).getCodigo();

        contaService.registar("joao@exemplo.pt", "passwordsegura1", "João", codigoUm);

        assertThrows(
                IllegalStateException.class,
                () -> contaService.registar("outro@exemplo.pt", "passwordsegura2", "Outro", codigoDois));
    }
}
