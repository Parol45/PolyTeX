package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dto.CommitDTO;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.service.FileService;
import ru.test.restservice.service.GitService;
import ru.test.restservice.service.LogService;
import ru.test.restservice.service.ProjectService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static ru.test.restservice.utils.FileUtils.packZip;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileApiController {

    private final ProjectService projectService;
    private final FileService fileService;
    private final GitService gitService;
    private final LogService logService;

    /**
     * Приём файла с фронта и проверка его расширения на допустимость
     */
    @PostMapping(value = "/projects/{projectId}/upload", headers = "content-type=multipart/*")
    public FileItemDTO handleFileUpload(@PathVariable UUID projectId, @RequestParam("file") MultipartFile file, @RequestParam("path") String path, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s has uploaded file %s", auth.getName(), path));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        return fileService.save(file, path, projectId);
    }

    /**
     * Возврат рабочих файлов пользователю через объект
     *
     * @return Список объектов-файлов: имя, тип и содержимое
     */
    @GetMapping("/projects/{projectId}/files")
    public List<FileItemDTO> returnFileList(@PathVariable UUID projectId, Authentication auth) throws IOException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        return fileService.listFiles(projectId);
    }

    @GetMapping("/projects/{projectId}/get-file")
    public List<String> returnFileContent(@PathVariable UUID projectId, @RequestParam String filepath, Authentication auth) throws IOException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        return Files.readAllLines( Paths.get( "projects/" + projectId + "/" + filepath.substring(1)));
    }

    @GetMapping("/projects/{projectId}/commit-file")
    public FileItemDTO returnCommitFile(@PathVariable UUID projectId, @RequestParam String fileId, Authentication auth) throws IOException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        return new FileItemDTO(null, null, gitService.getCommitFile(projectId, fileId), null);
    }

    /**
     * Перезапись содержимого файлов
     */
    @PutMapping("/projects/{projectId}/files")
    public void saveFiles(@PathVariable UUID projectId, @RequestBody List<FileItemDTO> files, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s saved file changes", auth.getName()));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        fileService.rewriteFiles(files, projectId);
    }

    @DeleteMapping("/projects/{projectId}/files")
    public void deleteFile(@PathVariable UUID projectId, @RequestParam String path, Authentication auth) throws IOException, GitAPIException {
        logService.log(auth.getName(), projectId, String.format("User %s has deleted file %s", auth.getName(), path));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        fileService.deleteFile(path, projectId);
    }

    @PostMapping("/projects/{projectId}/rollback")
    public void rollback(@PathVariable UUID projectId, @RequestBody CommitDTO.File file, @RequestParam String commitDate, Authentication auth) throws IOException, GitAPIException {
        logService.log(auth.getName(), projectId, String.format("User %s rolled back file %s to %s", auth.getName(), file.path, commitDate));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        fileService.rewriteFiles(gitService.rollback(projectId, file, commitDate, auth.getName()), projectId);
    }

    @PostMapping("/projects/{projectId}/commit")
    public void commit(@PathVariable UUID projectId, Authentication auth) throws IOException, GitAPIException {
        logService.log(auth.getName(), projectId, String.format("User %s has committed project %s", auth.getName(), projectId));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        projectService.performCommit(projectId, auth.getName());
    }

    @GetMapping("/projects/{projectId}/clear-aux")
    public void clearAuxFiles(@PathVariable UUID projectId, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s deleted aux files from %s project", auth.getName(), projectId));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        fileService.clearAuxFiles(projectId);
    }

    @GetMapping("/projects/{projectId}/archive-project")
    public void archiveProject(@PathVariable UUID projectId, Authentication auth) throws IOException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        packZip(projectId);
    }
}
