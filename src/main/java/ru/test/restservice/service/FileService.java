package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.exceptions.FileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileService {
    // Список разрешённых файловых расширений
    public final static String TEXT_FILE_REGEX = ".+\\.(bib|tex)$";
    public final static String PIC_FILE_REGEX = ".+\\.(svg|jpg|png)$";
    public final static String AUX_FILE_REGEX = ".+\\.(aux|bbl|bcf|blg|log|out|pdf|run.xml|synctex.gz|toc)$";

    public static boolean isTextFile(String name) {
        return name.matches(TEXT_FILE_REGEX);
    }

    public static boolean isPicFile(String name) {
        return name.matches(PIC_FILE_REGEX);
    }

    public static boolean isAllowedFile(String name) {
        return isTextFile(name) || isPicFile(name);
    }

    private final ProjectRepository projectRepository;

    /**
     * Функция, возвращающая список файлов, с которыми пользователь может работать
     *
     * @return Список объектов с информацией о файлах
     */
    public List<FileItemDTO> listFiles(UUID projectId) throws IOException {
        ArrayList<FileItemDTO> result = new ArrayList<>();
        Stack<Path> files = new Stack<>();
        Project project = projectRepository.findById(projectId).get();
        Files.list(Paths.get(project.path)).forEach(files::add);
        while (!files.empty()) {
            Path file = files.pop();
            String filepath = file.toString().replace("\\", "/");
            filepath = filepath.replaceAll("^" + project.path, "");
            if (Files.isDirectory(file)) {
                Files
                        .list(file)
                        .forEach(files::push);
                result.add(new FileItemDTO(file.getFileName().toString(), "dir", filepath, new ArrayList<>()));
            } else {
                if (isTextFile(filepath)) {
                    try {
                        result.add(new FileItemDTO(file.getFileName().toString(), "txt", filepath, Files.readAllLines(file)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (isPicFile(filepath)) {
                    result.add(new FileItemDTO(file.getFileName().toString(), "pic", filepath, Collections.singletonList(project.id + filepath)));
                }
            }
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
        Project project = projectRepository.findById(projectId).get();
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
        Project project = projectRepository.findById(projectId).get();
        for (FileItemDTO file : files) {
            Path path = Paths.get(project.path + file.path);
            if (file.type.equals("dir")) {
                Files.createDirectory(path);
            } else {
                Files.write(path, file.content);
            }
        }
    }

    public void deleteFile(String path, UUID projectId) throws IOException {
        Project project = projectRepository.findById(projectId).get();
        Path fileToDeletePath = Paths.get(project.path + path);
        if (Files.isDirectory(fileToDeletePath)) {
            // TODO: не могу фиксануть, что не удаляет папки до окончания работы приложения
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
