package ru.test.restservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Класс для возврата информации о результате компиляции
 */
@Data
@AllArgsConstructor
public class CompilationResultDTO {
    public String latexMessage;
    public String biberMessage;
    public String pathToPdf;
}
