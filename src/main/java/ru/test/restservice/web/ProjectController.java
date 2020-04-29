package ru.test.restservice.web;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.test.restservice.service.FileService;
import ru.test.restservice.service.GitService;
import ru.test.restservice.service.ProjectService;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final FileService fileService;
    private final GitService gitService;

    @GetMapping(value = {"", "/"})
    public RedirectView redir() {
        return new RedirectView("/projects");
    }

    @GetMapping(value = "/projects")
    public ModelAndView listProjects(Authentication auth) {
        ModelAndView projs = new ModelAndView("project/list");
        projs.addObject("projects", projectService.listProjects(auth.getName()));
        return projs;
    }

    @GetMapping("/projects/{projectId}/")
    public ModelAndView openProject(@PathVariable UUID projectId, Authentication auth) {
        projectService.getProjectForUser(projectId, auth.getName());
        ModelAndView main = new ModelAndView("project/editor");
        main.addObject("projectId", projectId);
        return main;
    }

    @GetMapping("/projects/{projectId}/history")
    public ModelAndView showChangeHistory(@PathVariable UUID projectId, Authentication auth) throws IOException, GitAPIException {
        ModelAndView hist = new ModelAndView("project/history");
        hist.addObject("projectId", projectId);
        hist.addObject("commits", projectService.listCommits(projectId, auth.getName()));
        hist.addObject("files", fileService.listFiles(projectId)
                .stream()
                .filter(f -> f.type.equals("txt"))
                .collect(Collectors.toList()));
        return hist;
    }

}