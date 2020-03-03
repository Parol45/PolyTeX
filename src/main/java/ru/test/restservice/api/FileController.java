package ru.test.restservice.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dto.FileItemDTO;

import java.io.IOException;
import java.util.ArrayList;

import static ru.test.restservice.service.FileService.listFiles;
import static ru.test.restservice.service.FileService.saveAndReturnFile;

@RestController
@RequestMapping("/api")
public class FileController {
    /**
     * Приём файла с фронта и проверка его расширения на допустимость
     *
     * @param file файл, передаваемый с post-запросом
     * @return http код операции
     */
    @PostMapping(value = "/upload", headers = "content-type=multipart/*")
    public FileItemDTO handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        return saveAndReturnFile(file);
    }

    /**
     * Возврат рабочих файлов пользователю через объект
     *
     * @return Объект с тремя списками: имён, типов и содержимого
     */
    @GetMapping("/filelist")
    public ArrayList<FileItemDTO> returnFileList() throws IOException {
        return listFiles("test");
    }
}