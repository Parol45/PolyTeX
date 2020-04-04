package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.servlet.ModelAndView;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.entity.Project;
import ru.test.restservice.entity.User;
import ru.test.restservice.exceptions.NotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProjectService {

    // TODO: добавление пользователя в проект

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public ModelAndView listProjects(Authentication auth) {
        ModelAndView projs = new ModelAndView("projects");
        User user = userRepository.findByEmail(auth.getName());
        projs.addObject("projects",
                projectRepository
                        .findAll()
                        .stream()
                        .filter(pr -> pr.owners.contains(user))
                        .collect(Collectors.toList()));
        return projs;
    }

    public void createNewProject(String projectName, Authentication auth) throws IOException {
        User currentUser = userRepository.findByEmail(auth.getName());
        UUID newId = UUID.randomUUID();
        Project newProj = new Project(newId, projectName, "projects/" + newId, Collections.singleton(currentUser));
        projectRepository.save(newProj);
        Files.createDirectories(Paths.get("projects/" + newId));
        Files.createFile(Paths.get("projects/" + newId + "/" + projectName + ".tex"));
    }

    public void deleteProject(UUID projectId) throws IOException {
        Project projectToDelete = projectRepository.findById(projectId).get();
        FileSystemUtils.deleteRecursively(Paths.get(projectToDelete.path));
        projectRepository.delete(projectToDelete);
    }

    public void checkOwnership(UUID projectId, String email) {
        Optional<Project> project = projectRepository.findById(projectId);
        User user = userRepository.findByEmail(email);
        if (user == null || !project.isPresent() || !project.get().owners.contains(user)) {
            throw new NotFoundException();
        }
    }

    // TODO: добавить виды прав?
    // TODO: сообщение для страницы ошибки
    // TODO: удалять проект, если не осталось владельцев

    public void addOwner(UUID projectId, String email) {
        Optional<Project> project = projectRepository.findById(projectId);
        User user = userRepository.findByEmail(email);
        if (user != null && project.isPresent() && !project.get().owners.contains(user)) {
            Project temp = project.get();
            temp.owners.add(user);
            userRepository.save(user);
            projectRepository.save(temp);
        } else {
            throw new NotFoundException();
        }
    }

    public void removeOwner(UUID projectId, String email) {
        Optional<Project> project = projectRepository.findById(projectId);
        User user = userRepository.findByEmail(email);
        if (user != null && project.isPresent() && project.get().owners.contains(user)) {
            Project temp = project.get();
            temp.owners.remove(user);
            userRepository.save(user);
            projectRepository.save(temp);
        } else {
            throw new NotFoundException();
        }
    }
}
