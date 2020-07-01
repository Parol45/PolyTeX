package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.exceptions.GenericException;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class ClientDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LogService logService;
    /**
     * Метод, описывающий логику получения spring-объекта User из БД
     *
     * @param email имя пользователя
     */
    @Override
    public User loadUserByUsername(String email) {
        ru.test.restservice.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("email: " + email));
        if (user.banned) {
            throw new GenericException("You're banned");
        }
        logService.log(email, String.format("%s logged in", user.email));
        return new User(user.email, user.password, Collections.singleton(new SimpleGrantedAuthority(user.role)));
    }
}
