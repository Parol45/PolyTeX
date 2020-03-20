package ru.test.restservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.exceptions.FileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Service
public class FileService {
    // Список разрешённых файловых расширений
    public final static String TEXT_FILE_REGEX = ".+\\.(bib|tex)$";
    public final static String PIC_FILE_REGEX = ".+\\.(svg|jpg|png)$";

    public static boolean isTextFile(String name) {
        return name.matches(TEXT_FILE_REGEX);
    }

    public static boolean isPicFile(String name) {
        return name.matches(PIC_FILE_REGEX);
    }

    public static boolean isAllowedFile(String name) {
        return isTextFile(name) || isPicFile(name);
    }

    /**
     * Функция, возвращающая список файлов, с которыми пользователь может работать
     *
     * @param folder рабочая директория пользователя
     * @return Список объектов с информацией о файлах
     */
    public List<FileItemDTO> listFiles(String folder) throws IOException {
        ArrayList<FileItemDTO> result = new ArrayList<>();
        Files
                .list(Paths.get(folder))
                .filter((path) -> isAllowedFile(path.toString()))
                .forEach((file) ->
                {
                    String filename = file.getFileName().toString();
                    if (isTextFile(file.toString())) {
                        try {
                            result.add(new FileItemDTO(filename, "txt", Files.readAllLines(file)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        result.add(new FileItemDTO(filename, "pic", Collections.singletonList(file.toString())));
                    }
                });
        return result;
    }

    /**
     * Сохраняет и возвращает информацию о файле
     *
     * @param file файл с фронтенда
     * @return Объект с информацией о файле
     */
    public FileItemDTO save(MultipartFile file) throws IOException {
        String filename, fileType;
        List<String> content;
        // Чтобы не было NullPointerException
        if (file.getOriginalFilename() != null) {
            filename = file.getOriginalFilename();
            fileType = isTextFile(filename.toLowerCase()) ? "txt" : "pic";
        } else {
            throw new FileException("Wtf have u passed as file?");
        }
        Path path = Paths.get("test/" + file.getOriginalFilename());
        // Проверяю и на фронте, и на бэке разрешён ли файл (на фронте пользователь может удалить метод)
        if (isAllowedFile(filename)) {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            // Пока что перезаписываю файл, если он уже существует, позже буду возвращать еррор код
            Files.write(path, file.getBytes());
        } else {
            throw new FileException("Wrong file type");
        }
        // Если файл текстовый, то передаю его содержимое, иначе - путь до него
        if (fileType.equals("txt")) {
            content = Files.readAllLines(path);
        } else {
            content = Collections.singletonList(path.toString());
        }
        return new FileItemDTO(filename, fileType, content);
    }

    /**
     * Перезапись содержимого файлов
     */
    public void rewriteFiles(List<FileItemDTO> files) throws IOException {
        for (FileItemDTO file : files) {
            Path path = Paths.get("test/" + file.name);
            Files.write(path, file.content);
        }
    }

    public void deleteFile(String path) throws IOException {
        Path fileToDeletePath = Paths.get("test/" + path);
        Files.deleteIfExists(fileToDeletePath);
        if (path.endsWith(".tex")) {
            String prefix = fileToDeletePath.getFileName().toString().replaceAll("\\.tex$", "");
            try (Stream<Path> paths = Files.walk(fileToDeletePath.getParent())) {
                paths
                        .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith(prefix))
                        .forEach(f ->
                        {
                            try {
                                Files.deleteIfExists(f);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        }
    }
}
