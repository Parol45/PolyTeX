package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Service;
import ru.test.restservice.dao.ProjectRepository;
import ru.test.restservice.dto.CommitDTO;
import ru.test.restservice.dto.FileItemDTO;
import ru.test.restservice.entity.Project;
import ru.test.restservice.exceptions.NotFoundException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static ru.test.restservice.utils.FileUtils.isTextFile;

@Service
@RequiredArgsConstructor
public class GitService {

    private final ProjectRepository projectRepository;
    /**
     * Метод, создающий новый git репозиторий
     *
     * @param path путь к директории для репозитория
     */
    public void initRepository(String path) throws GitAPIException {
        Git git = Git.init().setDirectory(Paths.get(path).toFile()).call();
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").setAuthor("System", "System").call();
    }
    /**
     * Удаление файла из индексирования git
     *
     * @param path путь к git репозиторию
     * @param pattern маска имени файла
     */
    public void removeFile(String path, String pattern) throws IOException, GitAPIException {
        Git git = Git.open(new File(path + "/.git"));
        git.rm().addFilepattern(pattern).call();
    }
    /**
     * Коммит
     */
    public void commit(String path, String message, String username) throws IOException, GitAPIException {
        Git git = Git.open(new File(path + "/.git"));
        git.add().addFilepattern(".").call();
        git.commit().setMessage(message).setAuthor(username, username).call();
    }
    /**
     * Список всех коммитов в репозитории
     *
     * @param path путь к репозиторию
     */
    public List<CommitDTO> getCommitList(String path) throws IOException, GitAPIException {
        List<CommitDTO> result = new ArrayList<>();
        Git git = Git.open(new File(path + "/.git"));
        Repository repository = git.getRepository();
        Iterable<RevCommit> commits = git.log().all().call();
        for (RevCommit commit : commits) {
            List<CommitDTO.File> files = new ArrayList<>();
            RevTree tree = commit.getTree();
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String name = treeWalk.getPathString();
                if (isTextFile(name)) {
                    files.add(new CommitDTO.File(treeWalk.getObjectId(0).name(), "/" + name));
                }
            }
            String commitId = commit.getId().toString(),
                   commitAuthor = commit.getAuthorIdent().getName(),
                   commitTitle = commit.getName();
            LocalDateTime commitDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(commit.getCommitTime() * 1000L), ZoneId.systemDefault());
            //TODO: формат даты
            result.add(new CommitDTO(commitId, commitDate.toString(), commitAuthor, commitTitle, files));
        }
        return result;
    }
    /**
     * Чтение содержимого файла по id
     */
    public String getFileContents(String fileId, Repository repository) throws IOException {
        ObjectId objectId = ObjectId.fromString(fileId);
        ObjectLoader loader = repository.open(objectId);
        InputStream in = loader.openStream();
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (in, StandardCharsets.UTF_8.name()))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        return textBuilder.toString();
    }
    /**
     * Возврат содержимого файла по его id
     */
    public String getCommitFile(UUID projectId, String fileId) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        Git git = Git.open(new File(project.path + "/.git"));
        Repository repository = git.getRepository();
        return getFileContents(fileId, repository);
    }
    /**
     * Метод, возвращающий файл к состоянию одной из прошлых контрольных точек
     */
    public List<FileItemDTO> rollback(UUID projectId, CommitDTO.File file, String commitDate, String username) throws IOException, GitAPIException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        Git git = Git.open(new File(project.path + "/.git"));
        Repository repository = git.getRepository();
        commit(project.path, "Save before rollback to " + commitDate, username);
        Path fileToRollback = Paths.get(project.path + file.path);
        String contents = getFileContents(file.id, repository);
        if (!Files.exists(fileToRollback)) {
            Files.createDirectories(fileToRollback.getParent());
        } else {
            Files.delete(fileToRollback);
        }
        FileItemDTO newFile = new FileItemDTO(file.path, "txt", file.path, Arrays.asList(contents.split("\n")));
        return Collections.singletonList(newFile);
    }

}
