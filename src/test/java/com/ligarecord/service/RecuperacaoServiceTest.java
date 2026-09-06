package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.email.EnviadorDeEmail;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.GestorRepositoryImpl;
import com.ligarecord.repository.PedidoRecuperacaoRepository;
import com.ligarecord.repository.PedidoRecuperacaoRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RecuperacaoServiceTest {

    /** Guarda o que teria sido enviado, para os testes poderem ler o link. */
    private static class EnviadorDeTeste implements EnviadorDeEmail {
        record Mensagem(String para, String assunto, String corpo, String html) {
        }

        final List<Mensagem> enviadas = new ArrayList<>();

        @Override
        public void enviar(String para, String assunto, String texto, String html) {
            enviadas.add(new Mensagem(para, assunto, texto, html));
        }
    }

    private static final Pattern CODIGO_NO_LINK = Pattern.compile("codigo=(\\S+)");

    private GestorRepository gestorRepository;
    private PedidoRecuperacaoRepository pedidoRepository;
    private PasswordEncoder passwordEncoder;
    private EnviadorDeTeste enviador;
    private RecuperacaoService servico;
    private Gestor gestor;

    @BeforeEach
    void setUp() {
        gestorRepository = new GestorRepositoryImpl();
        pedidoRepository = new PedidoRecuperacaoRepositoryImpl();
        passwordEncoder = new BCryptPasswordEncoder();
        enviador = new EnviadorDeTeste();
        servico = new RecuperacaoService(gestorRepository, pedidoRepository, passwordEncoder,
                enviador, new LinksDaAplicacao("https://liga.exemplo.pt"));

        gestor = new Gestor(UUID.randomUUID(), "joao@exemplo.pt",
                passwordEncoder.encode("passwordantiga"), "João");
        gestorRepository.guardar(gestor);
    }

    private String codigoDoUltimoEmail() {
        Matcher m = CODIGO_NO_LINK.matcher(enviador.enviadas.getLast().corpo());
        assertTrue(m.find(), "o email tem de trazer o código no link");
        return m.group(1);
    }

    @Test
    void deveEnviarOLinkParaUmaContaQueExiste() {
        servico.pedir("joao@exemplo.pt");

        assertEquals(1, enviador.enviadas.size());
        assertEquals("joao@exemplo.pt", enviador.enviadas.getFirst().para());
        assertTrue(enviador.enviadas.getFirst().corpo()
                .contains("https://liga.exemplo.pt/nova-password.html?codigo="));
    }

    @Test
    void deveAceitarOEmailComMaiusculasEEspacos() {
        servico.pedir("  Joao@Exemplo.PT  ");

        assertEquals(1, enviador.enviadas.size());
    }

    @Test
    void naoDeveEnviarNadaParaUmEmailSemConta() {
        servico.pedir("ninguem@exemplo.pt");

        assertTrue(enviador.enviadas.isEmpty());
    }

    @Test
    void naoDeveEnviarNadaParaUmaContaDesativada() {
        gestor.setAtivo(false);
        gestorRepository.guardar(gestor);

        servico.pedir("joao@exemplo.pt");

        assertTrue(enviador.enviadas.isEmpty());
    }

    /** Um email inexistente não pode rebentar: a resposta tem de ser igual à do que existe. */
    @Test
    void naoDeveQueixarSeDeUmEmailMalEscrito() {
        assertDoesNotThrow(() -> servico.pedir("isto-nao-e-um-email"));
        assertDoesNotThrow(() -> servico.pedir(null));
        assertTrue(enviador.enviadas.isEmpty());
    }

    @Test
    void oCodigoNaoPodeFicarGuardadoEmClaro() {
        servico.pedir("joao@exemplo.pt");
        String codigo = codigoDoUltimoEmail();

        assertTrue(pedidoRepository.buscarPorCodigoHash(codigo).isEmpty(),
                "procurar pelo código em claro não pode encontrar nada");
    }

    @Test
    void deveRedefinirAPasswordComUmCodigoValido() {
        servico.pedir("joao@exemplo.pt");

        servico.redefinir(codigoDoUltimoEmail(), "passwordnovaemuitoboa");

        Gestor atual = gestorRepository.buscarPorId(gestor.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("passwordnovaemuitoboa", atual.getPasswordHash()));
        assertFalse(passwordEncoder.matches("passwordantiga", atual.getPasswordHash()));
    }

    @Test
    void redefinirDeveCortarAsSessoesAbertas() {
        assertNull(gestor.getSessoesValidasDesde(), "uma conta nova não tem nada a invalidar");

        servico.pedir("joao@exemplo.pt");
        servico.redefinir(codigoDoUltimoEmail(), "passwordnovaemuitoboa");

        assertNotNull(gestorRepository.buscarPorId(gestor.getId()).orElseThrow()
                .getSessoesValidasDesde());
    }

    @Test
    void oMesmoCodigoNaoPodeServirDuasVezes() {
        servico.pedir("joao@exemplo.pt");
        String codigo = codigoDoUltimoEmail();
        servico.redefinir(codigo, "passwordnovaemuitoboa");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.redefinir(codigo, "outrapasswordqualquer"));
        assertTrue(erro.getMessage().contains("não é válido ou já expirou"));
    }

    @Test
    void naoDeveAceitarUmCodigoInventado() {
        assertThrows(IllegalArgumentException.class,
                () -> servico.redefinir("codigo-que-nunca-existiu", "passwordnovaemuitoboa"));
        assertThrows(IllegalArgumentException.class,
                () -> servico.redefinir(null, "passwordnovaemuitoboa"));
        assertThrows(IllegalArgumentException.class,
                () -> servico.redefinir("", "passwordnovaemuitoboa"));
    }

    @Test
    void naoDeveAceitarUmaPasswordCurta() {
        servico.pedir("joao@exemplo.pt");
        String codigo = codigoDoUltimoEmail();

        assertThrows(IllegalArgumentException.class, () -> servico.redefinir(codigo, "curta"));

        // e o código tem de continuar a servir: a password é que estava mal,
        // não o link, e obrigar a pedir outro seria castigar o engano errado
        assertDoesNotThrow(() -> servico.redefinir(codigo, "passwordnovaemuitoboa"));
    }

    @Test
    void deveTravarOEnvioRepetidoParaAMesmaConta() {
        for (int i = 0; i < RecuperacaoService.MAXIMO_POR_JANELA + 3; i++) {
            servico.pedir("joao@exemplo.pt");
        }

        assertEquals(RecuperacaoService.MAXIMO_POR_JANELA, enviador.enviadas.size());
    }

    /** Cada pedido tem o seu código: o travão não pode passar por reutilizar um. */
    @Test
    void cadaPedidoDeveTerOSeuCodigo() {
        servico.pedir("joao@exemplo.pt");
        String primeiro = codigoDoUltimoEmail();
        servico.pedir("joao@exemplo.pt");
        String segundo = codigoDoUltimoEmail();

        assertNotEquals(primeiro, segundo);
        // o primeiro continua válido: pedir outro link não invalida o anterior,
        // e quem clicar no email mais antigo não fica sem perceber porquê
        assertDoesNotThrow(() -> servico.redefinir(primeiro, "passwordnovaemuitoboa"));
    }
}
