package ru.test.restservice.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dto.CommitDTO;
import ru.test.restservice.dto.CompilationResultDTO;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.service.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static ru.test.restservice.utils.FileUtils.packZip;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(description = "API действий с файлами проекта")
public class FileApiController {

    private final ProjectService projectService;
    private final FileService fileService;
    private final GitService gitService;
    private final LogService logService;
    private final CompilerService compilerService;

    /**
     * Приём файла с фронта и проверка его расширения на допустимость
     */
    @PostMapping(value = "/projects/{projectId}/upload", headers = "content-type=multipart/*")
    @ApiOperation("Загрузка своего файла в проект")
    public FileItemDTO handleFileUpload(
            @PathVariable UUID projectId,
            @ApiParam(value = "Файл - картинка или текстовый файл с допустимым расширением", required = true)
            @RequestParam("file") MultipartFile file,
            @ApiParam(value = "Относительный путь в файловой системе для сохранения файла на сервере", required=true)
            @RequestParam("path") String path, Authentication auth) throws IOException {
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
    @ApiOperation("Получение списка всех файлов и папок проект")
    public List<FileItemDTO> returnFileList(@PathVariable UUID projectId, Authentication auth) throws IOException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        return fileService.listFiles(projectId);
    }

    @GetMapping("/projects/{projectId}/get-file")
    @ApiOperation("Получение содержимого текстового файла по его пути")
    public List<String> returnFileContent(
            @PathVariable UUID projectId,
            @ApiParam(value = "Относительный путь в файловой системе до файла", required=true)
            @RequestParam String filepath, Authentication auth) throws IOException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        return Files.readAllLines(Paths.get("projects/" + projectId + "/" + filepath.substring(1)));
    }

    @GetMapping("/projects/{projectId}/commit-file")
    @ApiOperation("Получение списка сохранённых контрольных точек проекта")
    public FileItemDTO returnCommitFile(
            @PathVariable UUID projectId,
            @ApiParam(value = "Id файла в git", required=true)
            @RequestParam String fileId, Authentication auth) throws IOException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        return new FileItemDTO(null, null, gitService.getCommitFile(projectId, fileId), null);
    }

    /**
     * Перезапись содержимого файлов
     */
    @PutMapping("/projects/{projectId}/files")
    @ApiOperation("Сохранение переданного списка изменённых файлов проекта на сервере")
    public void saveFiles(
            @PathVariable UUID projectId,
            @ApiParam(value = "Файлы для сохранения", required=true)
            @RequestBody List<FileItemDTO> files, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s saved file changes", auth.getName()));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        fileService.rewriteFiles(files, projectId);
    }

    @DeleteMapping("/projects/{projectId}/files")
    @ApiOperation("Удаление файла проекта с сервера")
    public void deleteFile(
            @PathVariable UUID projectId,
            @ApiParam(value = "Относительный путь в файловой системе до файла", required=true)
            @RequestParam String path, Authentication auth) throws IOException, GitAPIException {
        logService.log(auth.getName(), projectId, String.format("User %s has deleted file %s", auth.getName(), path));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        fileService.deleteFile(path, projectId);
    }

    @PostMapping("/projects/{projectId}/rollback")
    @ApiOperation("Возвращение файла к состоянию одной из контрольных точек")
    public void rollback(
            @PathVariable UUID projectId,
            @ApiParam(value = "Объект с id и путём до файла", required=true)
            @RequestBody CommitDTO.File file,
            @ApiParam(value = "Дата контрольной точки, к которой происходит откат", required=true)
            @RequestParam String commitDate, Authentication auth) throws IOException, GitAPIException {
        logService.log(auth.getName(), projectId, String.format("User %s rolled back file %s to %s", auth.getName(), file.path, commitDate));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        fileService.rewriteFiles(gitService.rollback(projectId, file, commitDate, auth.getName()), projectId);
    }

    @PostMapping("/projects/{projectId}/commit")
    @ApiOperation("Сохранение текущего состояния файлов проекта в новую контрольную точку")
    public void commit(@PathVariable UUID projectId, Authentication auth) throws IOException, GitAPIException {
        logService.log(auth.getName(), projectId, String.format("User %s has committed project %s", auth.getName(), projectId));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        projectService.performCommit(projectId, auth.getName());
    }

    @GetMapping("/projects/{projectId}/clear-aux")
    @ApiOperation("Удаление ВСЕХ вспомогательных файлов, генерируемых LaTeX компилятором")
    public void clearAuxFiles(@PathVariable UUID projectId, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s deleted aux files from %s project", auth.getName(), projectId));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        fileService.clearAuxFiles(projectId);
    }

    @GetMapping("/projects/{projectId}/archive-project")
    @ApiOperation("Архивация файлов проекта для их загрузки")
    public void archiveProject(@PathVariable UUID projectId, Authentication auth) throws IOException {
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName());
        packZip(projectId);
    }

    @PostMapping("/projects/{projectId}/compile")
    @ApiOperation("Компиляция одного из tex-файлов проекта")
    public CompilationResultDTO compile(
            @PathVariable UUID projectId,
            @ApiParam(value = "Относительный путь в файловой системе до компилируемого файла", required=true)
            @RequestParam String targetFilepath,
            @ApiParam(value = "Список файлов со внесёнными изменениями", required=true)
            @RequestBody List<FileItemDTO> files, Authentication auth) throws IOException {
        logService.log(auth.getName(), projectId, String.format("User %s tried to compile file %s", auth.getName(), targetFilepath));
        projectService.tryToRefreshLastAccessDate(projectId, auth.getName(), true);
        fileService.rewriteFiles(files, projectId);
        return compilerService.compileTexFile(targetFilepath, projectId);
    }
}
