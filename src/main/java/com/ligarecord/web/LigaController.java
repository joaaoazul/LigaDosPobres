package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.service.ClassificacaoService;
import com.ligarecord.service.LigaService;
import com.ligarecord.web.dto.AdicionarEquipaRequest;
import com.ligarecord.web.dto.ClassificacaoDto;
import com.ligarecord.web.dto.CriarLigaRequest;
import com.ligarecord.web.dto.EquipaDto;
import com.ligarecord.web.dto.LigaDetalheDto;
import com.ligarecord.web.dto.LigaDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ligas")
public class LigaController {

    private final LigaService ligaService;
    private final ClassificacaoService classificacaoService;
    private final LigaRepository ligaRepository;
    private final EquipaRepository equipaRepository;

    public LigaController(LigaService ligaService,
                          ClassificacaoService classificacaoService,
                          LigaRepository ligaRepository,
                          EquipaRepository equipaRepository) {
        this.ligaService = ligaService;
        this.classificacaoService = classificacaoService;
        this.ligaRepository = ligaRepository;
        this.equipaRepository = equipaRepository;
    }

    @GetMapping
    public List<LigaDto> listar() {
        return ligaRepository.listarLigas().stream().map(LigaDto::de).toList();
    }

    @PostMapping
    public ResponseEntity<LigaDto> criar(@RequestBody CriarLigaRequest pedido) {
        Liga liga = ligaService.criarLiga(pedido.nome(), pedido.maxEquipas());
        return ResponseEntity.status(HttpStatus.CREATED).body(LigaDto.de(liga));
    }

    @GetMapping("/{ligaId}")
    public LigaDetalheDto detalhe(@PathVariable UUID ligaId) {
        Liga liga = liga(ligaId);
        return LigaDetalheDto.de(liga, classificacao(liga));
    }

    @GetMapping("/{ligaId}/classificacao")
    public List<ClassificacaoDto> classificacao(@PathVariable UUID ligaId) {
        return classificacao(liga(ligaId));
    }

    @PostMapping("/{ligaId}/terminar")
    public LigaDto terminar(@PathVariable UUID ligaId) {
        return LigaDto.de(ligaService.terminarLiga(liga(ligaId)));
    }

    @PostMapping("/{ligaId}/equipas")
    public ResponseEntity<EquipaDto> adicionarEquipa(@PathVariable UUID ligaId,
                                                     @RequestBody AdicionarEquipaRequest pedido) {
        if (pedido.nome() == null || pedido.nome().isBlank()) {
            throw new IllegalArgumentException("O nome da equipa é obrigatório.");
        }
        if (pedido.treinador() == null || pedido.treinador().isBlank()) {
            throw new IllegalArgumentException("O nome do treinador é obrigatório.");
        }

        Treinador treinador = new Treinador(UUID.randomUUID(), pedido.treinador().trim());
        Equipa equipa = new Equipa(
                UUID.randomUUID(),
                pedido.nome().trim(),
                treinador,
                null,
                EstadoEquipa.ATIVA
        );

        Equipa guardada = ligaService.adicionarEquipa(liga(ligaId), equipa);
        return ResponseEntity.status(HttpStatus.CREATED).body(EquipaDto.de(guardada));
    }

    @PostMapping("/{ligaId}/equipas/{equipaId}/desistencia")
    public EquipaDto registarDesistencia(@PathVariable UUID ligaId, @PathVariable UUID equipaId) {
        Equipa equipa = equipaRepository.buscarPorId(equipaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));
        return EquipaDto.de(ligaService.registarDesistencia(liga(ligaId), equipa));
    }

    private List<ClassificacaoDto> classificacao(Liga liga) {
        return classificacaoService.calcularClassificacao(liga).stream().map(ClassificacaoDto::de).toList();
    }

    private Liga liga(UUID ligaId) {
        return ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));
    }
}
