package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dto.CompilationResultDTO;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.exceptions.CompilationException;
import ru.test.restservice.exceptions.NotFoundException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CompilerService {

    private final ProjectRepository projectRepository;

    public String executeTerminalCommand(String command, String pathToFile) {
        StringBuilder messageBuilder = new StringBuilder();
        try {
            ProcessBuilder pBuilder = new ProcessBuilder();
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows) {
                pBuilder.command("cmd.exe", "/c", command);
            } else {
                pBuilder.command("sh", "-c", command);
            }
            pBuilder.directory(new File(pathToFile));
            Process proc = pBuilder.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                messageBuilder.append(line);
            }
            proc.waitFor();
            in.close();
        } catch (Exception e) {
            throw new CompilationException(e.getMessage());
        }
        return messageBuilder.toString();
    }

    /**
     * Метод, обращающийся к командной строке для вызова latex компилятора
     *
     * @param file компилируемый файл
     */
    public CompilationResultDTO compileTexFile(FileItemDTO file, UUID projectId) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        Files.deleteIfExists(Paths.get(project.path + file.path.replaceAll("\\.tex$", ".pdf")));
        String latexMessage, biberMessage = "",
                pdfLaTeX = String.format("pdflatex --synctex=1 --interaction=nonstopmode \"%s\"", file.name),
                biber = String.format("biber.exe \"%s\"", file.name.replaceAll("\\.tex$", "")),
                pathToFile = project.path + Paths.get(file.path).getParent();

        latexMessage = executeTerminalCommand(pdfLaTeX, pathToFile);
        Path bibliographyFile = Paths.get(project.path + file.path.replaceAll("\\.tex$", ".bcf"));
        if (Files.exists(bibliographyFile)) {
            biberMessage = executeTerminalCommand(biber, pathToFile);
        }

        String resultFilePath = file.path.replaceAll("\\.tex$", ".pdf");
        if (Files.exists(Paths.get(project.path + resultFilePath))) {
            return new CompilationResultDTO(latexMessage, biberMessage, project.path + resultFilePath);
        } else {
            return new CompilationResultDTO(latexMessage, biberMessage, "");
        }
    }

}

