package com.ligarecord.service;

import com.ligarecord.domain.ClassificaçãoGeral;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.JornadaRepository;

public class ClassificaçãoService {

    private EquipaRepository equipaRepository;
    private JornadaRepository jornadaRepository;

    public ClassificaçãoService(EquipaRepository equipaRepository, JornadaRepository jornadaRepository){
        this.equipaRepository = equipaRepository;
        this.jornadaRepository = jornadaRepository;
    }



}
