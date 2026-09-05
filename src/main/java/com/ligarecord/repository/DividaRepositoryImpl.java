package com.ligarecord.repository;

import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoDivida;

import java.util.List;
import java.util.Optional;

public class DividaRepositoryImpl extends RepositorioEmMemoria<Divida> implements DividaRepository {

    @Override
    public Divida guardarDivida(Divida divida) {
        return super.guardar(divida);
    }

    @Override
    public Optional<Divida> buscarPorEquipa(Equipa equipa) {
        return entidades.stream().filter(divida -> divida.getEquipa().equals(equipa)).findFirst();
    }

    @Override
    public List<Divida> listarDividas(Liga liga, EstadoDivida estadoDivida) {
        return entidades.stream()
                .filter(divida -> divida.getEquipa().getLiga().equals(liga) && divida.getEstado() == estadoDivida)
                .toList();
    }

    @Override
    public List<Divida> buscarPorTreinador(Gestor conta) {
        return entidades.stream()
                .filter(divida -> conta.equals(divida.getEquipa().getTreinador().getConta()))
                .toList();
    }
}
