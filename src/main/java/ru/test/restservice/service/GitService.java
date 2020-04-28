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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static ru.test.restservice.utils.FileUtils.isTextFile;

@Service
@RequiredArgsConstructor
public class GitService {

    // TODO: откат
    private final ProjectRepository projectRepository;
    private final FileService fileService;

    public void initRepository(String path) throws GitAPIException {

        Git git = Git.init().setDirectory(Paths.get(path).toFile()).call();
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").call();
    }

    public void commit(String path, String message) throws IOException, GitAPIException {
        Git git = Git.open(new File(path + "/.git"));
        git.add().addFilepattern(".").call();
        git.commit().setMessage(message).call();
    }

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
            } //String.valueOf(commit.getCommitTime())
            result.add(new CommitDTO(commit.getId().toString(), new Date(commit.getCommitTime() * 1000L),
                    commit.getAuthorIdent().getName(), commit.getName(), files));
        }
        return result;
    }

    public StringBuilder getFileContents(String fileId, Repository repository) throws IOException {
        ObjectId objectId = ObjectId.fromString(fileId);
        ObjectLoader loader = repository.open(objectId);
        InputStream in = loader.openStream();
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (in, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        return textBuilder;
    }

    public List<String> getCommitFilesList(UUID projectId, List<String> fileIds) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        Git git = Git.open(new File(project.path + "/.git"));
        Repository repository = git.getRepository();
        StringBuilder contents;
        List<String> result = new ArrayList<>();
        for (String fileId : fileIds) {
            contents = getFileContents(fileId, repository);
            result.add(contents.toString());
        }
        return result;
    }

    public void rollback(UUID projectId, CommitDTO.File file) throws IOException, GitAPIException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        Git git = Git.open(new File(project.path + "/.git"));
        Repository repository = git.getRepository();
        commit(project.path, "Save before rollback");
        Path fileToRollback = Paths.get(project.path + file.name);
        StringBuilder contents = getFileContents(file.id, repository);
        if (!Files.exists(fileToRollback)) {
            Files.createDirectories(fileToRollback.getParent());
        } else {
            Files.delete(fileToRollback);
        }
        FileItemDTO newFile = new FileItemDTO(file.name, "txt", file.name, Arrays.asList(contents.toString().split("\n")));
        fileService.rewriteFiles(Collections.singletonList(newFile), projectId);
    }

}
