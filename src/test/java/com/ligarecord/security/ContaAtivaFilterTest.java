package com.ligarecord.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Foca-se em {@code shouldNotFilter}: só ela decide que caminhos escapam à
 * verificação de conta ativa, e um prefixo demasiado largo aqui já deixou uma
 * conta desativada continuar a confirmar sessão ou a mudar a password.
 */
class ContaAtivaFilterTest {

    private final ContaAtivaFilter filtro = new ContaAtivaFilter(null, new ObjectMapper());

    private boolean isento(String caminho) {
        MockHttpServletRequest pedido = new MockHttpServletRequest();
        pedido.setRequestURI(caminho);
        return filtro.shouldNotFilter(pedido);
    }

    @Test
    void deixaPassarOsCaminhosDeAutenticacaoSemSessaoValida() {
        assertTrue(isento("/api/auth/login"));
        assertTrue(isento("/api/auth/registo"));
        assertTrue(isento("/api/auth/registo-treinador"));
        assertTrue(isento("/api/auth/logout"));
    }

    /**
     * Estes usam uma sessão já aberta tanto quanto qualquer outro pedido
     * protegido — partilhar só o prefixo /api/auth/ com os de cima deixava
     * uma conta desativada continuar a confirmar sessão, mudar a password ou
     * ligar um convite de treinador.
     */
    @Test
    void naoDeixaPassarOsCaminhosDeAutenticacaoComSessaoJaAberta() {
        assertFalse(isento("/api/auth/estado"));
        assertFalse(isento("/api/auth/password"));
        assertFalse(isento("/api/auth/treinador/ligar"));
    }

    @Test
    void deixaPassarOsRecursosEstaticos() {
        assertTrue(isento("/styles.css"));
        assertTrue(isento("/app.js"));
        assertTrue(isento("/index.html"));
    }

    @Test
    void naoDeixaPassarUmaRotaComumDaApi() {
        assertFalse(isento("/api/ligas"));
    }
}
