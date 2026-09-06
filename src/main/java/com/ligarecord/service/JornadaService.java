package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.ResultadoJornada;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoJornada;
import com.ligarecord.domain.enums.EstadoJornadaTreino;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.JornadaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JornadaService {

    private static final int NUMERO_JORNADAS_TREINO = 5;

    private JornadaRepository jornadaRepository;
    private final RegraDividaService regraDividaService;
    private final DividaService dividaService;

    public JornadaService(JornadaRepository jornadaRepository,
                          RegraDividaService regraDividaService,
                          DividaService dividaService){
        this.jornadaRepository = jornadaRepository;
        this.regraDividaService = regraDividaService;
        this.dividaService = dividaService;
    }

    /**
     * Abre a próxima jornada da liga. As primeiras {@value #NUMERO_JORNADAS_TREINO}
     * jornadas são de treino, as seguintes são oficiais. Cada tipo tem a sua
     * própria numeração.
     */
    @Transactional
    public Jornada abrirJornada(Liga liga){
        if (liga == null){
            throw new IllegalArgumentException("Não existe uma liga disponível");
        }

        if (liga.getEstado() != EstadoLiga.ATIVA){
            throw new IllegalStateException("Não é possível abrir jornadas numa liga desativada.");
        }

        if (existeJornadaAberta(liga)){
            throw new IllegalStateException("Já existe uma jornada aberta nesta liga.");
        }

        int countTreino = 0;
        int countOficial = 0;
        for(int i = 0; i < liga.getJornadas().size(); i++){
            if(liga.getJornadas().get(i).iseTreino()){
                countTreino++;
            } else {
                countOficial++;
            }
        }

        int jornadaAtual;
        EstadoJornadaTreino tipoJornada;

        if(countTreino < NUMERO_JORNADAS_TREINO){
            tipoJornada = EstadoJornadaTreino.TREINO;
            jornadaAtual = countTreino + 1;
        } else {
            tipoJornada = EstadoJornadaTreino.OFICIAL;
            jornadaAtual = countOficial + 1;
        }

        Jornada jornada = new Jornada(
                UUID.randomUUID(),
                jornadaAtual,
                EstadoJornada.ABERTA,
                tipoJornada,
                liga
        );

        liga.adicionarJornada(jornada);
        jornadaRepository.guardar(jornada);

        return jornada;
    }

    @Transactional
    public ResultadoJornada inserirResultado(Jornada jornada, Equipa equipa, int pontuacao){
        if(jornada == null){
            throw new IllegalArgumentException("Não existe uma jornada válida.");
        }
        if(equipa == null){
            throw new IllegalArgumentException("A equipa é obrigatória.");
        }
        if(jornada.getEstadoJ() == EstadoJornada.FECHADA){
            throw new IllegalStateException("A jornada já está fechada.");
        }
        // Sem isto, mexer nas pontuações depois de o empate ter sido detetado
        // invalidava-o em silêncio: o empate deixava de existir (ou passavam a
        // ser outras equipas) e o desempate submetido a seguir já não batia
        // certo com nada.
        if(jornada.getEstadoJ() == EstadoJornada.DESEMPATE){
            throw new IllegalStateException(
                    "Esta jornada está à espera de desempate. Resolve o empate antes de mexer nas pontuações.");
        }
        if(equipa.getEstado() != EstadoEquipa.ATIVA){
            throw new IllegalStateException("Não é possível inserir resultados de uma equipa desistente.");
        }
        if(pontuacao < 0){
            throw new IllegalArgumentException("A pontuação não pode ser negativa.");
        }

        for(ResultadoJornada existente : jornada.getResultadoJ()){
            if(existente.getEquipa().getId().equals(equipa.getId())){
                existente.setPontuacao(pontuacao);
                jornadaRepository.guardar(jornada);
                return existente;
            }
        }

        ResultadoJornada resultado = new ResultadoJornada(
                UUID.randomUUID(),
                equipa,
                jornada,
                pontuacao,
                0,
                false
        );

        jornada.adicionarResultado(resultado);
        jornadaRepository.guardar(jornada);

        return resultado;
    }

    /**
     * Fecha a jornada e atribui as posições por ordem decrescente de pontuação.
     *
     * <p>Se sobrarem equipas empatadas, a jornada NÃO fecha: fica em
     * {@link EstadoJornada#DESEMPATE} à espera de {@link #resolverDesempate},
     * e nenhum bloco de dívida é cobrado. É de propósito — o valor de cada
     * bloco sai da posição de cada equipa na jornada, por isso cobrar antes de
     * o empate estar desfeito era cobrar a mais a umas e a menos a outras, e
     * já com o dinheiro registado não havia forma limpa de corrigir.
     */
    @Transactional
    public Jornada fecharJornada(Jornada jornada){
        if(jornada == null){
            throw new IllegalArgumentException("Não existe uma jornada válida.");
        }
        if(jornada.getEstadoJ() == EstadoJornada.FECHADA){
            throw new IllegalStateException("Esta jornada já se encontra fechada.");
        }
        if(jornada.getEstadoJ() == EstadoJornada.DESEMPATE){
            throw new IllegalStateException(
                    "Esta jornada está à espera de desempate. Resolve o empate para a fechar.");
        }
        if(jornada.getResultadoJ().isEmpty()){
            throw new IllegalStateException("Não é possível fechar uma jornada sem resultados.");
        }

        atribuirPosicoesPorPontuacao(jornada);

        if(!resultadosEmpatados(jornada).isEmpty()){
            jornada.setEstadoJ(EstadoJornada.DESEMPATE);
            jornadaRepository.guardar(jornada);
            return jornada;
        }

        jornada.setEstadoJ(EstadoJornada.FECHADA);
        jornadaRepository.guardar(jornada);

        fecharBlocoSeForACaso(jornada);

        return jornada;
    }

    /**
     * Desfaz o empate de uma jornada com a ordem que o gestor deu, fecha-a, e
     * só então deixa o bloco ser cobrado.
     *
     * <p>{@code ordem} tem de conter exactamente as equipas empatadas. A
     * pontuação continua a mandar na ordenação; a ordem dada só decide entre
     * quem tem a mesma pontuação, por isso uma lista mal ordenada nunca
     * consegue trocar equipas entre pontuações diferentes.
     */
    @Transactional
    public Jornada resolverDesempate(Jornada jornada, List<UUID> ordem){
        if(jornada == null){
            throw new IllegalArgumentException("Não existe uma jornada válida.");
        }
        if(jornada.getEstadoJ() != EstadoJornada.DESEMPATE){
            throw new IllegalStateException("Esta jornada não está à espera de desempate.");
        }
        if(ordem == null || ordem.isEmpty()){
            throw new IllegalArgumentException("Indica a ordem das equipas empatadas.");
        }

        List<ResultadoJornada> empatados = resultadosEmpatados(jornada);
        Set<UUID> equipasEmpatadas = empatados.stream()
                .map(resultado -> resultado.getEquipa().getId())
                .collect(Collectors.toSet());
        Set<UUID> equipasIndicadas = new HashSet<>(ordem);

        if(equipasIndicadas.size() != ordem.size() || !equipasIndicadas.equals(equipasEmpatadas)){
            throw new IllegalArgumentException(
                    "A ordem tem de indicar, uma só vez, exactamente as equipas empatadas.");
        }

        Map<UUID, Integer> lugarNaOrdem = new HashMap<>();
        for(int i = 0; i < ordem.size(); i++){
            lugarNaOrdem.put(ordem.get(i), i);
        }

        List<ResultadoJornada> ordenados = new ArrayList<>(jornada.getResultadoJ());
        ordenados.sort(Comparator.comparingInt(ResultadoJornada::getPontuacao).reversed()
                .thenComparingInt(resultado -> lugarNaOrdem.getOrDefault(resultado.getEquipa().getId(), 0)));

        // Já não há posições partilhadas: depois do desempate cada equipa tem
        // a sua, e é essa que o fecho do bloco vai cobrar.
        for(int i = 0; i < ordenados.size(); i++){
            ordenados.get(i).setPosicao(i + 1);
        }
        empatados.forEach(resultado -> resultado.setDesempateManual(true));

        jornada.setEstadoJ(EstadoJornada.FECHADA);
        jornadaRepository.guardar(jornada);

        fecharBlocoSeForACaso(jornada);

        return jornada;
    }

    /** Posições por pontuação decrescente, com os empatados a partilhar a posição. */
    private void atribuirPosicoesPorPontuacao(Jornada jornada){
        List<ResultadoJornada> ordenados = new ArrayList<>(jornada.getResultadoJ());
        ordenados.sort(Comparator.comparingInt(ResultadoJornada::getPontuacao).reversed());

        int posicao = 0;
        Integer pontuacaoAnterior = null;
        for(int i = 0; i < ordenados.size(); i++){
            ResultadoJornada resultado = ordenados.get(i);
            if(pontuacaoAnterior == null || resultado.getPontuacao() != pontuacaoAnterior){
                posicao = i + 1;
                pontuacaoAnterior = resultado.getPontuacao();
            }
            resultado.setPosicao(posicao);
        }
    }

    /**
     * Os resultados que partilham pontuação com pelo menos outro. Inclui
     * equipas desistentes que tenham resultado nesta jornada: a posição delas
     * aparece na tabela como a de qualquer outra, mesmo não sendo cobrada.
     */
    private List<ResultadoJornada> resultadosEmpatados(Jornada jornada){
        return jornada.getResultadoJ().stream()
                .collect(Collectors.groupingBy(ResultadoJornada::getPontuacao))
                .values().stream()
                .filter(grupo -> grupo.size() > 1)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Sem regra de dívida definida para a liga, não há cobrança automática
     * nenhuma — o gestor continua a fechar blocos à mão.
     */
    private void fecharBlocoSeForACaso(Jornada jornada) {
        Liga liga = jornada.getLiga();
        regraDividaService.buscarPorLiga(liga).ifPresent(regra -> {
            List<Jornada> pendentes = jornadasPendentesDeBloco(liga);
            if (dividaService.prontoParaFecharBloco(pendentes, regra)) {
                dividaService.processarFechoBloco(regra, pendentes);
                pendentes.forEach(j -> {
                    j.setIncluidaEmBloco(true);
                    jornadaRepository.guardar(j);
                });
            }
        });
    }

    /**
     * As jornadas já fechadas da liga que ainda não entraram em nenhum
     * bloco. Basta o estado {@link Jornada#isIncluidaEmBloco()}: como cada
     * jornada fica marcada assim que o seu bloco fecha, isto não depende da
     * ordem de {@code liga.getJornadas()} nem se perde se o gestor mudar
     * {@code jornadasPorBloco} a meio da época.
     */
    private List<Jornada> jornadasPendentesDeBloco(Liga liga) {
        return liga.getJornadas().stream()
                .filter(j -> j.getEstadoJ() == EstadoJornada.FECHADA && !j.isIncluidaEmBloco())
                .toList();
    }

    public boolean verificaSeTreino(Jornada jornada){
        if(jornada == null){
            throw new IllegalArgumentException("Não existe uma jornada válida.");
        } else return jornada.iseTreino();
    }

    private boolean existeJornadaAberta(Liga liga){
        for(Jornada jornada : liga.getJornadas()){
            if(jornada.getEstadoJ() != EstadoJornada.FECHADA){
                return true;
            }
        }
        return false;
    }
}
