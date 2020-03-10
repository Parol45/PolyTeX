package ru.test.restservice.service;

import org.springframework.stereotype.Service;
import ru.test.restservice.dto.CompilationResultDTO;
import ru.test.restservice.exceptions.CompilationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.test.restservice.MainApplication.isWindows;

@Service
public class CompilerService {

    // TODO: после добавления авторизации связать компиляцию с рабочей папкой и компилировать по filename
    public String workingFolder = "test";

    /**
     * Метод, создающий в папке пользователя новый .tex файл
     * или перезаписывающий уже существующий
     *
     * @param text содержимое создаваемого .tex файла
     */
    public void createTexFile(String text) throws IOException {
        Path path = Paths.get("test/test.tex");
        Files.createDirectories(Paths.get(workingFolder));
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Метод, обращающийся к командной строке для вызова latex компилятора
     *
     * @param filename имя компилируемого файла
     */
    public CompilationResultDTO compileTexFile(String folder, String filename) {
        StringBuilder messageBuilder = new StringBuilder();
        String line, message, path;
        try {
            ProcessBuilder pBuilder = new ProcessBuilder();
            if (isWindows) {
                pBuilder.command("cmd.exe", "/c", "pdflatex --synctex=1 --interaction=nonstopmode \"" + filename + "\"");
            } else {
                pBuilder.command("sh", "-c", "pdflatex --synctex=1 --interaction=nonstopmode \"" + filename + "\"");
            }
            pBuilder.directory(new File(folder));
            Process proc = pBuilder.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                messageBuilder.append(line);
            }
            proc.waitFor();
            in.close();
            message = messageBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CompilationException(e.getMessage());
        }
        // TODO: учесть fatal error и другие
        if (message.contains("! LaTeX Error:")) {
            path = "";
        } else {
            path = "test/" + filename.replace(".tex", ".pdf");
        }
        return new CompilationResultDTO(message, path);
    }

}

