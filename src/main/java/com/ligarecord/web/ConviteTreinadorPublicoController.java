package com.ligarecord.web;

import com.ligarecord.service.ConviteTreinadorService;
import com.ligarecord.web.dto.ConvitePublicoDto;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O que a página do convite lê antes de haver conta nenhuma.
 *
 * <p>Público por obrigação: quem chega aqui vem de um link e não tem sessão —
 * se isto exigisse autenticação, o convite levava ao login, que é exactamente
 * onde a pessoa ainda não consegue entrar. O que o torna seguro é o código:
 * 24 bytes aleatórios, que não se adivinham, e sem os quais nada disto responde.
 *
 * <p>Um código inválido, gasto, revogado ou expirado dá todos a mesma resposta,
 * pela mesma razão de sempre — não dizer a quem tenta que acertou em alguma
 * coisa.
 */
@RestController
@RequestMapping("/api/convites-treinador")
public class ConviteTreinadorPublicoController {

    private final ConviteTreinadorService conviteService;

    public ConviteTreinadorPublicoController(ConviteTreinadorService conviteService) {
        this.conviteService = conviteService;
    }

    @GetMapping("/{codigo}")
    @Transactional(readOnly = true)
    public ResponseEntity<ConvitePublicoDto> ver(@PathVariable String codigo) {
        // no-store: a resposta é sobre um convite concreto e não tem nada que
        // fazer na cache de um proxy pelo caminho.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ConvitePublicoDto.de(conviteService.exigirDisponivel(codigo)));
    }
}
