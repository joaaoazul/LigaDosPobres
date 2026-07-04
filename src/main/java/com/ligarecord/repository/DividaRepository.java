package com.ligarecord.repository;

import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoDivida;

import java.util.List;
import java.util.Optional;

public interface DividaRepository {

    Divida guardarDivida(Divida divida);
    Optional<Divida> buscarPorEquipa(Equipa equipa);
    List<Divida> listarDividas(Liga liga, EstadoDivida estadoDivida);
}
