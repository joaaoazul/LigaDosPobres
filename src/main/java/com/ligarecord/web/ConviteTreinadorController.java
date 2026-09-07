package com.ligarecord.web;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Equipa;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.ConviteTreinadorService;
import com.ligarecord.service.EmissaoDeConvitesService;
import com.ligarecord.service.LinksDaAplicacao;
import com.ligarecord.web.dto.ConviteTreinadorDto;
import com.ligarecord.web.dto.CriarConviteTreinadorRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Convites ao lugar de treinador de uma equipa. Emitidos pelo gestor dono da
 * liga — nunca pelo treinador, que ainda não tem conta nenhuma nesse momento.
 */
@RestController
@RequestMapping("/api/ligas/{ligaId}/equipas/{equipaId}/convites-treinador")
public class ConviteTreinadorController {

    private final ConviteTreinadorService conviteService;
    private final EmissaoDeConvitesService emissaoService;
    private final EquipaRepository equipaRepository;
    private final LinksDaAplicacao links;

    public ConviteTreinadorController(ConviteTreinadorService conviteService,
                                      EmissaoDeConvitesService emissaoService,
                                      EquipaRepository equipaRepository,
                                      LinksDaAplicacao links) {
        this.conviteService = conviteService;
        this.emissaoService = emissaoService;
        this.equipaRepository = equipaRepository;
        this.links = links;
    }

    /**
     * Emite o convite, ou devolve o que já lá estiver por usar — daí o
     * {@code 200} quando não houve nada a criar. Carregar duas vezes no botão
     * dá o mesmo link, e não duas credenciais para a mesma equipa.
     *
     * <p>Se o lugar tiver email, tenta entregá-lo por lá. O resultado vem no
     * campo {@code envio}, e o link vem sempre: um envio que não saiu não pode
     * deixar o gestor sem maneira de convidar.
     *
     * <p><b>Sem {@code @Transactional} aqui, de propósito</b>, ao contrário do
     * revogar mais abaixo: são dois passos e têm de ser duas transações. O
     * convite é emitido e gravado primeiro; só depois é que o email sai. Ao
     * contrário, uma falha a gravar deixava uma mensagem entregue a apontar
     * para um convite que não existe — e um email não se retira depois de sair.
     */
    @PostMapping
    public ResponseEntity<ConviteTreinadorDto> emitir(@AuthenticationPrincipal GestorAutenticado autenticado,
                                                      @PathVariable UUID ligaId,
                                                      @PathVariable UUID equipaId,
                                                      @RequestBody CriarConviteTreinadorRequest pedido) {
        EmissaoDeConvitesService.PorEntregar emitido = emissaoService.emitirParaEquipa(
                autenticado.getId(), ligaId, equipaId, pedido.diasValidade());

        ConviteTreinadorService.Envio envio = conviteService.enviarPorEmail(emitido.conviteId());

        return ResponseEntity
                .status(emitido.novo() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ConviteTreinadorDto.deEmissao(emitido, links.convite(emitido.codigo()), envio));
    }

    @DeleteMapping("/{conviteId}")
    @Transactional
    public ConviteTreinadorDto revogar(@AuthenticationPrincipal GestorAutenticado autenticado,
                                       @PathVariable UUID ligaId,
                                       @PathVariable UUID equipaId,
                                       @PathVariable UUID conviteId) {
        // confirma que a equipa (e portanto o convite) é de quem está a pedir
        equipa(autenticado, ligaId, equipaId);
        return dto(conviteService.revogar(conviteId, equipaId));
    }

    private ConviteTreinadorDto dto(ConviteTreinador convite) {
        return ConviteTreinadorDto.de(convite, links.convite(convite.getCodigo()));
    }

    private Equipa equipa(GestorAutenticado autenticado, UUID ligaId, UUID equipaId) {
        Equipa equipa = equipaRepository.buscarPorIdEGestor(equipaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));
        if (equipa.getLiga() == null || !equipa.getLiga().getId().equals(ligaId)) {
            throw new RecursoNaoEncontradoException("Equipa não encontrada.");
        }
        return equipa;
    }
}
