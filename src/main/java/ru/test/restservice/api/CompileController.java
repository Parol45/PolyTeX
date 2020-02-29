package ru.test.restservice.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.test.restservice.domain.CompilationResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.test.restservice.MainApplication.isWindows;

//TODO: после авторизации связать компиляцию с рабочей папкой

@RestController
public class CompileController {

    //TODO: заменить записью изменений после компиляции/каждые N секунд работы с проектом
    private void createTexFile(String text) {
        Path path = Paths.get("test/test.tex");
        try {
            if (!Files.exists(Paths.get("test"))) {
                Files.createDirectory(Paths.get("test"));
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(path, text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Результирующий пдф закидывается в рабочую папку юзера
    private String compileTexFile(){
        StringBuilder message = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder();
            if (isWindows) {
                builder.command("cmd.exe", "/c", "pdflatex --synctex=1 --interaction=nonstopmode \"test.tex\"");
            } else {
                builder.command("sh", "-c", "pdflatex --synctex=1 --interaction=nonstopmode \"test.tex\"");
            }
            builder.directory(new File("test"));
            Process p = builder.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                message.append(line);
            }
            p.waitFor();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message.toString();
    }

    @PostMapping("/api/compile")
    public CompilationResult compileSourceTexCode(@RequestBody String textInp) {
        createTexFile(textInp);
        String compRet = compileTexFile();
        return new CompilationResult(compRet.contains("! LaTeX Error:") ? compRet : "ok", "there will be relative path to file");
    }
}
