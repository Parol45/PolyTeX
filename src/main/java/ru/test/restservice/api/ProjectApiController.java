package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.test.restservice.dto.ProjectDTO;
import ru.test.restservice.service.LogService;
import ru.test.restservice.service.ProjectService;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectApiController {

    private final ProjectService projectService;
    private final LogService logService;

    @PostMapping("/projects")
    public ProjectDTO createNewProject(@RequestParam String projectName, Authentication auth) throws IOException, GitAPIException {
        ProjectDTO project = projectService.createNewProject(projectName, auth.getName());
        logService.log(auth.getName(), project.getId(), String.format("User %s created new project: %s", auth.getName(), projectName));
        return project;
    }

    @PostMapping("/projects/{projectId}/add-owner")
    public void addOwner(@PathVariable UUID projectId, @RequestParam String email, Authentication auth) {
        logService.log(auth.getName(), projectId, String.format("User %s added to project %s contributors %s", auth.getName(), projectId, email));
        projectService.addOwner(projectId, email, auth.getName());
    }

    @PostMapping("/projects/{projectId}/remove-owner")
    public void removeOwner(@PathVariable UUID projectId, @RequestParam String email, Authentication auth) {
        logService.log(auth.getName(), projectId, String.format("User %s removed from project %s contributors %s", auth.getName(), projectId, email));
        projectService.removeOwner(projectId, email, auth.getName());
    }

    @GetMapping("/projects/{projectId}/clear-aux")
    public void clearAuxFiles(@PathVariable UUID projectId, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s deleted aux files from %s project", auth.getName(), projectId));
        projectService.clearAuxFiles(projectId, auth.getName());
    }

}
