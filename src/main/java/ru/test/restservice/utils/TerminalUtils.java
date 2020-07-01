package ru.test.restservice.utils;

import ru.test.restservice.exceptions.CompilationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class TerminalUtils {
    public static String executeTerminalCommand(String command, String pathToFile) {
        StringBuilder messageBuilder = new StringBuilder();
        try {
            ProcessBuilder pBuilder = new ProcessBuilder();
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows) {
                pBuilder.command("cmd.exe", "/c", command);
            } else {
                pBuilder.command("sh", "-c", command);
            }
            pBuilder.directory(new File(pathToFile));
            Process proc = pBuilder.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                messageBuilder.append(line);
            }
            proc.waitFor();
            in.close();
        } catch (Exception e) {
            throw new CompilationException(e.getMessage());
        }
        return messageBuilder.toString();
    }
}
