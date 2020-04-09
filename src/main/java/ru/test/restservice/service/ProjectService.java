package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.dto.CommitDTO;
import ru.test.restservice.dto.ProjectDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.entity.User;
import ru.test.restservice.exceptions.NotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.test.restservice.utils.FileUtils.isAuxFile;


@Service
@RequiredArgsConstructor
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final GitService gitService;

    public List<ProjectDTO> listProjects(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(NotFoundException::new);
        return projectRepository
                .findAll()
                .stream()
                .filter(pr -> pr.owners.contains(user))
                .map(ProjectDTO::new)
                .collect(Collectors.toList());
    }

    public ProjectDTO createNewProject(String projectName, String email) throws IOException, GitAPIException {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        UUID newId = UUID.randomUUID();
        Project newProj = new Project(newId, projectName, "projects/" + newId, Collections.singleton(currentUser));
        projectRepository.save(newProj);
        Files.createDirectories(Paths.get("projects/" + newId));
        Files.createFile(Paths.get("projects/" + newId + "/" + projectName + ".tex"));
        gitService.initRepository("projects/" + newId);
        return new ProjectDTO(newProj);
    }

    public void deleteProject(UUID projectId, String email) {
        Project projectToDelete = checkOwnership(projectId, email);
        FileSystemUtils.deleteRecursively(Paths.get(projectToDelete.path).toFile());
        projectRepository.delete(projectToDelete);
    }

    public Project checkOwnership(UUID projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        if (!project.owners.contains(user)) {
            throw new NotFoundException();
        }
        return project;
    }

    // TODO: добавить виды прав?
    // TODO: удалять проект, если не осталось владельцев
    // TODO: блокирование проекта для одновременной работы

    public void addOwner(UUID projectId, String email, String owner) {
        Project project = checkOwnership(projectId, owner);
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        if (!project.owners.contains(user)) {
            project.owners.add(user);
            userRepository.save(user);
            projectRepository.save(project);
        } else {
            throw new NotFoundException();
        }
    }

    public void removeOwner(UUID projectId, String email, String owner) {
        Project project = checkOwnership(projectId, owner);
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        if (project.owners.contains(user)) {
            project.owners.remove(user);
            userRepository.save(user);
            projectRepository.save(project);
        } else {
            throw new NotFoundException();
        }
    }

    public List<CommitDTO> listCommits(UUID projectId, String email) throws IOException, GitAPIException {
        Project project = checkOwnership(projectId, email);
        gitService.commit(project.path, "Another test");
        return gitService.getCommitList(project.path);
    }

    public void clearAuxFiles(UUID projectId, String email) throws IOException {
        Project project = checkOwnership(projectId, email);
        Path projectDest = Paths.get(project.path);
        try (Stream<Path> paths = Files.walk(projectDest)) {
            paths
                    .filter(path -> Files.isRegularFile(path))
                    .forEach(file -> {
                        if (isAuxFile(file.toString())) {
                            try {
                                Files.delete(file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }
}
