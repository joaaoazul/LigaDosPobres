package com.ligarecord.web;

import com.ligarecord.service.SuporteService;
import com.ligarecord.web.dto.MensagemSuporteRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * O formulário de contacto, aberto a quem não tem sessão — é para quem não
 * consegue entrar que ele serve. O travão ao abuso vive no
 * {@link SuporteService}; aqui só se apanha a origem do pedido, que é a única
 * coisa que o serviço não consegue ver sozinho.
 */
@RestController
@RequestMapping("/api/suporte")
public class SuporteController {

    private final SuporteService suporteService;

    public SuporteController(SuporteService suporteService) {
        this.suporteService = suporteService;
    }

    /**
     * Responde sempre 200 a uma mensagem aceite, mesmo que o email não tenha
     * saído: ela ficou guardada, e quem escreveu não ganha nada em saber que o
     * fornecedor de email está em baixo. O {@code enviado} diz-lhe é se pode
     * contar com resposta pelo caminho do costume.
     */
    @PostMapping
    public Map<String, Boolean> receber(@RequestBody MensagemSuporteRequest pedido,
                                        HttpServletRequest pedidoHttp) {
        boolean enviado = suporteService.receber(
                pedido.nome(), pedido.email(), pedido.assunto(), pedido.mensagem(),
                // Atrás do Caddy e da Cloudflare isto é o IP de quem visita, e
                // não o do proxy, porque FORWARD_HEADERS está ligado (ver
                // application.properties). Sem isso seria o mesmo para toda a
                // gente, e o travão do serviço passava a ser global.
                pedidoHttp.getRemoteAddr());
        return Map.of("enviado", enviado);
    }
}
