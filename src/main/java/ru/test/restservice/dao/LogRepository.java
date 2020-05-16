package ru.test.restservice.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.test.restservice.entity.LogEntry;

public interface LogRepository extends JpaRepository<LogEntry, String> {
}
