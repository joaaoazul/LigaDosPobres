package com.ligarecord.security;

import com.ligarecord.web.dto.ErroDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
                    .requestMatchers("/login.html", "/registo.html", "/styles.css", "/auth.js",
                                     "/favicon.ico", "/erro.html").permitAll()
                    .requestMatchers("/api/auth/registo", "/api/auth/login", "/api/auth/estado").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated())
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(this::naoAutenticado)
                    .accessDeniedHandler(this::acessoNegado))
            .logout(l -> l.disable());

        return http.build();
    }

    private void acessoNegado(jakarta.servlet.http.HttpServletRequest pedido,
                              HttpServletResponse resposta,
                              org.springframework.security.access.AccessDeniedException excecao)
            throws java.io.IOException {
        resposta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(resposta.getWriter(),
                new ErroDto(403, "Pedido rejeitado por falta de token de segurança. Recarrega a página."));
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
