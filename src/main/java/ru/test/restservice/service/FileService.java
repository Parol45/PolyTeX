package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.exceptions.FileException;
import ru.test.restservice.exceptions.NotFoundException;

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

    /**
     * Функция, возвращающая список файлов, с которыми пользователь может работать
     *
     * @return Список объектов с информацией о файлах
     */

    public List<FileItemDTO> listFiles(UUID projectId) throws IOException {
        ArrayList<FileItemDTO> result = new ArrayList<>();
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        try (Stream<Path> paths = Files.walk(Paths.get(project.path))) {
            paths
                    .filter(path -> !path.getFileName().toString().matches(projectId.toString() + "|\\.git"))
                    .forEach(file -> {
                        String filepath = file.toString().replace("\\", "/");
                        filepath = filepath.replaceAll("^" + project.path, "");
                        if (Files.isDirectory(file)) {
                            result.add(new FileItemDTO(file.getFileName().toString(), "dir", filepath, new ArrayList<>()));
                        } else {
                            if (isTextFile(filepath)) {
                                try {
                                    result.add(new FileItemDTO(file.getFileName().toString(), "txt", filepath, Files.readAllLines(file)));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
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
            Path path = Paths.get(project.path + file.path);
            if (file.type.equals("dir")) {
                Files.createDirectory(path);
            } else {
                Files.write(path, file.content);
            }
        }
    }

    // TODO: удалять файлы из git tree
    public void deleteFile(String path, UUID projectId) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
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


}
