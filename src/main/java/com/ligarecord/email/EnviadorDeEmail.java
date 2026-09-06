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
 */
public interface EnviadorDeEmail {

    /**
     * As duas versões vão sempre juntas. Quem recebe em texto simples, ou num
     * cliente que recusa HTML, tem de continuar a conseguir recuperar a
     * password; e um email só com HTML é olhado de lado pelos filtros de spam.
     */
    void enviar(String para, String assunto, String texto, String html);
}
