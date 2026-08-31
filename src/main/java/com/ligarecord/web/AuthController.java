package com.ligarecord.web;

import com.ligarecord.domain.Gestor;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.GestorService;
import com.ligarecord.web.dto.AlterarPasswordRequest;
import com.ligarecord.web.dto.GestorDto;
import com.ligarecord.web.dto.LoginRequest;
import com.ligarecord.web.dto.RegistoRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GestorService gestorService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(GestorService gestorService, AuthenticationManager authenticationManager) {
        this.gestorService = gestorService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/registo")
    public ResponseEntity<GestorDto> registar(@RequestBody RegistoRequest pedido,
                                              HttpServletRequest http,
                                              HttpServletResponse resposta) {
        Gestor gestor = gestorService.registar(
                pedido.email(), pedido.password(), pedido.nome(), pedido.codigo());

        // Regista e inicia sessão de imediato: evita pedir a password duas vezes seguidas.
        autenticar(pedido.email(), pedido.password(), http, resposta);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GestorDto(gestor.getId(), gestor.getNome(), gestor.getEmail(), gestor.isAdmin()));
    }

    @PostMapping("/login")
    public GestorDto login(@RequestBody LoginRequest pedido,
                           HttpServletRequest http,
                           HttpServletResponse resposta) {
        try {
            Authentication autenticacao = autenticar(pedido.email(), pedido.password(), http, resposta);
            return GestorDto.de((GestorAutenticado) autenticacao.getPrincipal());
        } catch (AuthenticationException e) {
            // Mensagem igual para email inexistente e password errada: não confirma
            // a quem tenta se um dado email tem conta.
            throw new CredenciaisInvalidasException("Email ou password incorretos.");
        }
    }

    /**
     * Muda a password do gestor com sessão aberta.
     *
     * <p>A sessão é invalidada a seguir, de propósito: quem mudou a password
     * volta a entrar com a nova, e qualquer sessão aberta noutro sítio deixa de
     * servir. Se a password foi mudada por se suspeitar que alguém a sabia, uma
     * sessão que continuasse viva tornava a mudança inútil.
     */
    @PostMapping("/password")
    public ResponseEntity<Void> alterarPassword(@AuthenticationPrincipal GestorAutenticado autenticado,
                                                @RequestBody AlterarPasswordRequest pedido,
                                                HttpServletRequest http) {
        gestorService.alterarPassword(autenticado.getId(), pedido.atual(), pedido.nova());

        HttpSession sessao = http.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        HttpSession sessao = http.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /** Usado pelo frontend para saber se deve mostrar a aplicação ou a página de login. */
    @GetMapping("/estado")
    public ResponseEntity<GestorDto> estado(Authentication autenticacao) {
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof GestorAutenticado gestor)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(GestorDto.de(gestor));
    }

    private Authentication autenticar(String email, String password,
                                      HttpServletRequest http, HttpServletResponse resposta) {
        Authentication autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email == null ? "" : email.trim().toLowerCase(), password));

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        contextRepository.saveContext(contexto, http, resposta);

        return autenticacao;
    }
}
