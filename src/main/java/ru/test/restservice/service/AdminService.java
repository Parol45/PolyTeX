package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.MainApplication;
import ru.test.restservice.config.AdminProperties;
import ru.test.restservice.dao.TemplateRepository;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.entity.Template;
import ru.test.restservice.entity.User;
import ru.test.restservice.exceptions.GenericException;
import ru.test.restservice.exceptions.NotFoundException;
import ru.test.restservice.utils.FileUtils;
import ru.test.restservice.utils.Setting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminProperties adminProperties;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    /**
     * Метод, добавляющий в БД информацию о новом пользователе
     *
     * @param email имя пользователя
     * @param password пароль пользователя
     * @return новый созданный объект-сущность
     */
    // TODO: обрабатывать нарушение уник индекса
    public User register(String email, String password) {
        User newUser = new User(email, new BCryptPasswordEncoder(4).encode(password), "ROLE_USER", false);
        userRepository.save(newUser);
        logService.log(email, String.format("User %s has been registered", newUser.email));
        return newUser;
    }

    /**
     * Метод, возвращающий системные настройки из объекта adminProperties
     *
     * @return новый созданный объект-сущность
     */
    public Map<String, String> returnSettings() throws IOException {
        HashMap<String, String> settings = new HashMap<>();
        ArrayList<String> configLines = (ArrayList<String>) Files.readAllLines(Paths.get("config/application.yml"));
        String maxUploadFileSize = configLines
                .stream()
                .filter(line -> line.matches(".*max-file-size:.*"))
                .findFirst()
                .get();
        Pattern pattern = Pattern.compile("max-file-size: ([0-9]+)MB");
        Matcher matcher = pattern.matcher(maxUploadFileSize);
        if (matcher.find()) {
            settings.put("maxUploadFileSize", matcher.group(1));
            settings.put("idleTimeout", Integer.toString(adminProperties.idleTimeout));
            settings.put("maxProjectCount", Integer.toString(adminProperties.maxProjectCount));
            settings.put("compilationInterval", Integer.toString(adminProperties.compilationInterval));
            settings.put("maxPathLength", Integer.toString(adminProperties.maxPathLength));
            return settings;
        } else {
            throw new GenericException("maxUploadFileSize is not found");
        }
    }

    /**
     * Установка новых настроек в контекст приложения
     *
     * @param settings НАСТРОЙКИ
     */
    public void setNewSettings(List<Setting> settings) throws IOException {
        ArrayList<String> configLines = (ArrayList<String>) Files.readAllLines(Paths.get("config/application.yml"));
        boolean needReload = false;
        for (Setting set : settings) {
            if (set.property.equals("maxUploadFileSize")) {
                rewriteConfigLines(configLines, ".*max-file-size:.*", "max-file-size: [0-9]+MB", "max-file-size: " + set.value + "MB");
                rewriteConfigLines(configLines, ".*max-request-size:.*", "max-request-size: [0-9]+MB", "max-request-size: " + set.value + "MB");
                needReload = true;
            } else {
                switch (set.property) {
                    case "idleTimeout":
                        rewriteConfigLines(configLines, ".*idle-timeout:.*", "idle-timeout: [0-9]+", "idle-timeout: " + set.value);
                        adminProperties.idleTimeout = Integer.parseInt(set.value);
                        break;
                    case "maxProjectCount":
                        rewriteConfigLines(configLines, ".*max-project-count:.*", "max-project-count: [0-9]+", "max-project-count: " + set.value);
                        adminProperties.maxProjectCount = Integer.parseInt(set.value);
                        break;
                    case "compilationInterval":
                        rewriteConfigLines(configLines, ".*compilation-interval:.*", "compilation-interval: [0-9]+", "compilation-interval: " + set.value);
                        adminProperties.compilationInterval = Integer.parseInt(set.value);
                        break;
                    case "maxPathLength":
                        rewriteConfigLines(configLines, ".*max-path-length:.*", "max-path-length: [0-9]+", "max-path-length: " + set.value);
                        adminProperties.maxPathLength = Integer.parseInt(set.value);
                        break;
                }
            }
        }
        if (needReload) {
            MainApplication.restart();
        }
        Files.write(Paths.get("config/application.yml"), configLines);
    }

    /**
     * Вынесенный метод изменения строк конфиг-файлов
     */
    private void rewriteConfigLines(List<String> configLines, String regexWhich, String regexFrom, String regexTo) {
        for (int i = 0; i < configLines.size(); i++) {
            if (configLines.get(i).matches(regexWhich)) {
                configLines.set(i, configLines.get(i).replaceAll(regexFrom, regexTo));
            }
        }
    }

    /**
     * Вроде очевидно:
     *
     * - добавление/удаление прав админа по id
     * - блокирование/разблокирование пользователя по id
     */
    public void grantAdmin(UUID id) {
        User user = userRepository.findById(id).orElseThrow(NotFoundException::new);
        user.role = "ROLE_ADMIN";
        userRepository.save(user);
    }

    public void revokeAdmin(UUID id) {
        User user = userRepository.findById(id).orElseThrow(NotFoundException::new);
        user.role = "ROLE_USER";
        userRepository.save(user);
    }

    public void blockUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(NotFoundException::new);
        user.banned = true;
        userRepository.save(user);
    }

    public void unblockUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(NotFoundException::new);
        user.banned = false;
        userRepository.save(user);
    }

    /**
     * Добавление информации о новом шаблоне в БД и сохранение на диске
     *
     * @param templateName имя шаблона
     * @param file zip-архив с файлами шаблона
     * @param templateDescription описание шаблона
     * @return новый созданный объект-сущность
     */
    public Template createTemplate(String templateName, MultipartFile file, String templateDescription) throws IOException {
        UUID newId = UUID.randomUUID();
        Template temp = new Template(newId, templateName, templateDescription);
        templateRepository.save(temp);
        Path zip = Paths.get(String.format("templates/%s/%s", newId.toString(), file.getOriginalFilename()));
        Files.createDirectories(zip.getParent());
        file.transferTo(zip);
        FileUtils.unpackZip(zip);
        return temp;
    }

    /**
     * Удаление шаблона по его id
     */
    public void deleteTemplate(UUID templateId) {
        Template temp = templateRepository.findById(templateId).orElseThrow(NotFoundException::new);
        FileUtils.deleteDir(Paths.get("templates/" + temp.id));
        templateRepository.delete(temp);
    }
}
