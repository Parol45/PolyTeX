package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import ru.test.restservice.dto.CompilationResultDTO;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.service.CompilerService;
import ru.test.restservice.service.FileService;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CompilerController {

    private final FileService fileService;
    private final CompilerService compilerService;

    /**
     * Метод, обрабатывающий запросы на компиляцию с фронтенда
     *
     * @param target имя компилируемого .tex файла
     * @return JSON с ошибкой компиляции или путь к pdf
     */
    @PostMapping("/compile")
    public CompilationResultDTO compileHandler(@RequestParam String target, @RequestBody List<FileItemDTO> files) throws IOException {
        fileService.rewriteFiles(files);
        return compilerService.compileTexFile(compilerService.workingFolder, target);
    }
}
