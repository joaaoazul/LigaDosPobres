package com.ligarecord.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModeloDeEmailTest {

    private static final String LINK = "https://liga.exemplo.pt/nova-password.html?codigo=abc123";

    @Test
    void deveLevarAsDuasVersoes() {
        ModeloDeEmail.Mensagem m = ModeloDeEmail.recuperacaoDePassword("João", LINK, "dentro de uma hora");

        assertFalse(m.assunto().isBlank());
        assertFalse(m.texto().isBlank());
        assertFalse(m.html().isBlank());
    }

    @Test
    void oLinkTemDeAparecerNasDuas() {
        ModeloDeEmail.Mensagem m = ModeloDeEmail.recuperacaoDePassword("João", LINK, "dentro de uma hora");

        assertTrue(m.texto().contains(LINK));
        assertTrue(m.html().contains("href=\"" + LINK + "\""));
        // E também à vista, em texto: há clientes que desfazem botões, e quem
        // recebe tem de poder copiar o endereço à mão.
        assertTrue(m.html().contains(">" + LINK + "<"));
    }

    /**
     * O nome é escrito pela própria pessoa no registo. Sem escapar, um nome com
     * um sinal de menor desfazia a mensagem, e é o género de coisa que só se
     * descobre no dia em que alguém se chama assim.
     */
    @Test
    void deveEscaparONomeNoHtml() {
        ModeloDeEmail.Mensagem m = ModeloDeEmail.recuperacaoDePassword(
                "<script>alert(1)</script>", LINK, "dentro de uma hora");

        assertFalse(m.html().contains("<script>"));
        assertTrue(m.html().contains("&lt;script&gt;"));
    }

    @Test
    void aValidadeVemDeQuemChama() {
        ModeloDeEmail.Mensagem m = ModeloDeEmail.recuperacaoDePassword("João", LINK, "dentro de 3 horas");

        assertTrue(m.texto().contains("dentro de 3 horas"));
        assertTrue(m.html().contains("dentro de 3 horas"));
    }

    /** Um nome vazio ou nulo não pode rebentar o envio. */
    @Test
    void deveAguentarUmNomeEmFalta() {
        assertDoesNotThrow(() -> ModeloDeEmail.recuperacaoDePassword(null, LINK, "dentro de uma hora"));
        assertDoesNotThrow(() -> ModeloDeEmail.recuperacaoDePassword("", LINK, "dentro de uma hora"));
    }

    /**
     * O HTML de email tem regras próprias, e estas três são as que partem a
     * mensagem em clientes reais se alguém as esquecer numa alteração futura.
     */
    @Test
    void deveSeguirAsRegrasDoHtmlDeEmail() {
        String html = ModeloDeEmail.recuperacaoDePassword("João", LINK, "dentro de uma hora").html();

        assertFalse(html.contains("<style"), "folhas de estilo são deitadas fora por vários clientes");
        assertFalse(html.contains("<img"), "imagens ficam bloqueadas até se autorizarem");
        assertTrue(html.contains("<table"), "a disposição tem de ser em tabelas");
    }
}
