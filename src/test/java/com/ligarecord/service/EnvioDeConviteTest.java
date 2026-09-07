package com.ligarecord.service;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.email.EnviadorDeEmail;
import com.ligarecord.repository.ConviteTreinadorRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A entrega do convite por email: quando sai, quando não sai, e o que fica registado. */
class EnvioDeConviteTest {

    private static class EnviadorDeTeste implements EnviadorDeEmail {
        record Mensagem(String para, String assunto, String texto, String html) {
        }

        final List<Mensagem> enviadas = new ArrayList<>();
        boolean consegue = true;

        @Override
        public boolean enviar(String para, String assunto, String texto, String html) {
            if (!consegue) {
                return false;
            }
            enviadas.add(new Mensagem(para, assunto, texto, html));
            return true;
        }
    }

    private EnviadorDeTeste enviador;
    private ConviteTreinadorService conviteService;
    private Gestor gestor;
    private Treinador treinador;
    private Equipa equipa;

    @BeforeEach
    void setUp() {
        enviador = new EnviadorDeTeste();
        conviteService = new ConviteTreinadorService(
                new ConviteTreinadorRepositoryImpl(), enviador,
                new LinksDaAplicacao("https://liga.exemplo.pt"));

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        treinador = new Treinador(UUID.randomUUID(), "João Azul");
        Liga liga = new Liga(UUID.randomUUID(), "Liga do Café", 10, EstadoLiga.ATIVA, gestor);
        equipa = new Equipa(UUID.randomUUID(), "Leões", treinador, liga, EstadoEquipa.ATIVA);
    }

    private ConviteTreinador convite() {
        return conviteService.emitir(gestor, equipa, null).convite();
    }

    @Test
    void semEmailNaoTentaEnviar() {
        assertEquals(ConviteTreinadorService.Envio.SEM_EMAIL, conviteService.enviarPorEmail(convite()));
        assertTrue(enviador.enviadas.isEmpty());
    }

    @Test
    void comEmailEnviaOLinkEGuardaORasto() {
        treinador.setEmail("joao@exemplo.pt");
        ConviteTreinador convite = convite();

        assertEquals(ConviteTreinadorService.Envio.ENVIADO, conviteService.enviarPorEmail(convite));
        assertEquals(1, enviador.enviadas.size());

        EnviadorDeTeste.Mensagem mensagem = enviador.enviadas.get(0);
        assertEquals("joao@exemplo.pt", mensagem.para());
        assertTrue(mensagem.texto().contains("https://liga.exemplo.pt/convite.html?c=" + convite.getCodigo()));
        // O email diz sempre quem convidou e para quê: é o que permite a quem o
        // recebe por engano perceber que não lhe diz respeito.
        assertTrue(mensagem.texto().contains("Gestor"));
        assertTrue(mensagem.texto().contains("Leões"));
        assertTrue(mensagem.texto().contains("Liga do Café"));

        assertEquals(1, convite.getEnvios());
        assertEquals("joao@exemplo.pt", convite.getEnviadoPara());
    }

    /** Uma falha do servidor de email não pode gastar o travão nem sujar o rasto. */
    @Test
    void envioFalhadoNaoContaComoEnviado() {
        treinador.setEmail("joao@exemplo.pt");
        enviador.consegue = false;
        ConviteTreinador convite = convite();

        assertEquals(ConviteTreinadorService.Envio.FALHOU, conviteService.enviarPorEmail(convite));
        assertEquals(0, convite.getEnvios());
        assertFalse(convite.estaRevogado());
        // e o convite continua a valer pelo link
        assertTrue(convite.estaDisponivel());
    }

    @Test
    void naoEnviaDuasVezesSeguidas() {
        treinador.setEmail("joao@exemplo.pt");
        ConviteTreinador convite = convite();

        assertEquals(ConviteTreinadorService.Envio.ENVIADO, conviteService.enviarPorEmail(convite));
        assertEquals(ConviteTreinadorService.Envio.LIMITE_ATINGIDO, conviteService.enviarPorEmail(convite));
        assertEquals(1, enviador.enviadas.size());
    }

    @Test
    void naoPassaDoMaximoDeEnvios() {
        treinador.setEmail("joao@exemplo.pt");
        ConviteTreinador convite = convite();

        // Marcados à mão para saltar o intervalo mínimo entre envios, que é o
        // outro travão e não é o que este teste quer exercitar.
        for (int i = 0; i < ConviteTreinadorService.MAXIMO_ENVIOS; i++) {
            convite.marcarEnviado("joao@exemplo.pt");
        }

        assertEquals(ConviteTreinadorService.Envio.LIMITE_ATINGIDO, conviteService.enviarPorEmail(convite));
        assertTrue(enviador.enviadas.isEmpty());
    }

    /** O email vai para o contacto do lugar, e não para o da conta de quem o emitiu. */
    @Test
    void enviaParaOEmailDoLugarENaoParaODoGestor() {
        treinador.setEmail("Joao@Exemplo.PT");
        conviteService.enviarPorEmail(convite());

        assertEquals("Joao@Exemplo.PT", enviador.enviadas.get(0).para());
    }
}
