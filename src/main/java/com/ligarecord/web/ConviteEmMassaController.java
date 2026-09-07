package com.ligarecord.web;

import com.ligarecord.security.GestorAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ligarecord.service.EmissaoDeConvitesService;
import com.ligarecord.service.ConviteTreinadorService;
import com.ligarecord.service.LinksDaAplicacao;
import com.ligarecord.web.dto.ConviteEmMassaDto;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Convidar de uma vez os treinadores de uma liga que ainda não têm conta.
 *
 * <p><b>Sem {@code @Transactional} aqui, de propósito.</b> São dois passos e
 * têm de ser duas transações: primeiro emitem-se os convites todos e grava-se;
 * só depois é que os emails saem, um a um. Numa transação só, uma falha a meio
 * do envio desfazia os convites já enviados e deixava links a apontar para
 * nada — a única coisa que não se pode retirar depois de sair.
 */
@RestController
@RequestMapping("/api/ligas/{ligaId}/convites-treinador")
public class ConviteEmMassaController {

    private static final Logger LOG = LoggerFactory.getLogger(ConviteEmMassaController.class);

    private final EmissaoDeConvitesService emissaoService;
    private final ConviteTreinadorService conviteService;
    private final LinksDaAplicacao links;

    public ConviteEmMassaController(EmissaoDeConvitesService emissaoService,
                                    ConviteTreinadorService conviteService,
                                    LinksDaAplicacao links) {
        this.emissaoService = emissaoService;
        this.conviteService = conviteService;
        this.links = links;
    }

    @PostMapping
    public ResponseEntity<ConviteEmMassaDto> convidarEmFalta(
            @AuthenticationPrincipal GestorAutenticado autenticado,
            @PathVariable UUID ligaId) {

        // 1. Emitir tudo e gravar. A autorização acontece lá dentro, na consulta
        //    da liga filtrada pelo dono.
        EmissaoDeConvitesService.Emissao emissao =
                emissaoService.emitirEmFalta(autenticado.getId(), ligaId);

        // 2. Só agora os emails, cada um na sua transação.
        List<ConviteEmMassaDto.Linha> linhas = new ArrayList<>();
        int enviadas = 0;

        for (EmissaoDeConvitesService.PorEntregar convite : emissao.porEntregar()) {
            // Um envio que rebente não pode levar a resposta com ele: os
            // convites já estão emitidos, e esta resposta é a única cópia dos
            // links que o gestor tem para entregar à mão.
            ConviteTreinadorService.Envio envio;
            try {
                envio = conviteService.enviarPorEmail(convite.conviteId());
            } catch (RuntimeException e) {
                LOG.error("Falhou o envio do convite {} no lote da liga {}: {}",
                        convite.conviteId(), ligaId, e.toString());
                envio = ConviteTreinadorService.Envio.FALHOU;
            }
            if (envio == ConviteTreinadorService.Envio.ENVIADO) {
                enviadas++;
            }
            linhas.add(new ConviteEmMassaDto.Linha(
                    convite.equipa(), convite.treinador(), envio.name(), links.convite(convite.codigo())));
        }

        for (EmissaoDeConvitesService.Fora fora : emissao.fora()) {
            linhas.add(new ConviteEmMassaDto.Linha(
                    fora.equipa(), fora.treinador(), fora.motivo(), null));
        }

        // no-store: a resposta leva os links todos de uma vez, e são credenciais.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new ConviteEmMassaDto(linhas, emissao.porEntregar().size(), enviadas));
    }
}
