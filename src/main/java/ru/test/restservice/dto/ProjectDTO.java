package ru.test.restservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.test.restservice.entity.Project;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс для передачи информации о проекте и конструктор на основе сущности из БД
 */
@Data
@AllArgsConstructor
public class ProjectDTO {
    UUID id;
    String name;
    List<String> owners;
    boolean busy;
    boolean creator;
    UUID creatorId;
    String creatorName;

    public ProjectDTO(Project project, boolean busy, boolean creator, UUID creatorId, String creatorName) {
        this.id = project.id;
        this.name = project.name;
        this.owners = project.owners
                .stream()
                .map(o -> o.email)
                .collect(Collectors.toList());
        this.busy = busy;
        this.creator = creator;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
    }
}
