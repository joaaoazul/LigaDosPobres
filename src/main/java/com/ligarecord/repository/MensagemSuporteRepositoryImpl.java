package com.ligarecord.repository;

import com.ligarecord.domain.MensagemSuporte;

import java.time.Instant;
import java.util.Objects;

public class MensagemSuporteRepositoryImpl extends RepositorioEmMemoria<MensagemSuporte>
        implements MensagemSuporteRepository {

    @Override
    public MensagemSuporte guardar(MensagemSuporte mensagem) {
        return super.guardar(mensagem);
    }

    @Override
    public long contarDoIpDesde(String ip, Instant desde) {
        return entidades.stream()
                .filter(mensagem -> Objects.equals(mensagem.getIp(), ip))
                .filter(mensagem -> mensagem.getCriadoEm().isAfter(desde))
                .count();
    }
}
