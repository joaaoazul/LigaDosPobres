package com.ligarecord.security;

import com.ligarecord.web.dto.ErroDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final ContaAtivaFilter contaAtivaFilter;

    public SecurityConfig(ObjectMapper objectMapper, ContaAtivaFilter contaAtivaFilter) {
        this.objectMapper = objectMapper;
        this.contaAtivaFilter = contaAtivaFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // O token CSRF vai num cookie legível por JavaScript para o frontend o
        // reenviar no cabeçalho X-XSRF-TOKEN. Sem isto, um site externo conseguia
        // fazer pedidos autenticados em nome de quem tem sessão aberta.
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();

        // Por omissão o Spring Security 6 só materializa o token quando alguém lhe
        // toca, o que faz o cookie não existir no primeiro pedido de um browser
        // novo — e o primeiro POST falhar. Desligar o atributo diferido força o
        // token a ser escrito em todos os pedidos.
        CsrfTokenRequestAttributeHandler manipuladorCsrf = new CsrfTokenRequestAttributeHandler();
        manipuladorCsrf.setCsrfRequestAttributeName(null);

        http
            .csrf(c -> c
                    .csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(manipuladorCsrf))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(a -> a
                    .requestMatchers("/login.html", "/registo.html", "/registo-treinador.html", "/styles.css",
                                     "/auth.js", "/login.js", "/registo.js", "/registo-treinador.js",
                                     "/favicon.ico").permitAll()
                    // Quem vem recuperar a password não tem sessão nenhuma: se
                    // estas páginas exigissem autenticação, o link do email
                    // levava ao login, que é precisamente onde a pessoa não
                    // consegue entrar.
                    .requestMatchers("/recuperar.html", "/recuperar.js",
                                     "/nova-password.html", "/nova-password.js").permitAll()
                    // Pela mesma razão, a página do convite de treinador: quem
                    // chega pelo link ainda não tem conta nenhuma para entrar.
                    // O que a protege é o código, que não se adivinha.
                    .requestMatchers("/convite.html", "/convite.js").permitAll()
                    // Os tipos de letra são servidos pela própria aplicação, para a
                    // CSP poder continuar a ser default-src 'self'. Sem esta linha,
                    // o pedido do .woff2 na página de login seria reencaminhado para
                    // o login e a página caía no tipo de letra do sistema.
                    .requestMatchers("/fontes/**").permitAll()
                    .requestMatchers("/api/auth/registo", "/api/auth/registo-treinador",
                                     "/api/auth/login", "/api/auth/estado",
                                     "/api/auth/recuperar", "/api/auth/recuperar/confirmar").permitAll()
                    // Só a leitura do convite pelo código; emitir e revogar
                    // continuam atrás da autorização por dono da liga.
                    .requestMatchers(HttpMethod.GET, "/api/convites-treinador/*").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    // A administração é a única zona com autorização por papel;
                    // todo o resto é isolado por dono, não por perfil — com uma
                    // excepção: criar uma liga exige a permissão explícita
                    // PODE_CRIAR_LIGAS, para quem só foi convidado a treinar uma
                    // equipa não ficar, de brinde, a poder criar as suas próprias.
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/ligas").hasAuthority("PODE_CRIAR_LIGAS")
                    .anyRequest().authenticated())
            .addFilterAfter(contaAtivaFilter,
                    org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(this::naoAutenticado)
                    .accessDeniedHandler(this::acessoNegado))
            .logout(l -> l.disable());

        return http.build();
    }

    /**
     * Dois motivos distintos dão 403 e não se devem confundir: falta o token
     * CSRF (o utilizador recarrega a página e resolve-se) ou a conta não tem
     * permissão (recarregar não resolve nada).
     */
    private void acessoNegado(jakarta.servlet.http.HttpServletRequest pedido,
                              HttpServletResponse resposta,
                              org.springframework.security.access.AccessDeniedException excecao)
            throws java.io.IOException {

        boolean falhaCsrf = excecao instanceof org.springframework.security.web.csrf.CsrfException;

        resposta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(resposta.getWriter(), new ErroDto(403, falhaCsrf
                ? "Pedido rejeitado por falta de token de segurança. Recarrega a página."
                : "Não tens permissão para esta operação."));
    }

    /**
     * Uma chamada à API sem sessão recebe 401 em JSON, para o JavaScript poder
     * tratar o caso. Já alguém a navegar para uma página é reencaminhado para o
     * login: devolver JSON ao browser deixaria o utilizador a olhar para texto
     * cru em vez de um ecrã de entrada.
     */
    private void naoAutenticado(jakarta.servlet.http.HttpServletRequest pedido,
                                HttpServletResponse resposta,
                                org.springframework.security.core.AuthenticationException excecao)
            throws java.io.IOException {

        if (pedido.getRequestURI().startsWith("/api/")) {
            resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resposta.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(resposta.getWriter(), new ErroDto(401, "Sessão não iniciada."));
            return;
        }

        resposta.sendRedirect("/login.html");
    }
}
