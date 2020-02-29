package ru.test.restservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

// Класс для возврата информации о результате компиляции
@Data
@AllArgsConstructor
public class CompilationResult {
    public String message;
    public String path;
}
