package com.ligarecord.web;

import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoDivida;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.DividaService;
import com.ligarecord.web.dto.BlocoDividaDto;
import com.ligarecord.web.dto.DividaDto;
import com.ligarecord.web.dto.RegistarBlocoRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Dívidas de equipas. Só o gestor fecha blocos e marca pagamentos — a vista
 * do treinador sobre as suas próprias dívidas vive em
 * {@link MinhasDividasController}.
 */
@RestController
@RequestMapping("/api/ligas/{ligaId}")
public class DividaController {

    private final DividaService dividaService;
    private final LigaRepository ligaRepository;
    private final EquipaRepository equipaRepository;

    public DividaController(DividaService dividaService,
                            LigaRepository ligaRepository,
                            EquipaRepository equipaRepository) {
        this.dividaService = dividaService;
        this.ligaRepository = ligaRepository;
        this.equipaRepository = equipaRepository;
    }

    @GetMapping("/dividas")
    @Transactional(readOnly = true)
    public List<DividaDto> listarDaLiga(@AuthenticationPrincipal GestorAutenticado autenticado,
                                        @PathVariable UUID ligaId,
                                        @RequestParam(defaultValue = "PENDENTE") EstadoDivida estado) {
        Liga liga = liga(autenticado, ligaId);
        return dividaService.listarPorLiga(liga, estado).stream()
                .map(this::comTotal)
                .toList();
    }

    @GetMapping("/equipas/{equipaId}/divida")
    @Transactional(readOnly = true)
    public DividaDto detalhe(@AuthenticationPrincipal GestorAutenticado autenticado,
                             @PathVariable UUID ligaId,
                             @PathVariable UUID equipaId) {
        Equipa equipa = equipa(autenticado, ligaId, equipaId);
        return dividaService.buscarPorEquipa(equipa)
                .map(this::comTotal)
                .orElseGet(() -> DividaDto.vazia(equipa));
    }

    @PostMapping("/equipas/{equipaId}/divida/blocos")
    @Transactional
    public ResponseEntity<BlocoDividaDto> registarBloco(@AuthenticationPrincipal GestorAutenticado autenticado,
                                                        @PathVariable UUID ligaId,
                                                        @PathVariable UUID equipaId,
                                                        @RequestBody RegistarBlocoRequest pedido) {
        if (pedido.valor() == null) {
            throw new IllegalArgumentException("O valor do bloco é obrigatório.");
        }
        Equipa equipa = equipa(autenticado, ligaId, equipaId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BlocoDividaDto.de(dividaService.registarBloco(equipa, pedido.valor())));
    }

    @PostMapping("/equipas/{equipaId}/divida/blocos/{blocoId}/pagar")
    @Transactional
    public DividaDto resolverBloco(@AuthenticationPrincipal GestorAutenticado autenticado,
                                   @PathVariable UUID ligaId,
                                   @PathVariable UUID equipaId,
                                   @PathVariable UUID blocoId) {
        Equipa equipa = equipa(autenticado, ligaId, equipaId);
        return comTotal(dividaService.resolverBloco(equipa, blocoId));
    }

    @PostMapping("/equipas/{equipaId}/divida/pagar")
    @Transactional
    public DividaDto resolverDivida(@AuthenticationPrincipal GestorAutenticado autenticado,
                                    @PathVariable UUID ligaId,
                                    @PathVariable UUID equipaId) {
        Equipa equipa = equipa(autenticado, ligaId, equipaId);
        return comTotal(dividaService.resolverDivida(equipa));
    }

    private DividaDto comTotal(Divida divida) {
        return DividaDto.de(divida, dividaService.calcularTotalDivida(divida));
    }

    private Liga liga(GestorAutenticado autenticado, UUID ligaId) {
        return ligaRepository.buscarPorIdEGestor(ligaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));
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
