package com.ligarecord.security;

import com.ligarecord.repository.GestorRepository;
import com.ligarecord.web.dto.ErroDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Verifica, a cada pedido autenticado, que a conta continua ativa.
 *
 * <p>Sem isto, desativar um gestor só teria efeito no próximo início de sessão:
 * quem já estivesse autenticado continuava a trabalhar com a sessão aberta, que
 * é precisamente o cenário em que se desativa uma conta com urgência.
 *
 * <p>Custa uma consulta por chave primária em cada pedido. É o preço de o
 * bloqueio ser imediato.
 */
@Component
public class ContaAtivaFilter extends OncePerRequestFilter {

    private final GestorRepository gestorRepository;
    private final ObjectMapper objectMapper;

    public ContaAtivaFilter(GestorRepository gestorRepository, ObjectMapper objectMapper) {
        this.gestorRepository = gestorRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pedido,
                                    HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao != null && autenticacao.getPrincipal() instanceof GestorAutenticado gestor) {
            boolean continuaAtivo = gestorRepository.buscarPorId(gestor.getId())
                    .map(com.ligarecord.domain.Gestor::isAtivo)
                    .orElse(false);

            if (!continuaAtivo) {
                SecurityContextHolder.clearContext();
                jakarta.servlet.http.HttpSession sessao = pedido.getSession(false);
                if (sessao != null) {
                    sessao.invalidate();
                }
                resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resposta.setContentType("application/json;charset=UTF-8");
                objectMapper.writeValue(resposta.getWriter(), new ErroDto(401, "A tua conta foi desativada."));
                return;
            }
        }

        cadeia.doFilter(pedido, resposta);
    }

    /**
     * Só faz sentido em pedidos que podem estar autenticados. Isto exclui só
     * os caminhos de autenticação que não pressupõem uma sessão já válida
     * (entrar, registar, sair) — {@code /api/auth/estado},
     * {@code /api/auth/password} e {@code /api/auth/treinador/ligar} usam
     * uma sessão existente tanto quanto qualquer outro pedido protegido, e
     * excluí-los todos só por partilharem o prefixo {@code /api/auth/}
     * deixava uma conta desativada continuar a confirmar sessão, mudar a
     * password ou ligar um convite — o oposto do que esta classe promete.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest pedido) {
        String caminho = pedido.getRequestURI();
        if (caminho.endsWith(".css") || caminho.endsWith(".js") || caminho.endsWith(".html")) {
            return true;
        }
        return caminho.equals("/api/auth/registo")
                || caminho.equals("/api/auth/registo-treinador")
                || caminho.equals("/api/auth/login")
                || caminho.equals("/api/auth/logout");
    }
}
