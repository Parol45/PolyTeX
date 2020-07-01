package ru.test.restservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;
/**
 * Сущность, хранимая в соответствующем репозитории
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    public String description;
}
