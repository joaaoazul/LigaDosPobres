package com.ligarecord.service;

import com.ligarecord.domain.ClassificacaoGeral;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ClassificacaoServiceTest {

    @Test
    public void equipaDesistenteDeveFicarEmUltimaPosicao(){
        ClassificacaoService classificacaoService = new ClassificacaoService();
        Liga ligaTeste = new Liga(UUID.randomUUID(), "Teste", 20, EstadoLiga.ATIVA);
        Treinador joao = new Treinador(UUID.randomUUID(), "João Azul");
        Treinador rica = new Treinador(UUID.randomUUID(), "Ricardo Gomes");
Treinador david = new Treinador(UUID.randomUUID(), "David Livramento");


        Equipa bairroCaixa = new Equipa(UUID.randomUUID(), "Bairro da Caixa FC", joao, ligaTeste, EstadoEquipa.ATIVA);
        Equipa rotundaBarco = new Equipa(UUID.randomUUID(), "Rotunda do Barco FC", rica, ligaTeste, EstadoEquipa.ATIVA);
Equipa real = new Equipa(UUID.randomUUID(), "RealDesistente", david, ligaTeste, EstadoEquipa.DESISTENTE);


        ligaTeste.adicionarEquipa(bairroCaixa);
        ligaTeste.adicionarEquipa(rotundaBarco);
        ligaTeste.adicionarEquipa(real);

        List<ClassificacaoGeral> resultado = classificacaoService.calcularClassificacao(ligaTeste);

        resultado.get(resultado.size() - 1);

        assertEquals(real, resultado.get(resultado.size() - 1).getEquipa());

    }

}
