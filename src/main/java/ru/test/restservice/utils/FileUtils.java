package ru.test.restservice.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {

    // Список разрешённых файловых расширений
    public final static String TEXT_FILE_REGEX = ".+\\.(bib|tex)$";
    public final static String PIC_FILE_REGEX = ".+\\.(svg|jpg|png)$";

    public final static String AUX_FILE_REGEX = ".+\\.(aux|bbl|bcf|blg|log|out|pdf|run.xml|synctex.gz|toc|idx)$";

    public static boolean isTextFile(String name) {
        return name.matches(TEXT_FILE_REGEX);
    }

    public static boolean isPicFile(String name) {
        return name.matches(PIC_FILE_REGEX);
    }

    public static boolean isAllowedFile(String name) {
        return isTextFile(name) || isPicFile(name);
    }

    public static boolean isAuxFile(String name) {
        return name.matches(AUX_FILE_REGEX);
    }

    public static void deleteAux(Path file) {
        if (isAuxFile(file.toString())) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteDir(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод, распаковывающий zip-архив в ту же директорию
     *
     * @param zip - путь до zip-архива
     */
    public static void unpackZip(Path zip) {
        try {
            ZipFile zipFile = new ZipFile(zip.toString());
            zipFile.extractAll(zip.getParent().toString());
            Files.delete(zip);
            ArrayList<Path> subfolders;
            try (Stream<Path> files = Files.list(zip.getParent())) {
                subfolders = files.collect(Collectors.toCollection(ArrayList::new));
            }
            if (subfolders.size() == 1 && Files.isDirectory(subfolders.get(0))) {
                try (Stream<Path> files = Files.list(subfolders.get(0))) {
                    files.forEach(f -> f.toFile().renameTo(new File(zip.getParent().toString() + "/" + f.getFileName().toString())));
                }
                Files.deleteIfExists(subfolders.get(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод, архивирующий папку с проетком
     *
     * @param projectId - id архивируемого проекта
     */
    public static void packZip(UUID projectId) throws IOException {
        Path projectPath = Paths.get("projects/" + projectId);
        ZipFile zipFile = new ZipFile(String.format("projects/%s/%s.zip", projectId, projectId));
        try (Stream<Path> files = Files.list(projectPath)) {
            files.forEach(f -> {
                try {
                    if (Files.isDirectory(f) && !f.toString().matches(".*" + projectId.toString() + "([\\\\/]\\.git.*)?")) {
                        zipFile.addFolder(f.toFile());
                    } else if (isAllowedFile(f.getFileName().toString())) {
                        zipFile.addFile(f.toFile());
                    }
                } catch (ZipException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Копирование всех файлов из одной директории в другую
     */
    public static void copyContentTo(String from, String to) {
        String regex = "^" + Paths.get(from).toString().replace("\\", "\\\\") + "[\\\\/]?";
        try (Stream<Path> paths = Files.walk(Paths.get(from))) {
            paths
                    .forEach(f -> {
                        try {
                            if (!Files.isDirectory(f)) {
                                String relativePath = to + "/" + f.toString().replaceAll(regex, "");
                                Files.createDirectories(Paths.get(relativePath).getParent());
                                Files.copy(f, Paths.get(relativePath));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаление папки с проектом
     * (чтобы не возникало разных ошибок, связанных с незакрытыми стримами, удалять этим методом)
     */
    public static void deleteProjectFromDisk(UUID projectId) {
        if (projectId != null) {
            try (Stream<Path> paths = Files.walk(Paths.get("projects/" + projectId))) {
                paths.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
