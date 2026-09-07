package com.ligarecord.domain;

import com.ligarecord.domain.enums.EscalaDivida;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Como uma liga cobra as suas equipas: uma inscrição única na entrada, e
 * depois um valor por período de jornadas que sobe com a posição na
 * classificação — quem vai melhor paga menos. Cada liga tem a sua própria
 * regra, porque os valores variam de liga para liga.
 *
 * <p>De onde sai esse valor é a {@link EscalaDivida}:
 *
 * <ul>
 *   <li><b>Fórmula</b> — {@code valorInicial + incremento × escalão}, travada
 *       no {@code valorMaximo}. Uma rampa de degraus todos do mesmo tamanho.
 *   <li><b>Tabela</b> — uma linha por posição, escrita à mão. Existe porque há
 *       ligas cuja tabela não é uma rampa regular (sobe 0,20€ por lugar até ao
 *       7º e 0,10€ daí em diante, por exemplo), e obrigá-las a uma fórmula era
 *       torcer a liga para caber no programa.
 * </ul>
 *
 * <p>Uma liga sem regra definida não tem cobrança automática nenhuma: o
 * gestor continua a fechar blocos e a cobrar a inscrição à mão.
 */
@Entity
@Table(name = "regra_divida")
public class RegraDivida extends EntidadeBase {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liga_id", nullable = false, unique = true)
    private Liga liga;

    @Column(name = "valor_inscricao", nullable = false)
    private BigDecimal valorInscricao;

    @Column(name = "valor_inicial", nullable = false)
    private BigDecimal valorInicial;

    @Column(nullable = false)
    private BigDecimal incremento;

    @Column(name = "equipas_por_escalao", nullable = false)
    private int equipasPorEscalao;

    @Column(name = "valor_maximo", nullable = false)
    private BigDecimal valorMaximo;

    @Column(name = "jornadas_por_bloco", nullable = false)
    private int jornadasPorBloco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalaDivida escala = EscalaDivida.FORMULA;

    /**
     * Há ligas em que as jornadas de treino não são cobradas. Nas que já
     * existiam são — o manual diz "para efeitos de dinheiro e de classificação,
     * as duas contam igual" — e é por isso que o valor por omissão é este.
     */
    @Column(name = "cobra_treino", nullable = false)
    private boolean cobraTreino = true;

    @OneToMany(mappedBy = "regra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicao")
    private List<EscalaValor> tabela = new ArrayList<>();

