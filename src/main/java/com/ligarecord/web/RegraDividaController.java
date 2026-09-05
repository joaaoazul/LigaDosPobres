package com.ligarecord.web;

import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.RegraDividaService;
import com.ligarecord.web.dto.DefinirRegraDividaRequest;
import com.ligarecord.web.dto.RegraDividaDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Como uma liga cobra as suas equipas. Sem regra definida, a liga não tem
 * cobrança automática nenhuma — ver {@link com.ligarecord.service.DividaService}.
 */
@RestController
@RequestMapping("/api/ligas/{ligaId}/regra-divida")
public class RegraDividaController {

    private final RegraDividaService regraDividaService;
    private final LigaRepository ligaRepository;

    public RegraDividaController(RegraDividaService regraDividaService, LigaRepository ligaRepository) {
        this.regraDividaService = regraDividaService;
        this.ligaRepository = ligaRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public RegraDividaDto obter(@AuthenticationPrincipal GestorAutenticado autenticado,
                                @PathVariable UUID ligaId) {
        Liga liga = liga(autenticado, ligaId);
        return regraDividaService.buscarPorLiga(liga)
                .map(RegraDividaDto::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esta liga ainda não tem regra de dívida."));
    }

    @PutMapping
    @Transactional
    public RegraDividaDto definir(@AuthenticationPrincipal GestorAutenticado autenticado,
                                  @PathVariable UUID ligaId,
                                  @RequestBody DefinirRegraDividaRequest pedido) {
        if (pedido.valorInscricao() == null || pedido.valorInicial() == null || pedido.incremento() == null
                || pedido.equipasPorEscalao() == null || pedido.valorMaximo() == null
                || pedido.jornadasPorBloco() == null) {
            throw new IllegalArgumentException("Todos os campos da regra são obrigatórios.");
        }

        Liga liga = liga(autenticado, ligaId);
        RegraDivida regra = regraDividaService.definir(liga,
                pedido.valorInscricao(), pedido.valorInicial(), pedido.incremento(),
                pedido.equipasPorEscalao(), pedido.valorMaximo(), pedido.jornadasPorBloco());
        return RegraDividaDto.de(regra);
    }

    private Liga liga(GestorAutenticado autenticado, UUID ligaId) {
        return ligaRepository.buscarPorIdEGestor(ligaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));
    }
}
