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

    /* ---------------------------------------------- convite de treinador --- */

    private static final String LINK_CONVITE = "https://liga.exemplo.pt/convite.html?c=abc123";

    private ModeloDeEmail.Mensagem convite(String nomeTreinador, String nomeGestor) {
        return ModeloDeEmail.conviteDeTreinador(nomeTreinador, nomeGestor,
                "Leões", "Liga do Café", LINK_CONVITE, "a 6 de outubro de 2026");
    }

    @Test
    void oConviteLevaAsDuasVersoesEOLinkNasDuas() {
        ModeloDeEmail.Mensagem m = convite("João", "Zé");

        assertFalse(m.texto().isBlank());
        assertFalse(m.html().isBlank());
        assertTrue(m.texto().contains(LINK_CONVITE));
        assertTrue(m.html().contains("href=\"" + LINK_CONVITE + "\""));
        assertTrue(m.html().contains(">" + LINK_CONVITE + "<"));
    }

    /**
     * O endereço foi escrito pelo gestor e pode estar errado. A mensagem tem de
     * fazer sentido para quem a recebe por engano: quem convidou, para quê, e
     * que ignorar não deixa nada em seu nome.
     */
    @Test
    void oConviteDizQuemConvidouEParaQue() {
        ModeloDeEmail.Mensagem m = convite("João", "Zé");

        assertTrue(m.assunto().contains("Leões"));
        assertTrue(m.texto().contains("Zé"));
        assertTrue(m.texto().contains("Leões"));
        assertTrue(m.texto().contains("Liga do Café"));
        assertTrue(m.texto().contains("ignora"));
        assertTrue(m.html().contains("ignora"));
    }

    /** Tanto o nome do treinador como o do gestor vêm de texto escrito à mão. */
    @Test
    void oConviteEscapaOsNomesNoHtml() {
        ModeloDeEmail.Mensagem m = convite("<script>", "\"Zé\"");

        assertFalse(m.html().contains("<script>"));
        assertTrue(m.html().contains("&lt;script&gt;"));
        assertTrue(m.html().contains("&quot;Zé&quot;"));
    }

    /** Sem liga (o caso de uma equipa ainda sem liga) a frase não pode ficar coxa. */
    @Test
    void oConviteAguentaUmaEquipaSemLiga() {
        ModeloDeEmail.Mensagem m = ModeloDeEmail.conviteDeTreinador(
                "João", "Zé", "Leões", null, LINK_CONVITE, "a 6 de outubro de 2026");

        assertTrue(m.texto().contains("treinar Leões."));
        assertFalse(m.texto().contains("na null"));
        assertFalse(m.html().contains("null"));
    }
}
