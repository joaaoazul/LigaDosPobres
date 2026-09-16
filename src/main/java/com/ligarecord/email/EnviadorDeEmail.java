package com.ligarecord.email;

/**
 * Envio de uma mensagem de correio. Os serviços dependem só disto; qual é o
 * fornecedor fica do lado das implementações, tal como o Hibernate fica do lado
 * dos adaptadores de repositório.
 *
 * <p>Falhar a enviar não é excepcional ao ponto de rebentar o pedido: quem pede
 * a recuperação da password recebe a mesma resposta de qualquer forma, e mandar
 * abaixo o pedido só serviria para dizer a quem tenta adivinhar emails que
 * aquele existe. Cabe a cada implementação registar a falha em log.
 *
 * <p>Devolve se a mensagem saiu. Na recuperação de password o resultado é
 * ignorado de propósito — a resposta é a mesma de qualquer maneira — mas no
 * convite de treinador quem carregou no botão escreveu o endereço com as
 * próprias mãos e tem direito a saber se aquilo chegou a partir.
 */
public interface EnviadorDeEmail {

    /**
     * As duas versões vão sempre juntas. Quem recebe em texto simples, ou num
     * cliente que recusa HTML, tem de continuar a conseguir recuperar a
     * password; e um email só com HTML é olhado de lado pelos filtros de spam.
     */
    default boolean enviar(String para, String assunto, String texto, String html) {
        return enviar(para, assunto, texto, html, null);
    }

    /**
     * O mesmo, dizendo para onde vai a resposta de quem carregar em "Responder".
     *
     * <p>Existe pelo formulário de contacto: a mensagem sai do endereço da
     * aplicação ({@code noreply@}) mas quem a escreveu foi uma pessoa, e sem
     * isto responder-lhe obrigava a copiar o endereço do corpo da mensagem para
     * uma mensagem nova. Com {@code responderA} a null é um envio normal.
     */
    boolean enviar(String para, String assunto, String texto, String html, String responderA);
}
