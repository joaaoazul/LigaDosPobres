package com.ligarecord.service;

import com.ligarecord.repository.TentativaLoginRepository;
import com.ligarecord.repository.TentativaLoginRepositoryImpl;
import com.ligarecord.web.LoginBloqueadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimiteDeLoginServiceTest {

    private TentativaLoginRepository tentativaRepository;
    private LimiteDeLoginService servico;

    @BeforeEach
    void setUp() {
        tentativaRepository = new TentativaLoginRepositoryImpl();
        servico = new LimiteDeLoginService(tentativaRepository);
    }

    @Test
    void naoBloqueiaUmEmailSemTentativasFalhadas() {
        assertDoesNotThrow(() -> servico.garantirNaoBloqueado("joao@exemplo.pt"));
    }

    @Test
    void bloqueiaDepoisDeEsgotarAsTentativasNaJanela() {
        for (int i = 0; i < LimiteDeLoginService.MAXIMO_POR_JANELA; i++) {
            servico.registarFalha("joao@exemplo.pt");
        }

        assertThrows(LoginBloqueadoException.class,
                () -> servico.garantirNaoBloqueado("joao@exemplo.pt"));
    }

    @Test
    void naoBloqueiaAindaUmaTentativaAbaixoDoLimite() {
        for (int i = 0; i < LimiteDeLoginService.MAXIMO_POR_JANELA - 1; i++) {
            servico.registarFalha("joao@exemplo.pt");
        }

        assertDoesNotThrow(() -> servico.garantirNaoBloqueado("joao@exemplo.pt"));
    }

    /** O email normalizado é o mesmo, por isso conta como o mesmo email. */
    @Test
    void contaAsTentativasIgnorandoMaiusculasEEspacos() {
        for (int i = 0; i < LimiteDeLoginService.MAXIMO_POR_JANELA; i++) {
            servico.registarFalha("  Joao@Exemplo.PT  ");
        }

        assertThrows(LoginBloqueadoException.class,
                () -> servico.garantirNaoBloqueado("joao@exemplo.pt"));
    }

    /**
     * Um email sem conta nenhuma esgota as tentativas tão depressa como um que
     * existe — senão o próprio bloqueio dizia quais os emails com conta.
     */
    @Test
    void bloqueiaTambemUmEmailQueNuncaTeveConta() {
        for (int i = 0; i < LimiteDeLoginService.MAXIMO_POR_JANELA; i++) {
            servico.registarFalha("ninguem@exemplo.pt");
        }

        assertThrows(LoginBloqueadoException.class,
                () -> servico.garantirNaoBloqueado("ninguem@exemplo.pt"));
    }

    @Test
    void naoBloqueiaUmEmailDiferenteDoQueFalhou() {
        for (int i = 0; i < LimiteDeLoginService.MAXIMO_POR_JANELA; i++) {
            servico.registarFalha("joao@exemplo.pt");
        }

        assertDoesNotThrow(() -> servico.garantirNaoBloqueado("outro@exemplo.pt"));
    }

    /**
     * Sem isto, um email gigante enchia a coluna (varchar(180), sem limite do
     * lado de cá) para sempre — é a única tabela de credenciais que nunca é
     * limpa, e ninguém precisa de sessão nenhuma para lhe bater.
     */
    @Test
    void cortaUmEmailGiganteAntesDeContarOuGravar() {
        String prefixoComum = "a".repeat(RegrasDeConta.MAXIMO_EMAIL);
        String primeiro = prefixoComum + "-um-final-qualquer@exemplo.pt";
        String segundo = prefixoComum + "-outro-final-completamente-diferente@exemplo.pt";

        for (int i = 0; i < LimiteDeLoginService.MAXIMO_POR_JANELA; i++) {
            servico.registarFalha(primeiro);
        }

        // Cortado ao mesmo limite, os dois ficam iguais para o limite de login,
        // mesmo divergindo depois do corte — prova que o corte acontece antes
        // de contar, não só que uma string enorme não rebenta nada.
        assertThrows(LoginBloqueadoException.class, () -> servico.garantirNaoBloqueado(segundo));
    }

}
