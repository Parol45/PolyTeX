package ru.test.restservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FileItemDTO {
    public String name;
    public String type;
    public String path;
    public List<String> content;
}
