package ru.test.restservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class ProjectAccessException extends RuntimeException {
    public ProjectAccessException() {
        super("User is already working with this project");
    }
}
