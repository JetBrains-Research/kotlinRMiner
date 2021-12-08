package org.jetbrains.research.kotlinrminer.cli;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GitService {
    private static final String REMOTE_REFS_PREFIX = "refs/remotes/origin/";

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

    public RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception {
        List<ObjectId> currentRemoteRefs = new ArrayList<>();
        for (Ref ref : repository.getRefDatabase().getRefs()) {
            String refName = ref.getName();
            if (refName.startsWith(REMOTE_REFS_PREFIX)) {
                if (branch == null || refName.endsWith("/" + branch)) {
                    currentRemoteRefs.add(ref.getObjectId());
                }
            }
        }

        RevWalk walk = new RevWalk(repository);
        for (ObjectId newRef : currentRemoteRefs) {
            walk.markStart(walk.parseCommit(newRef));
        }
        //walk.setRevFilter(commitsFilter);
        return walk;
    }

    public Iterable<RevCommit> createRevsWalkBetweenCommits(Repository repository,
                                                            String startCommitId,
                                                            String endCommitId)
        throws Exception {
        ObjectId from = repository.resolve(startCommitId);
        ObjectId to = repository.resolve(endCommitId);
        try (Git git = new Git(repository)) {
            List<RevCommit> revCommits = StreamSupport.stream(git.log().addRange(from, to).call()
                    .spliterator(), false)
                .filter(r -> r.getParentCount() == 1)
                .collect(Collectors.toList());
            Collections.reverse(revCommits);
            return revCommits;
        }
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
