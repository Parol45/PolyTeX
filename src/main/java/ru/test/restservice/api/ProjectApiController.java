package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.test.restservice.dto.ProjectDTO;
import ru.test.restservice.service.ProjectService;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectApiController {

    private final ProjectService projectService;

    @PostMapping("/projects")
    public ProjectDTO createNewProject(@RequestParam String projectName, Authentication auth) throws IOException, GitAPIException {
        return projectService.createNewProject(projectName, auth.getName());
    }

    @DeleteMapping("/projects/{projectId}")
    public void deleteProject(@PathVariable UUID projectId, Authentication auth) {
        projectService.deleteProject(projectId, auth.getName());
    }

    // TODO: сюда ещё бахнуть проверку по auth
    @PostMapping("/projects/{projectId}/add-owner")
    public void addOwner(@PathVariable UUID projectId, @RequestParam String email) {
        projectService.addOwner(projectId, email);
    }

    @PostMapping("/projects/{projectId}/remove-owner")
    public void removeOwner(@PathVariable UUID projectId, @RequestParam String email) {
        projectService.removeOwner(projectId, email);
    }

}
