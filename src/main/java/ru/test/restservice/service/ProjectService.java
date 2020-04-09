package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.dto.CommitDTO;
import ru.test.restservice.dto.ProjectDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.entity.User;
import ru.test.restservice.exceptions.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final FileService fileService;

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
        Git git = Git.init().setDirectory(Paths.get("projects/" + newId).toFile()).call();
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").call();
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
    // TODO: сообщение для страницы ошибки
    // TODO: удалять проект, если не осталось владельцев
    // TODO: блокирование проекта для одновременной работы

    public void addOwner(UUID projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);
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

    public void removeOwner(UUID projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);
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

    public List<CommitDTO> commits(UUID projectId, String email) throws IOException, GitAPIException {
        List<CommitDTO> result = new ArrayList<>();
        Project project = checkOwnership(projectId, email);
        Git git = Git.open(new File(project.path + "/.git"));
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Another test").call();
        Repository repository = git.getRepository();
        Iterable<RevCommit> commits = git.log().all().call();
        for (RevCommit commit : commits) {
            List<CommitDTO.File> files = new ArrayList<>();
            RevTree tree = commit.getTree();
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String name = treeWalk.getPathString();
                if (fileService.isTextFile(name)) {
                    files.add(new CommitDTO.File(treeWalk.getObjectId(0).name(), "/" + name));
                }
            }
            result.add(new CommitDTO(String.valueOf(commit.getCommitTime()), new Date(commit.getCommitTime() * 1000L),
                    commit.getAuthorIdent().getName(), commit.getName(), files));
        }
        return result;
    }

}
