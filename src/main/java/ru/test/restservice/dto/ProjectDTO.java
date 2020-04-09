package ru.test.restservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.test.restservice.entity.Project;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class ProjectDTO {
    UUID id;
    String name;
    List<String> owners;

    public ProjectDTO(Project project) {
        this.id = project.id;
        this.name = project.name;
        this.owners = project.owners
                .stream()
                .map(o -> o.email)
                .collect(Collectors.toList());
    }
}
