package ru.test.restservice.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dto.FileItemDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public static class FileException extends RuntimeException {
        public FileException(String message) {
            super(message);
        }
    }

    /**
     * Функция, возвращающая список файлов, с которыми пользователь может работать
     *
     * @param folder рабочая директория пользователя
     * @return Список объектов с информацией о файлах
     */
    public static ArrayList<FileItemDTO> listFiles(String folder) throws IOException {
        ArrayList<FileItemDTO> result = new ArrayList<>();
        List<Path> files = Files.list(Paths.get(folder)).filter(
                (path) -> isAllowedFile(path.toString())).collect(Collectors.toList());
        for (Path file : files) {
            String filename = file.getFileName().toString();
            if (isTextFile(file.toString())) {
                result.add(new FileItemDTO(filename, "txt", Files.readAllLines(file)));
            } else {
                result.add(new FileItemDTO(filename, "pic", Collections.singletonList(file.toString())));
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
    public static FileItemDTO saveAndReturnFile(MultipartFile file) throws IOException {
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
}
