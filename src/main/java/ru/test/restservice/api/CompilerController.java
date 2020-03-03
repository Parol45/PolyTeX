package ru.test.restservice.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.test.restservice.dto.CompilationResultDTO;

import java.io.IOException;

import static ru.test.restservice.service.CompilerService.*;


@RestController
public class CompilerController {

    /**
     * Метод, обрабатывающий запросы на компиляцию с фронтенда
     *
     * @param textInp содержимое компилируемого .tex файла
     * @return JSON с ошибкой компиляции или путь к pdf
     */
    @PostMapping("/api/compile")
    public CompilationResultDTO compileHandler(@RequestBody String textInp) throws IOException {
        createTexFile(textInp);
        return compileTexFile(workingFolder, "test.tex");
    }
}
