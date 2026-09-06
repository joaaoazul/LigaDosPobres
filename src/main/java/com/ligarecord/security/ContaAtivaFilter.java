package com.ligarecord.security;

import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.web.dto.ErroDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

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
            Optional<Gestor> atual = gestorRepository.buscarPorId(gestor.getId());

            if (atual.isEmpty() || !atual.get().isAtivo()) {
                terminar(pedido, resposta, "A tua conta foi desativada.");
                return;
            }

            if (sessaoAnteriorAUmaMudancaDePassword(pedido, atual.get())) {
                terminar(pedido, resposta, "A password desta conta foi alterada. Entra outra vez.");
                return;
            }

            trocarPeloPrincipalFresco(autenticacao, atual.get());
        }

        cadeia.doFilter(pedido, resposta);
    }

    /**
     * Uma sessão aberta antes da última mudança de password deixou de valer.
     *
     * <p>A sessão vive do lado do servidor e não sabe nada da password, por isso
     * mudá-la não expulsava ninguém: quem já lá estivesse dentro continuava lá.
     * Numa recuperação de password é o pior caso possível, porque a razão para a
     * recuperar costuma ser haver alguém na conta que não devia estar.
     *
     * <p>Uma conta que nunca mudou a password não tem data de corte e não passa
     * por aqui.
     */
    private boolean sessaoAnteriorAUmaMudancaDePassword(HttpServletRequest pedido, Gestor atual) {
        Instant corte = atual.getSessoesValidasDesde();
        if (corte == null) {
            return false;
        }
        HttpSession sessao = pedido.getSession(false);
        return sessao != null && Instant.ofEpochMilli(sessao.getCreationTime()).isBefore(corte);
    }

    private void terminar(HttpServletRequest pedido, HttpServletResponse resposta, String mensagem)
            throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession sessao = pedido.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(resposta.getWriter(), new ErroDto(401, mensagem));
    }

    /**
     * Reconstrói o principal a partir da linha que acabámos de ler, para este
     * pedido.
     *
     * <p>O {@link GestorAutenticado} é uma fotografia tirada no login e vive na
     * sessão, por isso as permissões ficavam congeladas nesse momento: tirar a
     * um gestor o direito de criar ligas não tinha efeito nenhum enquanto ele
     * tivesse sessão aberta — continuava a criá-las até sair e voltar a entrar.
     * Não custa uma consulta a mais: é a mesma linha que já foi lida para
     * confirmar que a conta continua ativa.
     *
     * <p>Só o contexto deste pedido é trocado. A sessão fica como está, e é
     * relida da base de dados no pedido seguinte de qualquer forma.
     */
    private void trocarPeloPrincipalFresco(Authentication autenticacao, Gestor atual) {
        GestorAutenticado fresco = new GestorAutenticado(atual);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                fresco, autenticacao.getCredentials(), fresco.getAuthorities());
        token.setDetails(autenticacao.getDetails());
        SecurityContextHolder.getContext().setAuthentication(token);
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
                || caminho.equals("/api/auth/logout")
                || caminho.startsWith("/api/auth/recuperar");
    }
}
