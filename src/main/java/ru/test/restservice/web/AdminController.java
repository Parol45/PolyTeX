package ru.test.restservice.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.test.restservice.dao.TemplateRepository;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.service.AdminService;
import ru.test.restservice.service.ProjectService;

import java.io.IOException;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;
    private final ProjectService projectService;
    private final AdminService adminService;

    @GetMapping("/projects")
    public ModelAndView returnProjects() {
        ModelAndView projects = new ModelAndView("admin/projects");
        projects.addObject("projects", projectService.listAllProjects());
        return projects;
    }

    @GetMapping("/settings")
    public ModelAndView returnSettings() throws IOException {
        ModelAndView settings = new ModelAndView("admin/settings");
        settings.addObject("settings", adminService.returnSettings());
        return settings;
    }

    @GetMapping("/templates")
    public ModelAndView returnTemplates() {
        ModelAndView templates = new ModelAndView("admin/templates");
        templates.addObject("templates", templateRepository.findAll());
        return templates;
    }

    @GetMapping("/users")
    public ModelAndView returnUsers() {
        ModelAndView users = new ModelAndView("admin/users");
        users.addObject("users", userRepository.findAll());
        return users;
    }

}
