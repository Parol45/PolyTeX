import com.owlike.genson.Genson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import ru.test.restservice.MainApplication;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.entity.User;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MainApplication.class)
@ContextConfiguration(classes=JpaConfig.class)
@AutoConfigureMockMvc
public class Tests {

    private static UUID projId = null;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProjectRepository projectRepository;

    List<FileItemDTO> fileList = Collections.singletonList(new FileItemDTO("test1.tex", "txt", "/test1.tex",
            Arrays.asList("\\documentclass[a4paper,12pt]{article}",
                    "\\usepackage{cmap}",
                    "\\usepackage{mathtext}",
                    "\\usepackage[T2A]{fontenc}",
                    "\\usepackage[utf8]{inputenc}",
                    "\\usepackage[english,russian]{babel}",
                    "\\usepackage{graphicx}",
                    "\\begin{document}",
                    "Hello",
                    "\\[S\\Theta = \\rho;\\;ST = \\rho_1;\\;SL = \\rho_2;\\;TL = r\\]",
                    "\\begin{tabular}{ l l l }",
                    "ИСЗ & Дата запуска & Масса, кг  \\\\",
                    "Спутник-1 & 4 октября 1957 & 83.6 \\\\",
                    "Спутник-2 & 3 ноября 1957 & 508.3  \\\\",
                    "Эксплорер-1 & 1 февраля 1958 & 21.5 \\\\",
                    "\\end{tabular}",
                    "",
                    "",
                    "",
                    "Now changes can be saved on server",
                    "",
                    "",
                    "",
                    "\\end{document}")));

    // Проверка обращений к общедоступным страницам
    @Test
    void anonTest() throws Exception {
        Optional<User> dbUser = userRepository.findByEmail("test_test");
        if (dbUser.isPresent()) {
            projectRepository.findAll()
                    .stream()
                    .filter(p -> p.owners.contains(dbUser.get()))
                    .forEach(p -> projectRepository.delete(p));
            userRepository.delete(dbUser.get());
        }

        mockMvc.perform(get("/registration")).andExpect(status().isOk());
        mockMvc.perform(post("/registration")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(buildUrlEncodedFormEntity(
                        "email", "test_test",
                        "password", "12345"
                ))).andExpect(status().isFound());

        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(buildUrlEncodedFormEntity(
                        "email", "test_test",
                        "password", "12345"
                ))).andExpect(status().isFound());

        // isFound грубо говоря означает, что доступа к странице нет и произошёл редирект на /login
        mockMvc.perform(get("/projects"))
                .andExpect(status().isFound());
    }

    // Проверка обращений к страницам, доступным с ролью USER
    @Test
    @WithMockUser(value = "test_test")
    void userTest() throws Exception {
        // Использует созданного в прошлом тесте пользователя
        User dbUser = userRepository.findByEmail("test_test").get();

        mockMvc.perform(get("/projects")).andExpect(status().isOk());
        mockMvc.perform(post("/api/projects/?projectName=test")).andExpect(status().isOk());

        projId = projectRepository.findAll()
                .stream()
                .filter(p -> p.owners.contains(dbUser))
                .findFirst()
                .get().id;
        System.out.println("projId = " + projId);

        mockMvc.perform(get("/projects/" + projId + "/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projects/" + projId + "/history"))
                .andExpect(status().isOk());

        // Далее идёт проверка самых важных функций api
        mockMvc.perform(put("/api/projects/" + projId + "/files/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new Genson().serialize(fileList)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/" + projId + "/compile/?targetFilepath=/test1.tex")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new Genson().serialize(fileList)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/projects/" + projId + "/files/?path=/test1.tex"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/" + projId + "/clear-aux"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/" + projId + "/remove-owner?email=test_test"))
                .andExpect(status().isOk());
    }

    /*
    @Test
    @WithMockUser(value = "test_test_test")
    TODO: void adminTest() throws Exception {
    }
    */

    @AfterAll
    static void cleanUp()
    {
        // Очистка файловой системы от созданных файлов тестового проекта
        if (projId != null) {
            try (Stream<Path> paths = Files.walk(Paths.get("projects/" + projId))) {
                paths.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Вспомогательная функция для работы с формами
    private String buildUrlEncodedFormEntity(String... params) {
        if ((params.length % 2) > 0) {
            throw new IllegalArgumentException("Need to give an even number of parameters");
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < params.length; i += 2) {
            if (i > 0) {
                result.append('&');
            }
            try {
                result.
                        append(URLEncoder.encode(params[i], StandardCharsets.UTF_8.name())).
                        append('=').
                        append(URLEncoder.encode(params[i + 1], StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return result.toString();
    }

}