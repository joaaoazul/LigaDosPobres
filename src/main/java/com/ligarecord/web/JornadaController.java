package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.JornadaRepository;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.JornadaService;
import com.ligarecord.web.dto.InserirResultadoRequest;
import com.ligarecord.web.dto.JornadaDto;
import com.ligarecord.web.dto.ResultadoDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * As rotas estão aninhadas na liga de propósito. Um caminho como
 * /api/jornadas/{id} não diz a que liga pertence a jornada, o que torna fácil
 * esquecer a verificação de dono; aqui a liga é sempre resolvida primeiro.
 */
@RestController
@RequestMapping("/api/ligas/{ligaId}/jornadas")
public class JornadaController {

    private final JornadaService jornadaService;
    private final LigaRepository ligaRepository;
    private final JornadaRepository jornadaRepository;
    private final EquipaRepository equipaRepository;

    public JornadaController(JornadaService jornadaService,
                             LigaRepository ligaRepository,
                             JornadaRepository jornadaRepository,
                             EquipaRepository equipaRepository) {
        this.jornadaService = jornadaService;
        this.ligaRepository = ligaRepository;
        this.jornadaRepository = jornadaRepository;
        this.equipaRepository = equipaRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<JornadaDto> listar(@AuthenticationPrincipal GestorAutenticado autenticado,
                                   @PathVariable UUID ligaId) {
        return liga(autenticado, ligaId).getJornadas().stream().map(JornadaDto::de).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<JornadaDto> abrir(@AuthenticationPrincipal GestorAutenticado autenticado,
                                            @PathVariable UUID ligaId) {
        Jornada jornada = jornadaService.abrirJornada(liga(autenticado, ligaId));
        return ResponseEntity.status(HttpStatus.CREATED).body(JornadaDto.de(jornada));
    }

    @GetMapping("/{jornadaId}")
    @Transactional(readOnly = true)
    public JornadaDto detalhe(@AuthenticationPrincipal GestorAutenticado autenticado,
                              @PathVariable UUID ligaId,
                              @PathVariable UUID jornadaId) {
        return JornadaDto.de(jornada(autenticado, ligaId, jornadaId));
    }

    @PutMapping("/{jornadaId}/resultados")
    @Transactional
    public ResultadoDto inserirResultado(@AuthenticationPrincipal GestorAutenticado autenticado,
                                         @PathVariable UUID ligaId,
                                         @PathVariable UUID jornadaId,
                                         @RequestBody InserirResultadoRequest pedido) {
        if (pedido.equipaId() == null) {
            throw new IllegalArgumentException("A equipa é obrigatória.");
        }
        Jornada jornada = jornada(autenticado, ligaId, jornadaId);
        Equipa equipa = equipaRepository.buscarPorIdEGestor(pedido.equipaId(), autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));

        if (!equipa.getLiga().getId().equals(ligaId)) {
            throw new RecursoNaoEncontradoException("Equipa não encontrada.");
        }

        return ResultadoDto.de(jornadaService.inserirResultado(jornada, equipa, pedido.pontuacao()));
    }

    @PostMapping("/{jornadaId}/fechar")
    @Transactional
    public JornadaDto fechar(@AuthenticationPrincipal GestorAutenticado autenticado,
                             @PathVariable UUID ligaId,
                             @PathVariable UUID jornadaId) {
        return JornadaDto.de(jornadaService.fecharJornada(jornada(autenticado, ligaId, jornadaId)));
    }

    private Liga liga(GestorAutenticado autenticado, UUID ligaId) {
        return ligaRepository.buscarPorIdEGestor(ligaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));
    }

    private Jornada jornada(GestorAutenticado autenticado, UUID ligaId, UUID jornadaId) {
        Jornada jornada = jornadaRepository.buscarPorIdEGestor(jornadaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jornada não encontrada."));
        if (!jornada.getLiga().getId().equals(ligaId)) {
            throw new RecursoNaoEncontradoException("Jornada não encontrada.");
        }
        return jornada;
    }
}
