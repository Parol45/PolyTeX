package ru.test.restservice.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.test.restservice.service.ProjectService;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping(value = {"", "/"})
    public RedirectView redir() {
        return new RedirectView("/projects");
    }

    @GetMapping(value = "/projects")
    public ModelAndView listProjects(Authentication auth) {
        return projectService.listProjects(auth);
    }

    @PostMapping("/projects")
    public RedirectView createNewProject(@RequestParam String projectName, Authentication auth) throws IOException {
        projectService.createNewProject(projectName, auth);
        return new RedirectView("/projects");
    }

    @GetMapping("/delete-project")
    public RedirectView deleteProject(@RequestParam UUID projectId) throws IOException {
        projectService.deleteProject(projectId);
        return new RedirectView("/projects");
    }

    @GetMapping("/projects/{projectId}")
    public ModelAndView openProject(@PathVariable UUID projectId, Authentication auth) {
        projectService.checkOwnership(projectId, auth.getName());
        ModelAndView main = new ModelAndView("index");
        main.addObject("projectId", projectId);
        return main;
    }

    @PostMapping("/projects/{projectId}/add-owner")
    public RedirectView addOwner(@PathVariable UUID projectId, @RequestParam String email) {
        projectService.addOwner(projectId, email);
        return new RedirectView("/projects");
    }

    @PostMapping("/projects/{projectId}/remove-owner")
    public RedirectView removeOwner(@PathVariable UUID projectId, @RequestParam String email) {
        projectService.removeOwner(projectId, email);
        return new RedirectView("/projects");
    }

}