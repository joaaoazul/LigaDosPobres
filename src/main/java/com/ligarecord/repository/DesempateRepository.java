package com.ligarecord.repository;

import com.ligarecord.domain.Desempate;
import com.ligarecord.domain.Jornada;
import java.util.Optional;

public interface DesempateRepository {
    Desempate guardar(Desempate desempate);
    Optional<Desempate> buscarPorJornada(Jornada jornada);
}
