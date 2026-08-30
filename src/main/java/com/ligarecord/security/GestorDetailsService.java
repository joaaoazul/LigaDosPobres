package com.ligarecord.security;

import com.ligarecord.repository.GestorRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestorDetailsService implements UserDetailsService {

    private final GestorRepository gestorRepository;

    public GestorDetailsService(GestorRepository gestorRepository) {
        this.gestorRepository = gestorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return gestorRepository.buscarPorEmail(email.trim().toLowerCase())
                .map(GestorAutenticado::new)
                // mensagem genérica: não revela se o email existe
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }
}
