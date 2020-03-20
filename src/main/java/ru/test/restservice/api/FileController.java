package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.service.FileService;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * Приём файла с фронта и проверка его расширения на допустимость
     */
    @PostMapping(value = "/upload", headers = "content-type=multipart/*")
    public FileItemDTO handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        return fileService.save(file);
    }

    /**
     * Возврат рабочих файлов пользователю через объект
     *
     * @return Список объектов-файлов: имя, тип и содержимое
     */
    @GetMapping("/files")
    public List<FileItemDTO> returnFileList() throws IOException {
        return fileService.listFiles("test");
    }

    /**
     * Перезапись содержимого файлов
     */
    @PutMapping("/files")
    public void saveFiles(@RequestBody List<FileItemDTO> files) throws IOException {
        fileService.rewriteFiles(files);
    }

    @DeleteMapping("/files")
    public void deleteFile(@RequestParam String path) throws IOException {
        fileService.deleteFile(path);
    }
}
