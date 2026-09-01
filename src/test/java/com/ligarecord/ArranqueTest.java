package com.ligarecord;

import com.ligarecord.repository.LigaLogoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arranca a aplicação inteira contra um Postgres a sério.
 *
 * <p>Isto existe por uma razão concreta: {@code spring.jpa.hibernate.ddl-auto} é
 * {@code validate}, portanto qualquer desencontro entre uma entidade e as
 * migrações do Flyway não é um teste vermelho — é a aplicação a recusar
 * arrancar. Sem um teste que levante o contexto, esse desencontro só aparecia
 * em produção, no deploy, com o serviço em baixo.
 *
 * <p>Os outros testes são unitários e correm sem base de dados nenhuma; este é
 * o único que paga o preço de um contentor, e paga-o por todos.
 */
@SpringBootTest
@Testcontainers
class ArranqueTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private LigaLogoRepository ligaLogoRepository;

    @Test
    void aplicacaoArrancaEAsEntidadesBatemCertoComAsMigracoes() {
        // O corpo é quase vazio de propósito: se as migrações e as entidades não
        // coincidirem, o contexto nem chega a subir e o teste falha aqui.
        assertThat(ligaLogoRepository).isNotNull();
    }

    /**
     * O logo é lido e apagado por SQL nativo, que o Hibernate não valida contra
     * o esquema por não ser JPQL. Um nome de coluna errado nessas consultas só
     * daria erro no primeiro upload real; aqui dá erro no CI.
     */
    @Test
    @Transactional
    void asConsultasNativasDoLogoCorrespondemAoEsquema() {
        UUID ligaSemLogo = UUID.randomUUID();

        assertThat(ligaLogoRepository.buscar(ligaSemLogo)).isEmpty();

        // Apagar um logo que não existe não afecta linha nenhuma nem rebenta —
        // é a garantia que substituiu o existsById() que aqui estava antes.
        ligaLogoRepository.apagar(ligaSemLogo);

        assertThat(ligaLogoRepository.buscar(ligaSemLogo)).isEmpty();
    }
}
