package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.test.restservice.dto.CompilationResultDTO;
import ru.test.restservice.service.CompilerService;

import java.io.IOException;



@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CompilerController {

    private final CompilerService compilerService;

    /**
     * Метод, обрабатывающий запросы на компиляцию с фронтенда
     *
     * @param textInp содержимое компилируемого .tex файла
     * @return JSON с ошибкой компиляции или путь к pdf
     */
    @PostMapping("/compile")
    public CompilationResultDTO compileHandler(@RequestBody String textInp) throws IOException {
        compilerService.createTexFile(textInp);
        return compilerService.compileTexFile(compilerService.workingFolder, "test.tex");
    }
}
