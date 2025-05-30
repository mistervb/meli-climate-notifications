package com.mercadolibre.itarc.climatehub_ms_user.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mercadolibre.itarc.climatehub_ms_user.repository.UserRepository;
import com.mercadolibre.itarc.climatehub_ms_user.model.entity.UserEntity;

/**
 * Implementação do UserDetailsService para autenticação com Spring Security.
 * Esta classe é responsável por carregar os detalhes do usuário a partir do email.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carrega um usuário pelo email e o converte em UserDetails.
     *
     * @param email o email do usuário a ser carregado
     * @return UserDetails contendo as informações do usuário
     * @throws UsernameNotFoundException se o usuário não for encontrado
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                String.format("Usuário não encontrado com o email: %s", email)
            ));

        return new UserDetailsImpl(user);
    }
} 