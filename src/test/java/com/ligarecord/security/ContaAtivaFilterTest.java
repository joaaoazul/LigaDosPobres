package com.ligarecord.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.GestorRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    /**
     * O GestorAutenticado é uma fotografia tirada no login e vive na sessão.
     * Sem o refrescar a cada pedido, tirar a alguém o direito de criar ligas
     * não tinha efeito nenhum enquanto essa pessoa tivesse sessão aberta: as
     * autoridades continuavam a ser as do momento em que entrou.
     */
    @Test
    void refrescaAsPermissoesDaSessaoAPartirDaBaseDeDados() throws Exception {
        GestorRepository gestores = new GestorRepositoryImpl();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        gestores.guardar(gestor);

        // A sessão foi aberta quando ele ainda podia criar ligas.
        GestorAutenticado noLogin = new GestorAutenticado(gestor);
        assertTrue(temPermissaoDeCriarLigas(noLogin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(noLogin, null, noLogin.getAuthorities()));

        // Entretanto um administrador tira-lhe a permissão.
        gestor.setPodeCriarLigas(false);
        gestores.guardar(gestor);

        MockHttpServletRequest pedido = new MockHttpServletRequest();
        pedido.setRequestURI("/api/ligas");
        new ContaAtivaFilter(gestores, new ObjectMapper())
                .doFilter(pedido, new MockHttpServletResponse(), new MockFilterChain());

        GestorAutenticado depois = (GestorAutenticado) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertFalse(temPermissaoDeCriarLigas(depois));
        assertEquals(gestor.getId(), depois.getId());
    }

    /** Uma conta desativada a meio da sessão continua a ser barrada. */
    @Test
    void contaDesativadaAMeioDaSessaoLevaComUm401() throws Exception {
        GestorRepository gestores = new GestorRepositoryImpl();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        gestores.guardar(gestor);

        GestorAutenticado noLogin = new GestorAutenticado(gestor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(noLogin, null, noLogin.getAuthorities()));

        gestor.setAtivo(false);
        gestores.guardar(gestor);

        MockHttpServletRequest pedido = new MockHttpServletRequest();
        pedido.setRequestURI("/api/ligas");
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        new ContaAtivaFilter(gestores, new ObjectMapper()).doFilter(pedido, resposta, new MockFilterChain());

        assertEquals(401, resposta.getStatus());
        assertFalse(resposta.getContentAsString().isBlank());
    }

    private boolean temPermissaoDeCriarLigas(GestorAutenticado conta) {
        return conta.getAuthorities().stream()
                .anyMatch(autoridade -> autoridade.getAuthority().equals("PODE_CRIAR_LIGAS"));
    }
}
