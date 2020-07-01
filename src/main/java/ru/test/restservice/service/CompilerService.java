package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dto.CompilationResultDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.exceptions.NotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static ru.test.restservice.utils.TerminalUtils.executeTerminalCommand;


@Service
@RequiredArgsConstructor
public class CompilerService {

    private final ProjectRepository projectRepository;

    /**
     * Метод, обращающийся к командной строке для вызова latex компилятора
     *
     * @param filepath путь до компилируемого файла
     */
    public CompilationResultDTO compileTexFile(String filepath, UUID projectId) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        String latexMessage, biberMessage = "",
                fullPathToFile = project.path + filepath,
                filename = Paths.get(fullPathToFile).getFileName().toString(),
                parentDir = Paths.get(fullPathToFile).getParent().toString(),
                pdfLaTeXCommand = String.format("pdflatex --synctex=1 --interaction=nonstopmode \"%s\"", filename),
                biberCommand = String.format("biber \"%s\"", filename.replaceAll("\\.tex$", ""));

        // Удаляю, потому что удачность компиляции зависит от появления пдфника
        Files.deleteIfExists(Paths.get(fullPathToFile.replaceAll("\\.tex$", ".pdf")));

        latexMessage = executeTerminalCommand(pdfLaTeXCommand, parentDir);
        Path bibliographyFile = Paths.get(fullPathToFile.replaceAll("\\.tex$", ".bcf"));
        if (Files.exists(bibliographyFile)) {
            biberMessage = executeTerminalCommand(biberCommand, parentDir);
        }

        String resultFilepath = filepath.replaceAll("\\.tex$", ".pdf");
        if (Files.exists(Paths.get(project.path + resultFilepath))) {
            return new CompilationResultDTO(latexMessage, biberMessage, project.path + resultFilepath);
        } else {
            return new CompilationResultDTO(latexMessage, biberMessage, "");
        }
    }

}

