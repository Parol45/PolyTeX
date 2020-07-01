package ru.test.restservice.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс представляющий всю информацию о файле
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileItemDTO {
    public String name;
    public String type;
    public String path;
    public List<String> content;
}
