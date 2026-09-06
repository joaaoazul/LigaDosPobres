package com.ligarecord.web;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.ConviteTreinadorService;
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
    private final EquipaRepository equipaRepository;
    private final GestorRepository gestorRepository;
    private final LinksDaAplicacao links;

    public ConviteTreinadorController(ConviteTreinadorService conviteService,
                                      EquipaRepository equipaRepository,
                                      GestorRepository gestorRepository,
                                      LinksDaAplicacao links) {
        this.conviteService = conviteService;
        this.equipaRepository = equipaRepository;
        this.gestorRepository = gestorRepository;
        this.links = links;
    }

    /**
     * Emite o convite, ou devolve o que já lá estiver por usar — daí o
     * {@code 200} quando não houve nada a criar. Carregar duas vezes no botão
     * dá o mesmo link, e não duas credenciais para a mesma equipa.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<ConviteTreinadorDto> emitir(@AuthenticationPrincipal GestorAutenticado autenticado,
                                                      @PathVariable UUID ligaId,
                                                      @PathVariable UUID equipaId,
                                                      @RequestBody CriarConviteTreinadorRequest pedido) {
        Equipa equipa = equipa(autenticado, ligaId, equipaId);
        Gestor gestor = gestorRepository.buscarPorId(autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));

        ConviteTreinadorService.Emissao emissao =
                conviteService.emitir(gestor, equipa, pedido.diasValidade());

        return ResponseEntity
                .status(emissao.novo() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(dto(emissao.convite()));
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
