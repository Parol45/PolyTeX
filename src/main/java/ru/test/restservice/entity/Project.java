package ru.test.restservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
/**
 * Сущность, хранимая в соответствующем репозитории
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor

// TODO: убрать path, потому что это "projects/" + id
public class Project {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    public String path;

    public UUID creatorId;

    @ManyToMany(fetch = FetchType.EAGER)
    public Set<User> owners = new HashSet<>();

}