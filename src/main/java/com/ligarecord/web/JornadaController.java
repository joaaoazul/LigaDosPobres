package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.JornadaRepository;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.service.JornadaService;
import com.ligarecord.web.dto.InserirResultadoRequest;
import com.ligarecord.web.dto.JornadaDto;
import com.ligarecord.web.dto.ResultadoDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
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

    @GetMapping("/api/ligas/{ligaId}/jornadas")
    public List<JornadaDto> listar(@PathVariable UUID ligaId) {
        return liga(ligaId).getJornadas().stream().map(JornadaDto::de).toList();
    }

    @PostMapping("/api/ligas/{ligaId}/jornadas")
    public ResponseEntity<JornadaDto> abrir(@PathVariable UUID ligaId) {
        Jornada jornada = jornadaService.abrirJornada(liga(ligaId));
        return ResponseEntity.status(HttpStatus.CREATED).body(JornadaDto.de(jornada));
    }

    @GetMapping("/api/jornadas/{jornadaId}")
    public JornadaDto detalhe(@PathVariable UUID jornadaId) {
        return JornadaDto.de(jornada(jornadaId));
    }

    @PutMapping("/api/jornadas/{jornadaId}/resultados")
    public ResultadoDto inserirResultado(@PathVariable UUID jornadaId,
                                         @RequestBody InserirResultadoRequest pedido) {
        if (pedido.equipaId() == null) {
            throw new IllegalArgumentException("A equipa é obrigatória.");
        }
        Equipa equipa = equipaRepository.buscarPorId(pedido.equipaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));

        return ResultadoDto.de(
                jornadaService.inserirResultado(jornada(jornadaId), equipa, pedido.pontuacao())
        );
    }

    @PostMapping("/api/jornadas/{jornadaId}/fechar")
    public JornadaDto fechar(@PathVariable UUID jornadaId) {
        return JornadaDto.de(jornadaService.fecharJornada(jornada(jornadaId)));
    }

    private Liga liga(UUID ligaId) {
        return ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));
    }

    private Jornada jornada(UUID jornadaId) {
        return jornadaRepository.buscarPorId(jornadaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jornada não encontrada."));
    }
}
