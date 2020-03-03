package ru.test.restservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Класс для возврата информации о содержащихся документах в рабочей папке
  */
// TODO: возвращать пути к файлам и подгружать по запросу с фронта
@Data
@AllArgsConstructor
public class FileItemDTO {
    public String fileName;
    public String fileType;
    // Содержимое файла (список строк) - сменить на filePath
    public List<String> content;
}
