package com.ligarecord.repository;

import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;

import java.util.Optional;

public interface RegraDividaRepository {

    RegraDivida guardar(RegraDivida regra);

    Optional<RegraDivida> buscarPorLiga(Liga liga);
}
