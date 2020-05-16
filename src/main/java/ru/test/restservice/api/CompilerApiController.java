package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.test.restservice.dto.CompilationResultDTO;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.service.CompilerService;
import ru.test.restservice.service.FileService;
import ru.test.restservice.service.LogService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CompilerApiController {

    private final FileService fileService;
    private final CompilerService compilerService;
    private final LogService logService;

    /**
     * Метод, обрабатывающий запросы на компиляцию с фронтенда
     *
     * @param targetFilepath путь до компилируемого .tex файла
     * @return JSON с ошибкой компиляции или путь к pdf
     */
    @PostMapping("/projects/{projectId}/compile")
    public CompilationResultDTO compileHandler(@PathVariable UUID projectId, @RequestParam String targetFilepath, @RequestBody List<FileItemDTO> files, Authentication auth) throws IOException {
        fileService.rewriteFiles(files, projectId);
        FileItemDTO targetFile = files.stream()
                .filter(f -> targetFilepath.equals(f.path))
                .findFirst()
                .get();
        logService.log(auth.getName(), projectId, String.format("User %s tried to compile file %s", auth.getName(), targetFilepath));
        return compilerService.compileTexFile(targetFile, projectId);
    }
}
