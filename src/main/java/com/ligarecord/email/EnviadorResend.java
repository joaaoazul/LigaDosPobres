package com.ligarecord.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Envio pelo Resend, com um POST à API deles.
 *
 * <p>Sem biblioteca: é um pedido HTTP com uma chave no cabeçalho, e o
 * {@code RestClient} já vem no Spring. Uma dependência a mais no
 * {@code pom.xml} só para isto não se pagava.
 *
 * <p>Quem decide se esta implementação entra é a {@link ConfiguracaoDeEmail}.
 */
public class EnviadorResend implements EnviadorDeEmail {

    private static final Logger LOG = LoggerFactory.getLogger(EnviadorResend.class);
    private static final String API = "https://api.resend.com/emails";

    private final RestClient cliente;
    private final String remetente;

    public EnviadorResend(String chave, String remetente) {
        this.remetente = remetente;
        this.cliente = RestClient.builder()
                .baseUrl(API)
                .defaultHeader("Authorization", "Bearer " + chave)
                .build();
    }

    @Override
    public void enviar(String para, String assunto, String corpo) {
        try {
            cliente.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", remetente, "to", para, "subject", assunto, "text", corpo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            // O endereço nunca vai para o log: numa recuperação de password isso
            // passava a dizer, a quem lesse os logs, quem tem conta aqui.
            LOG.error("Falhou o envio de um email pelo Resend: {}", e.getMessage());
        }
    }
}
