package com.ligarecord.security;

import com.ligarecord.repository.GestorRepository;
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

    public ContaAtivaFilter(GestorRepository gestorRepository) {
        this.gestorRepository = gestorRepository;
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
                if (pedido.getSession(false) != null) {
                    pedido.getSession(false).invalidate();
                }
                resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resposta.setContentType("application/json;charset=UTF-8");
                resposta.getWriter().write(
                        "{\"status\":401,\"mensagem\":\"A tua conta foi desativada.\"}");
                return;
            }
        }

        cadeia.doFilter(pedido, resposta);
    }

    /** Só faz sentido em pedidos que podem estar autenticados. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest pedido) {
        String caminho = pedido.getRequestURI();
        return caminho.startsWith("/api/auth/")
                || caminho.endsWith(".css")
                || caminho.endsWith(".js")
                || caminho.endsWith(".html");
    }
}
