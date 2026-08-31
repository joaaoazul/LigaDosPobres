package com.ligarecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LigaDosPobresApplication {

    private static final Logger log = LoggerFactory.getLogger(LigaDosPobresApplication.class);

    public static void main(String[] args) {
        registarDestinoDaBaseDeDados();
        SpringApplication.run(LigaDosPobresApplication.class, args);
    }

    /**
     * Diz, logo na primeira linha do log, a que base de dados a aplicação vai
     * tentar ligar-se.
     *
     * <p>Sem isto, uma variável de ambiente em falta faz a aplicação cair no
     * valor por omissão (localhost) e morrer com um "Connection refused" que não
     * diz a ninguém que o problema é configuração e não rede.
     */
    private static void registarDestinoDaBaseDeDados() {
        String url = System.getenv("DB_URL");

        if (url == null || url.isBlank()) {
            log.warn("DB_URL não está definida: vai ser usado o valor por omissão "
                    + "(localhost:5432). Em produção isto é quase de certeza um erro "
                    + "de configuração — confirma o nome da variável no serviço.");
            return;
        }

        log.info("Base de dados: {}", semCredenciais(url));
    }

    /** Um URL JDBC pode trazer a password nos parâmetros; nunca vai para o log. */
    private static String semCredenciais(String url) {
        int parametros = url.indexOf('?');
        return parametros < 0 ? url : url.substring(0, parametros);
    }
}
