package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/projects/{projectId}/add-owner")
    public void addOwner(@PathVariable UUID projectId, @RequestParam String email, Authentication auth) {
        projectService.addOwner(projectId, email, auth.getName());
    }

    @PostMapping("/projects/{projectId}/remove-owner")
    public void removeOwner(@PathVariable UUID projectId, @RequestParam String email, Authentication auth) {
        projectService.removeOwner(projectId, email, auth.getName());
    }

    @GetMapping("/projects/{projectId}/clear-aux")
    public void clearAuxFiles(@PathVariable UUID projectId, Authentication auth) throws IOException {
        projectService.clearAuxFiles(projectId, auth.getName());
    }

    @GetMapping("/user")
    public Object user(@AuthenticationPrincipal Object oauth2User) {
        return oauth2User;
    }
}
