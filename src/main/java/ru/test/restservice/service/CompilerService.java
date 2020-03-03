package ru.test.restservice.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.test.restservice.dto.CompilationResultDTO;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.test.restservice.MainApplication.isWindows;

public class CompilerService {

    // TODO: после добавления авторизации связать компиляцию с рабочей папкой и компилировать по filename
    public static String workingFolder = "test";

    // TODO: перенести все эксепшны в хандлер
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public static class CompilationException extends RuntimeException {
        public CompilationException(String message) {
            super(message);
        }
    }

    // TODO: добавить функцию сохранения изменений в файле

    /**
     * Метод, создающий в папке пользователя новый .tex файл
     * или перезаписывающий уже существующий
     *
     * @param text содержимое создаваемого .tex файла
     */
    public static void createTexFile(String text) throws IOException {
        Path path = Paths.get("test/test.tex");
        Files.createDirectories(Paths.get(workingFolder));
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Метод, обращающийся к командной строке для вызова latex компилятора
     *
     * @param filename имя компилируемого файла
     */
    public static CompilationResultDTO compileTexFile(String folder, String filename) {
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
        // TODO: учесть fatal error и другие и решить траблы с кодировкой
        if (message.contains("! LaTeX Error:")) {
            path = "";
        }
        else {
            path = "test/" + filename.replace(".tex", ".pdf");
        }
        return new CompilationResultDTO(message, path);
    }

}

