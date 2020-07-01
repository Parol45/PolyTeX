package ru.test.restservice.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.test.restservice.entity.LogEntry;

import java.util.UUID;

public interface LogRepository extends JpaRepository<LogEntry, UUID> {
}
