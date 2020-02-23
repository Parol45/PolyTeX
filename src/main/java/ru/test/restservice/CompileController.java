package ru.test.restservice;

import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
public class CompileController {
    String text = "";

    @GetMapping(value="/api/save")
    public String getResource() {
        return text;
    }

    @PostMapping(value = "/api/compile")
    public String postText(@RequestBody String textInp) {
        text = textInp;
        try {
            Path path = Paths.get("test/test.tex");
            if (!Files.exists(Paths.get("test"))) {
                Files.createDirectory(Paths.get("test"));
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(path, text.getBytes(StandardCharsets.UTF_8));

            Runtime r = Runtime.getRuntime();
            Process p;
            if (RestServiceApplication.isWindows)
                p = r.exec("cmd /c pdflatex --synctex=1 --interaction=nonstopmode --output-directory=\"test\" \"test.tex\"");
            else
                p = r.exec("sh -c 'pdflatex --synctex=1 --interaction=nonstopmode --output-directory=\"test\" \"test.tex\"'");
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            p.waitFor();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }
}
