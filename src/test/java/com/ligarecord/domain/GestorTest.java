package com.ligarecord.domain;

import com.ligarecord.domain.enums.PapelGestor;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GestorTest {

    private Gestor novoGestor() {
        return new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor", PapelGestor.GESTOR);
    }

    @Test
    void semLicencaAutorizadaContaOsDiasDeTrialDesdeOCriadoEm() {
        Gestor gestor = novoGestor();

        assertTrue(gestor.emTrial());
        assertTrue(gestor.licencaAtiva());
        // >= DIAS_TRIAL - 1 e não == DIAS_TRIAL: entre construir o gestor e
        // chegar aqui passam alguns milissegundos, e Instant.until trunca
        // para o dia completo anterior quando falta menos de um dia inteiro.
        assertTrue(gestor.diasLicencaRestantes() >= Gestor.DIAS_TRIAL - 1);
        assertEquals(gestor.getCriadoEm().plus(Gestor.DIAS_TRIAL, ChronoUnit.DAYS), gestor.licencaExpiraEm());
    }

    @Test
    void autorizarLicencaPorDiasPositivosEstendeOAcesso() {
        Gestor gestor = novoGestor();

        gestor.autorizarLicencaPor(365);

        assertFalse(gestor.emTrial());
        assertTrue(gestor.licencaAtiva());
        assertTrue(gestor.diasLicencaRestantes() >= 364);
    }

    @Test
    void autorizarLicencaPorZeroDiasRevogaDeImediato() {
        Gestor gestor = novoGestor();
        gestor.autorizarLicencaPor(365);

        gestor.autorizarLicencaPor(0);

        assertFalse(gestor.emTrial());
        assertFalse(gestor.licencaAtiva());
        assertEquals(0, gestor.diasLicencaRestantes());
    }

    @Test
    void diasLicencaRestantesNuncaFicaNegativoQuandoJaExpirouHaMuito() {
        Gestor gestor = novoGestor();

        gestor.autorizarLicencaPor(-100);

        assertFalse(gestor.licencaAtiva());
        assertEquals(0, gestor.diasLicencaRestantes());
    }

    @Test
    void licencaNuncaBloqueiaUmAdmin() {
        Gestor admin = new Gestor(UUID.randomUUID(), "admin@teste.pt", "hash", "Admin", PapelGestor.ADMIN);

        admin.autorizarLicencaPor(-100);

        assertTrue(admin.licencaAtiva());
    }

    @Test
    void licencaNuncaBloqueiaQuemNaoPodeCriarLigas() {
        Gestor treinador = novoGestor();
        treinador.setPodeCriarLigas(false);

        treinador.autorizarLicencaPor(-100);

        assertTrue(treinador.licencaAtiva());
    }
}
