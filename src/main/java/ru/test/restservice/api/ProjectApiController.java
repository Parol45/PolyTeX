package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    public ProjectDTO createNewProject(@RequestParam String projectName, @RequestParam UUID templateId, Authentication auth) throws IOException, GitAPIException, InterruptedException {
        ProjectDTO project = projectService.createNewProject(projectName, templateId, null, auth.getName());
        logService.log(auth.getName(), project.getId(), String.format("User %s created new project: %s", auth.getName(), projectName));
        return project;
    }


    @PostMapping(value = "/projects/upload-project", headers = "content-type=multipart/*")
    public ProjectDTO uploadProject(@RequestParam String projectName, @RequestParam("file") MultipartFile file, Authentication auth) throws IOException, GitAPIException, InterruptedException {
        ProjectDTO project = projectService.createNewProject(projectName, null, file, auth.getName());
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

    @DeleteMapping("/projects/{projectId}/")
    public void deleteProject(@PathVariable UUID projectId, Authentication auth) {
        logService.log(auth.getName(), projectId, String.format("User %s deleted project %s", auth.getName(), projectId));
        projectService.deleteProject(projectId, auth.getName());
    }

    @PostMapping("/projects/{projectId}/remove-yourself")
    public void removeYourself(@PathVariable UUID projectId, Authentication auth) {
        logService.log(auth.getName(), projectId, String.format("User %s removed from project %s contributors %s", auth.getName(), projectId, auth.getName()));
        projectService.removeOwner(projectId, auth.getName(), auth.getName());
    }

}
