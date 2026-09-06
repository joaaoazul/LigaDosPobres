package com.ligarecord.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Escolhe o enviador de email conforme haja ou não chave configurada.
 *
 * <p>Um {@code if} explícito em vez de duas anotações condicionais em duas
 * classes: com {@code @ConditionalOnMissingBean} entre componentes varridos, a
 * decisão passava a depender da ordem por que o Spring os encontra, que não é
 * garantida. Aqui vê-se, num sítio só, qual entra e porquê.
 */
@Configuration
public class ConfiguracaoDeEmail {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracaoDeEmail.class);

    @Bean
    public EnviadorDeEmail enviadorDeEmail(@Value("${email.chave:}") String chave,
                                           @Value("${email.remetente:}") String remetente) {
        if (chave.isBlank() || remetente.isBlank()) {
            LOG.warn("Sem EMAIL_CHAVE ou EMAIL_REMETENTE: as mensagens vão para o log "
                    + "em vez de serem enviadas. Ninguém consegue recuperar a password.");
            return new EnviadorParaLog();
        }
        LOG.info("Emails enviados pelo Resend, de {}.", remetente);
        return new EnviadorResend(chave, remetente);
    }
}