    /** As cobranças presas a uma jornada — o "Inverno" e o "Verão" de algumas ligas. */
    @OneToMany(mappedBy = "regra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("jornadaOficial")
    private List<CobrancaPeriodo> cobrancas = new ArrayList<>();

    protected RegraDivida() {
        // exigido pelo Hibernate
    }

    public RegraDivida(UUID id, Liga liga, BigDecimal valorInscricao, BigDecimal valorInicial,
                       BigDecimal incremento, int equipasPorEscalao, BigDecimal valorMaximo,
                       int jornadasPorBloco) {
        this.id = id;
        this.liga = liga;
        this.valorInscricao = valorInscricao;
        this.valorInicial = valorInicial;
        this.incremento = incremento;
        this.equipasPorEscalao = equipasPorEscalao;
        this.valorMaximo = valorMaximo;
        this.jornadasPorBloco = jornadasPorBloco;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Liga getLiga() {
        return liga;
    }

    public BigDecimal getValorInscricao() {
        return valorInscricao;
    }

    public void setValorInscricao(BigDecimal valorInscricao) {
        this.valorInscricao = valorInscricao;
    }

    public BigDecimal getValorInicial() {
        return valorInicial;
    }

    public void setValorInicial(BigDecimal valorInicial) {
        this.valorInicial = valorInicial;
    }

    public BigDecimal getIncremento() {
        return incremento;
    }

    public void setIncremento(BigDecimal incremento) {
        this.incremento = incremento;
    }

    public int getEquipasPorEscalao() {
        return equipasPorEscalao;
    }

    public void setEquipasPorEscalao(int equipasPorEscalao) {
        this.equipasPorEscalao = equipasPorEscalao;
    }

    public BigDecimal getValorMaximo() {
        return valorMaximo;
    }

    public void setValorMaximo(BigDecimal valorMaximo) {
        this.valorMaximo = valorMaximo;
    }

    public int getJornadasPorBloco() {
        return jornadasPorBloco;
    }

    public void setJornadasPorBloco(int jornadasPorBloco) {
        this.jornadasPorBloco = jornadasPorBloco;
    }

    public EscalaDivida getEscala() {
        return escala;
    }

    public void setEscala(EscalaDivida escala) {
        this.escala = escala;
    }

    public boolean isCobraTreino() {
        return cobraTreino;
    }

    public void setCobraTreino(boolean cobraTreino) {
        this.cobraTreino = cobraTreino;
    }

    public List<EscalaValor> getTabela() {
        return tabela;
    }

    public List<CobrancaPeriodo> getCobrancas() {
        return cobrancas;
    }

    /**
     * Acerta a lista de cobranças pela que o gestor guardou, casando pelo nome.
     *
     * <p>Casar pelo nome e não substituir tudo é o que impede uma cobrança já
     * feita de voltar a nascer por cobrar — e de ser cobrada duas vezes. Uma
     * cobrança já feita não muda de jornada nem de tabela: o dinheiro já está
     * lançado, e mudá-la em silêncio deixava a dívida a dizer uma coisa e a
     * regra outra.
     */
    public void acertarCobrancas(List<CobrancaPedida> pedidas) {
        for (CobrancaPeriodo existente : new ArrayList<>(cobrancas)) {
            boolean continuaPedida = pedidas.stream()
                    .anyMatch(pedida -> pedida.nome().equalsIgnoreCase(existente.getNome()));
            if (!continuaPedida) {
                if (existente.estaCobrada()) {
                    throw new IllegalStateException(
                            "A cobrança \"" + existente.getNome() + "\" já foi feita e não pode ser removida.");
                }
                cobrancas.remove(existente);
            }
        }

        for (CobrancaPedida pedida : pedidas) {
            CobrancaPeriodo cobranca = cobrancas.stream()
                    .filter(existente -> existente.getNome().equalsIgnoreCase(pedida.nome()))
                    .findFirst()
                    .orElse(null);

            if (cobranca == null) {
                cobranca = new CobrancaPeriodo(UUID.randomUUID(), this, pedida.nome(), pedida.jornadaOficial());
                cobranca.substituirTabela(pedida.tabela());
                cobrancas.add(cobranca);
                continue;
            }

            if (cobranca.estaCobrada()) {
                if (cobranca.getJornadaOficial() != pedida.jornadaOficial()) {
                    throw new IllegalStateException(
                            "A cobrança \"" + cobranca.getNome() + "\" já foi feita; a jornada não pode mudar.");
                }
                continue;
            }

            cobranca.setJornadaOficial(pedida.jornadaOficial());
            cobranca.substituirTabela(pedida.tabela());
        }
    }

    /** O que o gestor pediu para uma cobrança, antes de haver entidade nenhuma. */
    public record CobrancaPedida(String nome, int jornadaOficial, List<BigDecimal> tabela) {
    }

    /**
     * Substitui a tabela inteira: o gestor cola a lista que tem no papel e é
     * essa que passa a valer, do 1º ao último.
     *
     * <p><b>Reaproveita as linhas que já existem em vez de as apagar e voltar
     * a criar.</b> Apagar e recriar parece mais simples e não funciona: no
     * mesmo commit o Hibernate grava as inserções antes das remoções, e as
     * linhas novas esbarram na unicidade {@code (regra_id, posicao)} das
     * antigas — guardar a mesma regra duas vezes dava um conflito. É a mesma
     * armadilha que o {@code GlobalExceptionHandler} descreve para os blocos
     * da dívida.
     */
    public void substituirTabela(List<BigDecimal> valoresPorPosicao) {
        for (int i = 0; i < valoresPorPosicao.size(); i++) {
            if (i < tabela.size()) {
                tabela.get(i).definirValor(valoresPorPosicao.get(i));
            } else {
                tabela.add(new EscalaValor(UUID.randomUUID(), this, i + 1, valoresPorPosicao.get(i)));
            }
        }
        // A tabela nova pode ser mais curta do que a anterior; o que sobra pelo
        // fundo sai (as posições são sempre contíguas a partir do 1).
        while (tabela.size() > valoresPorPosicao.size()) {
            tabela.remove(tabela.size() - 1);
        }
    }

    /**
     * O valor desta posição segundo a escala em vigor.
     *
     * <p>Abaixo do fim da tabela repete-se a última linha: uma liga pode
     * receber uma equipa a mais do que as linhas que o gestor escreveu, e a
     * alternativa — não cobrar nada a quem ficar para lá do fim — premiava
     * exactamente quem ficou em último. É também o que a fórmula já faz, com o
     * valor máximo.
     */
    public BigDecimal valorDaPosicao(int posicao) {
        if (escala == EscalaDivida.TABELA) {
            if (tabela.isEmpty()) {
                throw new IllegalStateException("A regra usa uma tabela, mas a tabela está vazia.");
            }
            return tabela.stream()
                    .filter(linha -> linha.getPosicao() == posicao)
                    .map(EscalaValor::getValor)
                    .findFirst()
                    .orElseGet(() -> tabela.stream()
                            .max(Comparator.comparingInt(EscalaValor::getPosicao))
                            .orElseThrow()
                            .getValor());
        }

        int escalao = (posicao - 1) / equipasPorEscalao;
        return valorInicial.add(incremento.multiply(BigDecimal.valueOf(escalao))).min(valorMaximo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegraDivida outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
