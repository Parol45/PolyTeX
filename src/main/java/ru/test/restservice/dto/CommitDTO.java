package ru.test.restservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CommitDTO {
    String commitId;
    LocalDateTime commitTime;
    String committee;
    String commitMessage;
    List<File> files;

    @AllArgsConstructor
    public static class File {
        public String id;
        public String name;
    }
}
