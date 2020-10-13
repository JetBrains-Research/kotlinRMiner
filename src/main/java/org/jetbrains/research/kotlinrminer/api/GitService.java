package org.jetbrains.research.kotlinrminer.api;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

public class GitService {
    public Repository openRepository(String repositoryPath) throws Exception {
        File folder = new File(repositoryPath);
        Repository repository;
        if (folder.exists()) {
            RepositoryBuilder builder = new RepositoryBuilder();
            repository = builder
                .setGitDir(new File(folder, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
        } else {
            throw new FileNotFoundException(repositoryPath);
        }
        return repository;
    }

    public void fileTreeDiff(Repository repository,
                             RevCommit currentCommit,
                             List<String> filesBefore,
                             List<String> filesCurrent,
                             Map<String, String> renamedFilesHint) throws Exception {
        if (currentCommit.getParentCount() > 0) {
            ObjectId oldTree = currentCommit.getParent(0).getTree();
            ObjectId newTree = currentCommit.getTree();
            final TreeWalk tw = new TreeWalk(repository);
            tw.setRecursive(true);
            tw.addTree(oldTree);
            tw.addTree(newTree);

            final RenameDetector rd = new RenameDetector(repository);
            rd.setRenameScore(80);
            rd.addAll(DiffEntry.scan(tw));

            for (DiffEntry diff : rd.compute(tw.getObjectReader(), null)) {
                DiffEntry.ChangeType changeType = diff.getChangeType();
                String oldPath = diff.getOldPath();
                String newPath = diff.getNewPath();
                if (changeType != DiffEntry.ChangeType.ADD) {
                    if (isKotlinFile(oldPath)) {
                        filesBefore.add(oldPath);
                    }
                }
                if (changeType != DiffEntry.ChangeType.DELETE) {
                    if (isKotlinFile(newPath)) {
                        filesCurrent.add(newPath);
                    }
                }
                if (changeType == DiffEntry.ChangeType.RENAME && diff.getScore() >= rd.getRenameScore()) {
                    if (isKotlinFile(oldPath) && isKotlinFile(newPath)) {
                        renamedFilesHint.put(oldPath, newPath);
                    }
                }
            }
        }
    }

    public Repository cloneIfNotExists(String projectPath, String cloneUrl) throws Exception {
        File folder = new File(projectPath);
        Repository repository;
        if (folder.exists()) {
            RepositoryBuilder builder = new RepositoryBuilder();
            repository = builder
                .setGitDir(new File(folder, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();

        } else {
            Git git = Git.cloneRepository()
                .setDirectory(folder)
                .setURI(cloneUrl)
                .setCloneAllBranches(true)
                .call();
            repository = git.getRepository();
        }
        return repository;
    }

    private boolean isKotlinFile(String path) {
        return path.endsWith(".kt");
    }

}
