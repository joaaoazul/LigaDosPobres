package com.ligarecord.web;

import com.ligarecord.domain.ClassificacaoGeral;
import com.ligarecord.domain.CobrancaPeriodo;
import com.ligarecord.domain.Liga;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.ClassificacaoService;
import com.ligarecord.service.CobrancaPeriodoService;
import com.ligarecord.service.RegraDividaService;
import com.ligarecord.web.dto.CobrancaPeriodoDto;
import com.ligarecord.web.dto.ResolverDesempateGeralRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * As cobranças presas a uma jornada — o "Inverno" e o "Verão". Definem-se com a
 * regra de dívida; aqui vê-se em que pé estão e desfaz-se o empate quando ele
 * trava uma delas.
 */
@RestController
@RequestMapping("/api/ligas/{ligaId}/cobrancas")
public class CobrancaPeriodoController {

    private final RegraDividaService regraDividaService;
    private final CobrancaPeriodoService cobrancaService;
    private final ClassificacaoService classificacaoService;
    private final LigaRepository ligaRepository;

    public CobrancaPeriodoController(RegraDividaService regraDividaService,
                                     CobrancaPeriodoService cobrancaService,
                                     ClassificacaoService classificacaoService,
                                     LigaRepository ligaRepository) {
        this.regraDividaService = regraDividaService;
        this.cobrancaService = cobrancaService;
        this.classificacaoService = classificacaoService;
        this.ligaRepository = ligaRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CobrancaPeriodoDto> listar(@AuthenticationPrincipal GestorAutenticado autenticado,
                                           @PathVariable UUID ligaId) {
        Liga liga = liga(autenticado, ligaId);
        List<ClassificacaoGeral> classificacao = classificacaoService.calcularClassificacao(liga);

        return cobrancas(liga).stream()
                .map(cobranca -> CobrancaPeriodoDto.de(cobranca,
                        cobranca.estaCobrada()
                                ? List.of()
                                : cobrancaService.empatesPorDesfazer(cobranca, classificacao)))
                .toList();
    }

    /**
     * Desfaz o empate da classificação geral com a ordem que o gestor deu e
     * cobra a seguir — os dois passos no mesmo pedido, porque separá-los
     * deixava uma ordem guardada à espera de servir para alguma coisa.
     */
    @PostMapping("/{cobrancaId}/desempate")
    @Transactional
    public CobrancaPeriodoDto resolverDesempate(@AuthenticationPrincipal GestorAutenticado autenticado,
                                                @PathVariable UUID ligaId,
                                                @PathVariable UUID cobrancaId,
                                                @RequestBody ResolverDesempateGeralRequest pedido) {
        Liga liga = liga(autenticado, ligaId);
        CobrancaPeriodo cobranca = cobrancas(liga).stream()
                .filter(candidata -> candidata.getId().equals(cobrancaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobrança não encontrada."));

        cobrancaService.resolverDesempateECobrar(cobranca, liga, pedido.ordem());
        return CobrancaPeriodoDto.de(cobranca, List.of());
    }

    private List<CobrancaPeriodo> cobrancas(Liga liga) {
        return regraDividaService.buscarPorLiga(liga)
                .map(regra -> regra.getCobrancas())
                .orElse(List.of());
    }

    private Liga liga(GestorAutenticado autenticado, UUID ligaId) {
        return ligaRepository.buscarPorIdEGestor(ligaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));
    }
}
