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

import java.io.IOException;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileApiController {

    private final FileService fileService;
    private final GitService gitService;
    private final LogService logService;

    /**
     * Приём файла с фронта и проверка его расширения на допустимость
     */
    @PostMapping(value = "/projects/{projectId}/upload", headers = "content-type=multipart/*")
    public FileItemDTO handleFileUpload(@PathVariable UUID projectId, @RequestParam("file") MultipartFile file, @RequestParam("path") String path, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s has uploaded file %s", auth.getName(), path));
        return fileService.save(file, path, projectId);
    }

    /**
     * Возврат рабочих файлов пользователю через объект
     *
     * @return Список объектов-файлов: имя, тип и содержимое
     */
    @GetMapping("/projects/{projectId}/files")
    public List<FileItemDTO> returnFileList(@PathVariable UUID projectId) throws IOException {
        return fileService.listFiles(projectId);
    }

    @PostMapping("/projects/{projectId}/commit-files")
    public List<String> returnCommitFileList(@PathVariable UUID projectId, @RequestBody List<String> fileIds) throws IOException {
        return gitService.getCommitFilesList(projectId, fileIds);
    }

    /**
     * Перезапись содержимого файлов
     */
    @PutMapping("/projects/{projectId}/files")
    public void saveFiles(@PathVariable UUID projectId, @RequestBody List<FileItemDTO> files, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s saved file changes", auth.getName()));
        fileService.rewriteFiles(files, projectId);
    }

    @DeleteMapping("/projects/{projectId}/files")
    public void deleteFile(@PathVariable UUID projectId, @RequestParam String path, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s has deleted file %s", auth.getName(), path));
        fileService.deleteFile(path, projectId);
    }

    @PostMapping("/projects/{projectId}/rollback/")
    public void rollback(@PathVariable UUID projectId, @RequestBody CommitDTO.File file, @RequestParam String commitDate, Authentication auth) throws IOException, GitAPIException {
        logService.log(auth.getName(), projectId, String.format("User %s rolled back file %s to %s", auth.getName(), file.name, commitDate));
        gitService.rollback(projectId, file, commitDate);
    }
}
