package com.ligarecord.domain;

import com.ligarecord.domain.enums.EstadoJornada;
import com.ligarecord.domain.enums.EstadoJornadaTreino;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JornadaTest {

    private Jornada jornada(int numero, EstadoJornadaTreino tipo) {
        return new Jornada(UUID.randomUUID(), numero, EstadoJornada.ABERTA, tipo, null);
    }

    /**
     * numJornada reinicia em 1 quando as jornadas de treino terminam e
     * começam as oficiais (ver o comentário em {@link Liga#getJornadas()}):
     * ordenar só por número intercala treino com oficial.
     */
    @Test
    void ordemCronologicaPoeTodoOTreinoAntesDeTodasAsOficiais() {
        Jornada oficial1 = jornada(1, EstadoJornadaTreino.OFICIAL);
        Jornada treino2 = jornada(2, EstadoJornadaTreino.TREINO);
        Jornada treino1 = jornada(1, EstadoJornadaTreino.TREINO);
        Jornada oficial2 = jornada(2, EstadoJornadaTreino.OFICIAL);

        List<Jornada> desordenadas = new ArrayList<>(List.of(oficial1, treino2, treino1, oficial2));
        desordenadas.sort(Jornada.ORDEM_CRONOLOGICA);

        assertEquals(List.of(treino1, treino2, oficial1, oficial2), desordenadas);
    }
}
