package ru.test.restservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class FileException extends RuntimeException {
    public FileException(String message) {
        super(message);
    }
}