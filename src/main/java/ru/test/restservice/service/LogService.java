package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.test.restservice.dao.LogRepository;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.entity.LogEntry;
import ru.test.restservice.entity.User;
import ru.test.restservice.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final UserRepository userRepository;
    /**
     * Запись в БД действия пользователя
     */
    public void log(String username, String action) {
        log(username,null, action);
    }

    public void log(String username, UUID project_id, String action) {
        User user = userRepository.findByEmail(username).orElseThrow(NotFoundException::new);
        UUID id = UUID.randomUUID();
        logRepository.save(new LogEntry(id, user.getId(), project_id, LocalDateTime.now(), action));
    }

}
