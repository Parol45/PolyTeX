package ru.test.restservice.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.test.restservice.dao.TemplateRepository;
import ru.test.restservice.service.FileService;
import ru.test.restservice.service.ProjectService;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Api(description="Контроллер отвечающий за страницы LaTeX проекта")
public class ProjectController {

    private final ProjectService projectService;
    private final FileService fileService;
    private final TemplateRepository templateRepository;

    @GetMapping(value = {"", "/"})
    @ApiOperation("Переадресация на страницу со списком проектов")
    public RedirectView redir() {
        return new RedirectView("/projects");
    }

    @GetMapping(value = "/projects")
    @ApiOperation("Страница со списком проектов")
    public ModelAndView listProjects(Authentication auth) {
        ModelAndView projs = new ModelAndView("project/list");
        projs.addObject("projects", projectService.listProjectsFor(auth.getName()));
        projs.addObject("userEmail", auth.getName());
        projs.addObject("admin", auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        projs.addObject("templates", templateRepository.findAll());
        return projs;
    }

    @GetMapping("/projects/{projectId}/")
    @ApiOperation("Страница LaTeX-редактора для конкретного проекта")
    public ModelAndView openProject(@PathVariable UUID projectId, Authentication auth) {
        projectService.getProjectForUser(projectId, auth.getName());
        ModelAndView main = new ModelAndView("project/editor");
        main.addObject("projectId", projectId);
        return main;
    }

    @GetMapping("/projects/{projectId}/history")
    @ApiOperation("Страница истории изменения проекта")
    public ModelAndView showChangeHistory(@PathVariable UUID projectId, Authentication auth) throws IOException, GitAPIException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
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