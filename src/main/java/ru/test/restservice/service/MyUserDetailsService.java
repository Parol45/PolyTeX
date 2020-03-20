package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ru.test.restservice.dao.UserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    String adminHash = new BCryptPasswordEncoder(4).encode("admin");

    private final UserRepository userRepository;

    @Override
    public User loadUserByUsername(String email) {
        return new User("admin", adminHash,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

}
