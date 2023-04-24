package org.jetbrains.research.kotlinrminer.cli;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.research.kotlinrminer.cli.uml.UMLModel;
import org.jetbrains.research.kotlinrminer.common.RefactoringType;

import java.io.File;
import java.io.StringWriter;
import java.util.*;

public class GitHistoryKotlinRMiner {
    private static final String GITHUB_URL = "https://github.com/";
    private static final String BITBUCKET_URL = "https://bitbucket.org/";
    private final RefactoringType[] refactoringTypesToConsider = RefactoringType.ALL;

    protected List<Refactoring> detectRefactorings(@Nullable Project project,
                                                   GitService gitService,
                                                   Repository repository,
                                                   RevCommit currentCommit,
                                                   RefactoringHandler handler) throws Exception {
        List<Refactoring> refactoringsAtRevision;
        String commitId = currentCommit.getId().getName();
        List<String> filePathsBefore = new ArrayList<>();
        List<String> filePathsCurrent = new ArrayList<>();
        Map<String, String> renamedFilesHint = new HashMap<>();
        gitService.fileTreeDiff(repository, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);

        Set<String> repositoryDirectoriesBefore = new LinkedHashSet<>();
        Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<>();
        Map<String, String> fileContentsBefore = new LinkedHashMap<>();
        Map<String, String> fileContentsCurrent = new LinkedHashMap<>();
        try (RevWalk walk = new RevWalk(repository)) {
            // If no kt files changed, there is no refactoring. Also, if there are
            // only ADD's or only REMOVE's there is no refactoring
            boolean ktFilesChanged = !filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty();
            if (ktFilesChanged && currentCommit.getParentCount() > 0) {
                RevCommit parentCommit = currentCommit.getParent(0);
                populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore,
                    repositoryDirectoriesBefore);
                UMLModel parentUMLModel = createModelInKotlin(project, fileContentsBefore, repositoryDirectoriesBefore);

                populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent,
                    repositoryDirectoriesCurrent);
                UMLModel currentUMLModel = createModelInKotlin(project, fileContentsCurrent, repositoryDirectoriesCurrent);

                refactoringsAtRevision = parentUMLModel.diff(currentUMLModel, renamedFilesHint).getRefactorings();
                refactoringsAtRevision = filter(refactoringsAtRevision);
            } else {
                refactoringsAtRevision = Collections.emptyList();
            }
            handler.handle(commitId, refactoringsAtRevision, ktFilesChanged);

            walk.dispose();
        }
        return refactoringsAtRevision;
    }

    private void detect(GitService gitService,
                        Repository repository,
                        final RefactoringHandler handler,
                        Iterator<RevCommit> i) {
        int commitsCount = 0;
        int errorCommitsCount = 0;
        int refactoringsCount = 0;

        File metadataFolder = repository.getDirectory();
        File projectFolder = metadataFolder.getParentFile();
        String projectName = projectFolder.getName();

        long time = System.currentTimeMillis();
        while (i.hasNext()) {
            RevCommit currentCommit = i.next();
            try {
                List<Refactoring> refactoringsAtRevision =
                    detectRefactorings(null, gitService, repository, currentCommit, handler);
                refactoringsCount += refactoringsAtRevision.size();

            } catch (Exception e) {
                handler.handleException(currentCommit.getId().getName(), e);
                errorCommitsCount++;
            }

            commitsCount++;
            long time2 = System.currentTimeMillis();
            if ((time2 - time) > 20000) {
                time = time2;
            }
        }

        handler.onFinish(refactoringsCount, commitsCount, errorCommitsCount);
        System.out.printf("Analyzed %s [Commits: %d, Errors: %d, Refactorings: %d]%n", projectName,
            commitsCount, errorCommitsCount, refactoringsCount);
    }

    public void detectAtCommit(@Nullable Project project, Repository repository, String commitId, RefactoringHandler handler) {
        String cloneURL = repository.getConfig().getString("remote", "origin", "url");
        File metadataFolder = repository.getDirectory();
        File projectFolder = metadataFolder.getParentFile();
        GitService gitService = new GitService();
        RevWalk walk = new RevWalk(repository);
        try {
            RevCommit commit = walk.parseCommit(repository.resolve(commitId));
            if (commit.getParentCount() > 0) {
                walk.parseCommit(commit.getParent(0));
                this.detectRefactorings(project, gitService, repository, commit, handler);
            }
        } catch (Exception e) {
            handler.handleException(commitId, e);
        } finally {
            walk.close();
            walk.dispose();
        }
    }

    public void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler) {
        detectAtCommit(null, repository, commitId, handler);
    }

    public void detectAll(Repository repository, String branch, final RefactoringHandler handler) throws Exception {
        GitService gitService = new GitService();
        RevWalk walk = gitService.createAllRevsWalk(repository, branch);
        try {
            detect(gitService, repository, handler, walk.iterator());
        } finally {
            walk.dispose();
        }
    }

    public void detectBetweenCommits(Repository repository, String startCommitId, String endCommitId,
                                     RefactoringHandler handler) throws Exception {
        GitService gitService = new GitService();
        Iterable<RevCommit> walk = gitService.createRevsWalkBetweenCommits(repository, startCommitId, endCommitId);
        detect(gitService, repository, handler, walk.iterator());
    }

    protected List<Refactoring> filter(List<Refactoring> refactoringsAtRevision) {
        if (this.refactoringTypesToConsider == null) {
            return refactoringsAtRevision;
        }
        /*  TODO: perform filtration
            if (this.refactoringTypesToConsider.contains(ref.getRefactoringType())) {
            }*/
        return new ArrayList<>(refactoringsAtRevision);
    }

    protected UMLModel createModelInKotlin(@Nullable Project project, Map<String, String> fileContents, Set<String> repositoryDirectories) throws
        Exception {
        UMLModelPsiReaderCli psiReader = new UMLModelPsiReaderCli(repositoryDirectories);
        psiReader.parseFiles(fileContents);
        return psiReader.getUmlModel();
    }

    private void populateFileContents(Repository repository,
                                      RevCommit commit,
                                      List<String> filePaths,
                                      Map<String, String> fileContents,
                                      Set<String> repositoryDirectories) throws Exception {
        RevTree parentTree = commit.getTree();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(parentTree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String pathString = treeWalk.getPathString();
                if (filePaths.contains(pathString)) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(loader.openStream(), writer);
                    fileContents.put(pathString, writer.toString());
                }
                if (pathString.endsWith(".kt") && pathString.contains("/")) {
                    String directory = pathString.substring(0, pathString.lastIndexOf("/"));
                    repositoryDirectories.add(directory);
                    //include sub-directories
                    String subDirectory = directory;
                    while (subDirectory.contains("/")) {
                        subDirectory = subDirectory.substring(0, subDirectory.lastIndexOf("/"));
                        repositoryDirectories.add(subDirectory);
                    }
                }
            }
        }
    }

    public static String extractCommitURL(String cloneURL, String commitId) {
        int indexOfDotGit = cloneURL.length();
        if (cloneURL.endsWith(".git")) {
            indexOfDotGit = cloneURL.indexOf(".git");
        } else if (cloneURL.endsWith("/")) {
            indexOfDotGit = cloneURL.length() - 1;
        }
        String commitResource = "/";
        if (cloneURL.startsWith(GITHUB_URL)) {
            commitResource = "/commit/";
        } else if (cloneURL.startsWith(BITBUCKET_URL)) {
            commitResource = "/commits/";
        }
        return cloneURL.substring(0, indexOfDotGit) + commitResource + commitId;
    }

}
