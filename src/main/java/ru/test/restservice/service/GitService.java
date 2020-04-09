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
import ru.test.restservice.entity.Project;
import ru.test.restservice.exceptions.NotFoundException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static ru.test.restservice.utils.FileUtils.isTextFile;

@Service
@RequiredArgsConstructor
public class GitService {

    private final ProjectRepository projectRepository;

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
            }
            result.add(new CommitDTO(String.valueOf(commit.getCommitTime()), new Date(commit.getCommitTime() * 1000L),
                    commit.getAuthorIdent().getName(), commit.getName(), files));
        }
        return result;
    }

    public List<String> getCommitFilesList(UUID projectId, List<String> fileIds) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(NotFoundException::new);
        Git git = Git.open(new File(project.path + "/.git"));
        Repository repository = git.getRepository();
        StringBuilder textBuilder;
        List<String> result = new ArrayList<>();
        for (String fileId : fileIds) {
            ObjectId objectId = ObjectId.fromString(fileId);
            ObjectLoader loader = repository.open(objectId);
            InputStream in = loader.openStream();
            textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader
                    (in, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            result.add(textBuilder.toString());
        }
        return result;
    }

}
