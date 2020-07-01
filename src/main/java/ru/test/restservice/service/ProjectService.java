package ru.test.restservice.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.config.AdminProperties;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dao.TemplateRepository;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.dto.CommitDTO;
import ru.test.restservice.dto.ProjectDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.entity.User;
import ru.test.restservice.exceptions.GenericException;
import ru.test.restservice.exceptions.NotFoundException;
import ru.test.restservice.exceptions.ProjectAccessException;
import ru.test.restservice.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AdminProperties adminProperties;
    private final TemplateRepository templateRepository;

    private final GitService gitService;

    /**
     * Список для учёта действий над проектами
     */
    public Map<UUID, UserLastAccess> activeProjects = new HashMap<>();

    /**
     * Запланированное задание по очистке списка действий над проектами (каждые 2 часа)
     */
    @Scheduled(fixedRate = 7200000)
    public void activeProjectsCleanup() throws IOException, GitAPIException {
        for (UUID key : activeProjects.keySet()) {
            Duration duration = Duration.between(activeProjects.get(key).lastAccess, LocalDateTime.now());
            long minutesPassed = Math.abs(duration.toMinutes());
            if (minutesPassed > adminProperties.idleTimeout) {
                Optional<Project> proj = projectRepository.findById(key);
                if (proj.isPresent()) {
                    gitService.commit(proj.get().path, "Another test", userRepository.findById(activeProjects.get(key).userId).get().email);
                }
                activeProjects.remove(key);
            }
        }
    }

    /**
     * Проверка работает ли с проектом другой пользователь
     */
    public boolean checkIfBusy(UUID projectId, UUID userId) {
        if (activeProjects.containsKey(projectId) && !userId.equals(activeProjects.get(projectId).userId)) {
            Duration duration = Duration.between(activeProjects.get(projectId).lastAccess, LocalDateTime.now());
            long minutesPassed = Math.abs(duration.toMinutes());
            return minutesPassed < adminProperties.idleTimeout;
        } else {
            return false;
        }
    }

    /**
     * Проверка доступна ли компиляция для пользователя
     * (для регулирования нагрузки на систему используется интервал между компиляциями)
     */
    public boolean canCompile(User user) {
        for (UUID key : activeProjects.keySet()) {
            UserLastAccess ula = activeProjects.get(key);
            if (ula.userId.equals(user.getId()) && ula.lastCompilation != null) {
                Duration duration = Duration.between(ula.lastAccess, LocalDateTime.now());
                long secondsPassed = Math.abs(duration.getSeconds());
                if (secondsPassed < adminProperties.compilationInterval) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Обновление времени последнего действия над проектом в списке
     */
    public void tryToRefreshLastAccessDate(UUID projectId, String username) {
        tryToRefreshLastAccessDate(projectId, username, false);
    }

    /**
     * Возврат списка проектов, к которым пользователь имеет доступ
     */
    public List<ProjectDTO> listProjectsFor(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(NotFoundException::new);
        return projectRepository
                .findAll()
                .stream()
                .filter(pr -> pr.owners.contains(user))
                .map(pr -> new ProjectDTO(pr, checkIfBusy(pr.id, user.getId()), user.getId().equals(pr.creatorId), pr.creatorId, null))
                .collect(Collectors.toList());
    }

    public void tryToRefreshLastAccessDate(UUID projectId, String username, boolean isCompiled) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(NotFoundException::new);
        if (checkIfBusy(projectId, user.getId())) {
            throw new ProjectAccessException();
        } else if (isCompiled && !canCompile(user)) {
            throw new GenericException("Вы компилируете слишком часто");
        }
        else {
            LocalDateTime lastTimeCompiled = null;
            if (activeProjects.containsKey(projectId)) {
                UserLastAccess ula = activeProjects.get(projectId);
                lastTimeCompiled = ula.lastCompilation;
                if (!user.getId().equals(ula.userId)) {
                    Optional<Project> proj = projectRepository.findById(projectId);
                    if (proj.isPresent()) {
                        try {
                            gitService.commit(proj.get().path, "Another test", user.email);
                        } catch (IOException | GitAPIException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (isCompiled) {
                activeProjects.put(projectId, new UserLastAccess(user.getId(), LocalDateTime.now(), LocalDateTime.now()));
            } else {
                activeProjects.put(projectId, new UserLastAccess(user.getId(), LocalDateTime.now(), lastTimeCompiled));
            }
        }
    }

    /**
     * Список всех проектов в системе
     */
    public List<ProjectDTO> listAllProjects() {
        return projectRepository
                .findAll()
                .stream()
                .map(pr -> new ProjectDTO(pr, checkIfBusy(pr.id, UUID.randomUUID()), true, pr.creatorId, userRepository.findById(pr.creatorId).get().email))
                .collect(Collectors.toList());
    }

    /**
     * Создание нового проекта
     */
    public ProjectDTO createNewProject(String projectName, UUID templateId, MultipartFile file, String email) throws IOException, GitAPIException {
        // TODO: Можно вынести как новую системную настройку
        if (projectName.length() > 50) {
            throw new GenericException("Имя проекта слишком длинное");
        }
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        long userProjectCount = projectRepository
                .findAll()
                .stream()
                .filter(pr -> pr.owners.contains(currentUser))
                .count();
        // проверка на максимальное кол-во проектов
        if (userProjectCount < adminProperties.maxProjectCount) {
            UUID newId = UUID.randomUUID();
            Project newProj = new Project(newId, projectName, "projects/" + newId, currentUser.getId(), Collections.singleton(currentUser));
            projectRepository.save(newProj);
            Files.createDirectories(Paths.get("projects/" + newId));
            // проверка на создание проекта с помощью шаблона, загрузки архива или пустого
            if (templateId != null) {
                templateRepository.findById(templateId).orElseThrow(NotFoundException::new);
                FileUtils.copyContentTo("templates/" + templateId, "projects/" + newId);
            } else if (file != null) {
                Path zip = Paths.get(String.format("projects/%s/%s", newId, file.getOriginalFilename()));
                file.transferTo(zip);
                FileUtils.unpackZip(zip);
            } else {
                Files.createFile(Paths.get("projects/" + newId + "/" + projectName + ".tex"));
            }
            gitService.initRepository("projects/" + newId);
            return new ProjectDTO(newProj, checkIfBusy(newProj.id, currentUser.getId()), true, currentUser.getId(), currentUser.email);
        } else {
            throw new GenericException("You have max number of projects");
        }
    }

    /**
     * Возврат сущности-проекта, если пользователь им владеет
     *
     * @param forced возвращает project даже если пользователь работает с проектом
     *               (для панели администратора)
     */
    public Project getProjectForUser(UUID projectId, String email, boolean forced) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        if (!project.owners.contains(user)) {
            throw new NotFoundException();
        }
        if (!forced && checkIfBusy(projectId, user.getId())) {
            throw new ProjectAccessException();
        }
        return project;
    }

    public Project getProjectForUser(UUID projectId, String email) {
        return getProjectForUser(projectId, email, false);
    }

    /**
     * Добавление пользователя во владельцы проекта
     */
    public void addOwner(UUID projectId, String email, String owner) {
        if (email.length() > 50) {
            throw new GenericException("Имя пользователя слишком длинное");
        }
        Project project = getProjectForUser(projectId, owner);
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        long userProjectCount = projectRepository
                .findAll()
                .stream()
                .filter(pr -> pr.owners.contains(user))
                .count();
        if (userProjectCount >= adminProperties.maxProjectCount) {
            throw new GenericException("Пользователь уже имеет максимальное количество проектов");
        }
        if (!project.owners.contains(user)) {
            project.owners.add(user);
            userRepository.save(user);
            projectRepository.save(project);
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Удаление пользователя из владельцев проекта
     */
    public void removeOwner(UUID projectId, String email, String owner) {
        Project project = getProjectForUser(projectId, owner, true);
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        User userOwner = userRepository.findByEmail(owner)
                .orElseThrow(NotFoundException::new);
        if (project.owners.contains(user) && userOwner.getId().equals(project.creatorId)) {
            project.owners.remove(user);
            userRepository.save(user);
            projectRepository.save(project);
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Удаление проекта
     */
    public void deleteProject(UUID projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundException::new);
        if (project.creatorId.equals(user.getId())) {
            projectRepository.delete(project);
            FileUtils.deleteProjectFromDisk(projectId);
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Получение списка всех коммитов в репозитории проекта
     */
    public List<CommitDTO> listCommits(UUID projectId, String email) throws IOException, GitAPIException {
        Project project = getProjectForUser(projectId, email);
        return gitService.getCommitList(project.path);
    }

    /**
     * Ручное создание контрольной точки
     */
    public void performCommit(UUID projectId, String username) throws IOException, GitAPIException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        gitService.commit(project.path, "Another test", username);
    }

    /**
     * Класс для учёта действий над проектами
     */
    @AllArgsConstructor
    public static class UserLastAccess {
        UUID userId;
        LocalDateTime lastAccess;
        LocalDateTime lastCompilation;
    }
}
