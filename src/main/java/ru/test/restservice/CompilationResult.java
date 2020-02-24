package ru.test.restservice;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CompilationResult {
    public String message;
    public String path;
}
