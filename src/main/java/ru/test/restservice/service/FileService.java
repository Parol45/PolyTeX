package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.exceptions.FileException;
import ru.test.restservice.exceptions.NotFoundException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileService {
    // Список разрешённых файловых расширений
    public final static String TEXT_FILE_REGEX = ".+\\.(bib|tex)$";
    public final static String PIC_FILE_REGEX = ".+\\.(svg|jpg|png)$";
    public final static String AUX_FILE_REGEX = ".+\\.(aux|bbl|bcf|blg|log|out|pdf|run.xml|synctex.gz|toc)$";

    public boolean isTextFile(String name) {
        return name.matches(TEXT_FILE_REGEX);
    }

    public boolean isPicFile(String name) {
        return name.matches(PIC_FILE_REGEX);
    }

    public boolean isAllowedFile(String name) {
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

    public List<String> listCommitFiles(UUID projectId, List<String> fileIds) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        // TODO: создавать репозиторий напрямую
        Git git = Git.open(new File(project.path + "/.git"));
        Repository repository = git.getRepository();
        StringBuilder textBuilder;
        List<String> result = new ArrayList<>();
        for (String fileId : fileIds) {
            ObjectId objectId = ObjectId.fromString(fileId);
            ObjectLoader loader = repository.open(objectId);
            InputStream in = loader.openStream();
            textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader
                    (in, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            result.add(textBuilder.toString());
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
