package ru.test.restservice.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {

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
}
