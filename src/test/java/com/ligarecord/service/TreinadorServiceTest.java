package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.TreinadorRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreinadorServiceTest {

    private TreinadorService servico;

    @BeforeEach
    void setUp() {
        servico = new TreinadorService(new TreinadorRepositoryImpl());
    }

    @Test
    void criaOLugarComNomeEEmail() {
        Treinador treinador = servico.novo("  João Azul  ", "Joao@Exemplo.PT");

        assertEquals("João Azul", treinador.getNome());
        // minúsculas, como em qualquer email da aplicação
        assertEquals("joao@exemplo.pt", treinador.getEmail());
    }

    /** Nem todo o treinador tem email, e isso não é um campo por preencher. */
    @Test
    void oEmailEOpcional() {
        assertNull(servico.novo("João Azul", null).getEmail());
        assertNull(servico.novo("João Azul", "   ").getEmail());
        assertFalse(servico.novo("João Azul", null).temEmail());
    }

    @Test
    void recusaEmailInvalido() {
        assertThrows(IllegalArgumentException.class, () -> servico.novo("João Azul", "nao-e-email"));
    }

    @Test
    void recusaNomeVazio() {
        assertThrows(IllegalArgumentException.class, () -> servico.novo("  ", "joao@exemplo.pt"));
    }

    @Test
    void alterarApagaOEmailQuandoVemVazio() {
        Treinador treinador = servico.novo("João Azul", "joao@exemplo.pt");

        servico.alterar(treinador, "João A. Azul", "");

        assertEquals("João A. Azul", treinador.getNome());
        assertNull(treinador.getEmail());
    }

    @Test
    void desligarContaLibertaOLugar() {
        Treinador treinador = servico.novo("João Azul", null);
        treinador.setConta(new Gestor(UUID.randomUUID(), "joao@exemplo.pt", "hash", "João"));

        servico.desligarConta(treinador);

        assertFalse(treinador.temConta());
    }

    @Test
    void desligarContaDeUmLugarSemContaEUmConflito() {
        Treinador treinador = servico.novo("João Azul", null);

        assertThrows(IllegalStateException.class, () -> servico.desligarConta(treinador));
    }

    @Test
    void alterarGuardaOLugar() {
        Treinador treinador = servico.novo("João Azul", null);

        assertTrue(servico.alterar(treinador, "Zé", "ze@exemplo.pt").temEmail());
    }
}
