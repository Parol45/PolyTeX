package ru.test.restservice.service;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Service;
import ru.test.restservice.dto.CommitDTO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GitService {

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
                if (fileService.isTextFile(name)) {
                    files.add(new CommitDTO.File(treeWalk.getObjectId(0).name(), "/" + name));
                }
            }
            result.add(new CommitDTO(String.valueOf(commit.getCommitTime()), new Date(commit.getCommitTime() * 1000L),
                    commit.getAuthorIdent().getName(), commit.getName(), files));
        }
        return result;
    }

}
