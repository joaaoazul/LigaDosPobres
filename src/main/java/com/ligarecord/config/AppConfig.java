package com.ligarecord.config;

import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.EquipaRepositoryImpl;
import com.ligarecord.repository.JornadaRepository;
import com.ligarecord.repository.JornadaRepositoryImpl;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.repository.LigaRepositoryImpl;
import com.ligarecord.service.ClassificacaoService;
import com.ligarecord.service.JornadaService;
import com.ligarecord.service.LigaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga os repositórios em memória aos serviços de domínio. Como não há base de
 * dados, os dados vivem enquanto a aplicação estiver a correr.
 */
@Configuration
public class AppConfig {

    @Bean
    public LigaRepository ligaRepository() {
        return new LigaRepositoryImpl();
    }

    @Bean
    public EquipaRepository equipaRepository() {
        return new EquipaRepositoryImpl();
    }

    @Bean
    public JornadaRepository jornadaRepository() {
        return new JornadaRepositoryImpl();
    }

    @Bean
    public LigaService ligaService(LigaRepository ligaRepository, EquipaRepository equipaRepository) {
        return new LigaService(ligaRepository, equipaRepository);
    }

    @Bean
    public JornadaService jornadaService(JornadaRepository jornadaRepository) {
        return new JornadaService(jornadaRepository);
    }

    @Bean
    public ClassificacaoService classificacaoService() {
        return new ClassificacaoService();
    }
}
