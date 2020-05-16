package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.test.restservice.dao.UserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class ClientDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LogService logService;

    @Override
    public User loadUserByUsername(String email) {
        ru.test.restservice.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("email: " + email));
        logService.log(email, String.format("%s logged in", user.email));
        return new User(user.email, user.password, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
    }

}
