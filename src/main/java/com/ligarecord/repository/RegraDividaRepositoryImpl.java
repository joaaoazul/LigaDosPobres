package com.ligarecord.repository;

import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;

import java.util.Optional;

public class RegraDividaRepositoryImpl extends RepositorioEmMemoria<RegraDivida> implements RegraDividaRepository {

    @Override
    public RegraDivida guardar(RegraDivida regra) {
        return super.guardar(regra);
    }

    @Override
    public Optional<RegraDivida> buscarPorLiga(Liga liga) {
        return entidades.stream().filter(regra -> regra.getLiga().equals(liga)).findFirst();
    }
}
