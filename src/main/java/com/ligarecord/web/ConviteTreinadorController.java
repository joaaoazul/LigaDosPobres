package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.ConviteTreinadorService;
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
 * Convites que ligam o treinador de uma equipa a uma conta. Emitidos pelo
 * gestor dono da liga da equipa — nunca pelo treinador, que ainda não tem
 * conta nenhuma nesse momento.
 */
@RestController
@RequestMapping("/api/ligas/{ligaId}/equipas/{equipaId}/convites-treinador")
public class ConviteTreinadorController {

    private final ConviteTreinadorService conviteService;
    private final EquipaRepository equipaRepository;
    private final GestorRepository gestorRepository;

    public ConviteTreinadorController(ConviteTreinadorService conviteService,
                                      EquipaRepository equipaRepository,
                                      GestorRepository gestorRepository) {
        this.conviteService = conviteService;
        this.equipaRepository = equipaRepository;
        this.gestorRepository = gestorRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ConviteTreinadorDto> criar(@AuthenticationPrincipal GestorAutenticado autenticado,
                                                     @PathVariable UUID ligaId,
                                                     @PathVariable UUID equipaId,
                                                     @RequestBody CriarConviteTreinadorRequest pedido) {
        Equipa equipa = equipa(autenticado, ligaId, equipaId);
        Gestor gestor = gestorRepository.buscarPorId(autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));

        ConviteTreinadorDto convite = ConviteTreinadorDto.de(
                conviteService.criar(gestor, equipa.getTreinador(), pedido.diasValidade()));
        return ResponseEntity.status(HttpStatus.CREATED).body(convite);
    }

    @DeleteMapping("/{conviteId}")
    @Transactional
    public ConviteTreinadorDto revogar(@AuthenticationPrincipal GestorAutenticado autenticado,
                                       @PathVariable UUID ligaId,
                                       @PathVariable UUID equipaId,
                                       @PathVariable UUID conviteId) {
        // confirma que a equipa (e portanto o convite) é do gestor autenticado
        equipa(autenticado, ligaId, equipaId);
        return ConviteTreinadorDto.de(conviteService.revogar(conviteId, autenticado.getId()));
    }

    private Equipa equipa(GestorAutenticado autenticado, UUID ligaId, UUID equipaId) {
        Equipa equipa = equipaRepository.buscarPorIdEGestor(equipaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));
        if (!equipa.getLiga().getId().equals(ligaId)) {
            throw new RecursoNaoEncontradoException("Equipa não encontrada.");
        }
        return equipa;
    }
}
