package ru.test.restservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность, хранимая в соответствующем репозитории
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    @Id
    public UUID id;

    @Column(nullable = false)
    public UUID userId;

    public UUID projectId;

    @Column(nullable = false)
    public LocalDateTime dateTime;

    // TODO: ENUM?
    @Column(nullable = false)
    public String action;
}
