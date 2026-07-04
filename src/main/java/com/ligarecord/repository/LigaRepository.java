package com.ligarecord.repository;

import com.ligarecord.domain.Liga;

import java.util.List;

public interface LigaRepository {

    Liga guardarLiga(Liga liga);
    List<Liga> listarLigas();

}
