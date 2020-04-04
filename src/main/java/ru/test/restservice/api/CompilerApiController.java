package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.test.restservice.dto.CompilationResultDTO;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.service.CompilerService;
import ru.test.restservice.service.FileService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CompilerApiController {

    private final FileService fileService;
    private final CompilerService compilerService;

    /**
     * Метод, обрабатывающий запросы на компиляцию с фронтенда
     *
     * @param targetFilepath путь до компилируемого .tex файла
     * @return JSON с ошибкой компиляции или путь к pdf
     */
    @PostMapping("/projects/{projectId}/compile")
    public CompilationResultDTO compileHandler(@PathVariable UUID projectId, @RequestParam String targetFilepath, @RequestBody List<FileItemDTO> files) throws IOException {
        fileService.rewriteFiles(files, projectId);
        FileItemDTO targetFile = files.stream()
                .filter(f -> targetFilepath.equals(f.path))
                .findFirst()
                .get();
        return compilerService.compileTexFile(targetFile, projectId);
    }
}
