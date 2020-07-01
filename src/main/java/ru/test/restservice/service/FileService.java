package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.config.AdminProperties;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.exceptions.FileException;
import ru.test.restservice.exceptions.GenericException;
import ru.test.restservice.exceptions.NotFoundException;
import ru.test.restservice.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static ru.test.restservice.utils.FileUtils.*;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ProjectRepository projectRepository;
    private final GitService gitService;
    private final AdminProperties adminProperties;

    /**
     * Функция, возвращающая список файлов, с которыми пользователь может работать
     *
     * @return Список объектов с информацией о файлах
     */

    public List<FileItemDTO> listFiles(UUID projectId) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        ArrayList<FileItemDTO> result = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(project.path))) {
            paths
                    .filter(path -> !path.toString().matches(".*" + projectId.toString() + "([\\\\/]\\.git.*)?"))
                    .forEach(file -> {
                        String filepath = file.toString().replace("\\", "/");
                        filepath = filepath.replaceAll("^" + project.path, "");
                        if (Files.isDirectory(file)) {
                            result.add(new FileItemDTO(file.getFileName().toString(), "dir", filepath, new ArrayList<>()));
                        } else {
                            if (isTextFile(filepath)) {
                                result.add(new FileItemDTO(file.getFileName().toString(), "txt", filepath, null));
                            } else if (isPicFile(filepath)) {
                                result.add(new FileItemDTO(file.getFileName().toString(), "pic", filepath, Collections.singletonList("projects/" + project.id + filepath)));
                            }
                        }
                    });
        }
        return result;
    }

    /**
     * Сохраняет и возвращает информацию о файле
     *
     * @param file файл с фронтенда
     * @return Объект с информацией о файле
     */
    public FileItemDTO save(MultipartFile file, String relativePath, UUID projectId) throws IOException {
        String filename, fileType;
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        if (relativePath.length() > adminProperties.maxPathLength) {
            throw new GenericException("Имя файла слишком длинное");
        }
        List<String> content;
        // Чтобы не было NullPointerException
        if (file.getOriginalFilename() != null) {
            filename = file.getOriginalFilename();
            fileType = isTextFile(filename.toLowerCase()) ? "txt" : "pic";
        } else {
            throw new FileException("Wtf have u passed as file?");
        }
        Path path = Paths.get(project.path + relativePath);
        // Проверяю и на фронте, и на бэке разрешён ли файл (на фронте пользователь может удалить метод)
        if (isAllowedFile(filename)) {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(path, file.getBytes());
        } else {
            throw new FileException("Wrong file type");
        }
        // Если файл текстовый, то передаю его содержимое, иначе - путь до него
        if (fileType.equals("txt")) {
            content = Files.readAllLines(path);
        } else {
            content = Collections.singletonList(path.toString().replace("\\", "/"));
        }
        return new FileItemDTO(filename, fileType, path.toString().replace("\\", "/"), content);
    }

    /**
     * Перезапись содержимого файлов
     */
    public void rewriteFiles(List<FileItemDTO> files, UUID projectId) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        for (FileItemDTO file : files) {
            if (file.path.length() > adminProperties.maxPathLength) {
                throw new GenericException("Имя файла слишком длинное");
            }
            Path path = Paths.get(project.path + file.path);
            if (file.type.equals("dir")) {
                Files.createDirectory(path);
            } else {
                Files.write(path, file.content);
            }
        }
    }

    public void deleteFile(String path, UUID projectId) throws IOException, GitAPIException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        gitService.removeFile(project.path, path.substring(1));
        Path fileToDeletePath = Paths.get(project.path + path);
        if (Files.isDirectory(fileToDeletePath)) {
            FileSystemUtils.deleteRecursively(fileToDeletePath);
        } else {
            Files.deleteIfExists(fileToDeletePath);
        }
        if (path.endsWith(".tex")) {
            String pureFileName = fileToDeletePath.getFileName().toString().replaceAll("\\.tex$", "");
            Files.list(fileToDeletePath.getParent()).forEach(f -> {
                try {
                    if (Files.isRegularFile(f) && f.getFileName().toString().matches(pureFileName + AUX_FILE_REGEX)) {
                        Files.deleteIfExists(f);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Метод, удаляющий aux файлы в проекте
     */
    public void clearAuxFiles(UUID projectId) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        Path projectDest = Paths.get(project.path);
        try (Stream<Path> paths = Files.walk(projectDest)) {
            paths
                    .filter(path -> Files.isRegularFile(path))
                    .forEach(FileUtils::deleteAux);
        }
    }
}
