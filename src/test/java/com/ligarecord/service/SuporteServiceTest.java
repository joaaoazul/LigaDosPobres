package com.ligarecord.service;

import com.ligarecord.email.EnviadorDeEmail;
import com.ligarecord.repository.MensagemSuporteRepository;
import com.ligarecord.repository.MensagemSuporteRepositoryImpl;
import com.ligarecord.web.PedidosDemaisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuporteServiceTest {

    /** Guarda o que lhe mandam enviar, para se poder olhar para dentro. */
    private static class EnviadorDeTeste implements EnviadorDeEmail {

        private final List<String[]> enviados = new ArrayList<>();
        private boolean sai = true;

        @Override
        public boolean enviar(String para, String assunto, String texto, String html, String responderA) {
            enviados.add(new String[] {para, assunto, texto, responderA});
            return sai;
        }
    }

    private MensagemSuporteRepository mensagens;
    private EnviadorDeTeste enviador;
    private SuporteService servico;

    @BeforeEach
    void setUp() {
        mensagens = new MensagemSuporteRepositoryImpl();
        enviador = new EnviadorDeTeste();
        servico = new SuporteService(mensagens, enviador, "suporte@teste.pt", "admin@teste.pt");
    }

    @Test
    void enviaAMensagemParaOSuporteComRespostaParaQuemEscreveu() {
        boolean enviado = servico.receber("  João  ", " JOAO@Exemplo.PT ", "Não entro",
                "A password não funciona.", "203.0.113.7");

        assertTrue(enviado);
        assertEquals(1, enviador.enviados.size());
        String[] email = enviador.enviados.get(0);
        assertEquals("suporte@teste.pt", email[0]);
        assertEquals("[Suporte] Não entro", email[1]);
        assertTrue(email[2].contains("A password não funciona."), email[2]);
        // O reply-to é de quem escreveu, normalizado — é o que faz o "Responder"
        // do cliente de email ir ter com a pessoa, e não com o noreply.
        assertEquals("joao@exemplo.pt", email[3]);
    }

    /** Sem SUPORTE_EMAIL, vai para o do administrador em vez de não ir a lado nenhum. */
    @Test
    void semEnderecoDeSuporteUsaODoAdministrador() {
        servico = new SuporteService(mensagens, enviador, "", "admin@teste.pt");

        servico.receber("João", "joao@exemplo.pt", "Olá", "Mensagem.", "203.0.113.7");

        assertEquals("admin@teste.pt", enviador.enviados.get(0)[0]);
    }

    /**
     * O envio falhar não pode perder a mensagem: é o único sítio onde ela
     * existe depois de quem a escreveu fechar a página.
     */
    @Test
    void guardaAMensagemMesmoQuandoOEnvioFalha() {
        enviador.sai = false;

        boolean enviado = servico.receber("João", "joao@exemplo.pt", "Olá", "Mensagem.", "203.0.113.7");

        assertFalse(enviado);
        assertEquals(1, mensagens.contarDoIpDesde("203.0.113.7", Instant.now().minusSeconds(60)));
    }

    @Test
    void semDestinoNenhumConfiguradoAindaAssimGuarda() {
        servico = new SuporteService(mensagens, enviador, "", "");

        assertFalse(servico.receber("João", "joao@exemplo.pt", "Olá", "Mensagem.", "203.0.113.7"));
        assertEquals(1, mensagens.contarDoIpDesde("203.0.113.7", Instant.now().minusSeconds(60)));
        assertTrue(enviador.enviados.isEmpty());
    }

    @Test
    void travaQuemEnviaDemasiadasDaMesmaOrigem() {
        for (int i = 0; i < SuporteService.MAXIMO_POR_JANELA; i++) {
            servico.receber("João", "joao@exemplo.pt", "Olá", "Mensagem " + i, "203.0.113.7");
        }

        assertThrows(PedidosDemaisException.class,
                () -> servico.receber("João", "joao@exemplo.pt", "Olá", "Mais uma", "203.0.113.7"));
    }

    /**
     * O travão é por origem e não pelo email escrito no formulário: trocar de
     * email é grátis para quem está a despejar mensagens, mudar de origem não.
     */
    @Test
    void oTravaoEPorOrigemENaoPeloEmailEscrito() {
        for (int i = 0; i < SuporteService.MAXIMO_POR_JANELA; i++) {
            servico.receber("João", "joao" + i + "@exemplo.pt", "Olá", "Mensagem", "203.0.113.7");
        }

        assertThrows(PedidosDemaisException.class,
                () -> servico.receber("João", "outro@exemplo.pt", "Olá", "Mais uma", "203.0.113.7"));
        // Outra origem continua a passar.
        assertTrue(servico.receber("João", "joao@exemplo.pt", "Olá", "Mensagem", "198.51.100.4"));
    }

    @Test
    void recusaCamposVaziosOuGrandesDemais() {
        assertThrows(IllegalArgumentException.class,
                () -> servico.receber("", "joao@exemplo.pt", "Olá", "Mensagem", "203.0.113.7"));
        assertThrows(IllegalArgumentException.class,
                () -> servico.receber("João", "nao-e-email", "Olá", "Mensagem", "203.0.113.7"));
        assertThrows(IllegalArgumentException.class,
                () -> servico.receber("João", "joao@exemplo.pt", "", "Mensagem", "203.0.113.7"));
        assertThrows(IllegalArgumentException.class,
                () -> servico.receber("João", "joao@exemplo.pt", "Olá",
                        "a".repeat(SuporteService.MAXIMO_MENSAGEM + 1), "203.0.113.7"));
    }

    /**
     * Um pedido sem origem conhecida passa — não se perde uma mensagem
     * verdadeira por causa de um cabeçalho que falta — mas não é contado por
     * nenhum travão, que é o que o campo nulo quer dizer.
     */
    @Test
    void semOrigemConhecidaAindaAssimAceita() {
        assertTrue(servico.receber("João", "joao@exemplo.pt", "Olá", "Mensagem.", null));
        assertEquals(1, enviador.enviados.size());
    }
}
