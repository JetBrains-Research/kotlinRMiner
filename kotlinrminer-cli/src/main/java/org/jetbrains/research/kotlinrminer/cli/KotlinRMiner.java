package org.jetbrains.research.kotlinrminer.cli;

import org.eclipse.jgit.lib.Repository;
import org.jetbrains.research.kotlinrminer.cli.util.JsonUtil;

import java.util.List;


public class KotlinRMiner {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Please, specify the arguments\n");
            printUsage();
            return;
        }

        final String option = args[0];
        if (option.equalsIgnoreCase("-h") || option.equalsIgnoreCase("--h") ||
            option.equalsIgnoreCase("-help") || option.equalsIgnoreCase("--help")) {
            printUsage();
            return;
        }

        if (option.equalsIgnoreCase("-all")) {
            detectAll(args);
        } else if (option.equalsIgnoreCase("-c")) {
            detectAtCommit(args);
        } else if (option.equalsIgnoreCase("-bc")) {
            detectBetweenCommits(args);
        } else {
            System.out.println("Incorrect command. Please, use '-h' option for help.\n");
        }
    }

    /**
     * Detects refactorings at the specific commit.
     */
    private static void detectAtCommit(String[] args) throws Exception {
        String folder = args[1];
        String commitId = args[2];
        GitService gitService = new GitService();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryKotlinRMiner detector = new GitHistoryKotlinRMiner();
            StringBuilder sb = new StringBuilder();
            JsonUtil.startJSON(sb);
            detector.detectAtCommit(repo, commitId, new RefactoringHandler() {
                @Override
                public void handle(String commitId, List<Refactoring> refactorings) {
                    JsonUtil.commitJSON(sb, gitURL, commitId, refactorings);
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
            JsonUtil.endJSON(sb);
            System.out.println(sb);
        }
    }

    /**
     * Detects refactorings in all commits in the specified branch.
     */
    private static void detectAll(String[] args) throws Exception {
        if (args.length > 3) {
            System.out.println("Incorrect arguments. Please, use '-h' option for help.\n");
        }
        String folder = args[1];
        String branch = null;
        if (args.length == 3) {
            branch = args[2];
        }
        GitService gitService = new GitService();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryKotlinRMiner detector = new GitHistoryKotlinRMiner();
            StringBuilder sb = new StringBuilder();
            JsonUtil.startJSON(sb);
            detector.detectAll(repo, branch, new RefactoringHandler() {
                private int commitCount = 0;

                @Override
                public void handle(String commitId, List<Refactoring> refactorings) {
                    if (commitCount > 0) {
                        sb.append(",").append("\n");
                    }
                    JsonUtil.commitJSON(sb, gitURL, commitId, refactorings);
                    commitCount++;
                }

                @Override
                public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
                    System.out.printf("Total count: [Commits: %d, Errors: %d, Refactorings: %d]%n",
                        commitsCount, errorCommitsCount, refactoringsCount);
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
            JsonUtil.endJSON(sb);
            System.out.println(sb);
        }
    }

    /**
     * Detects refactorings in all commits in the range between two specified commits.
     */
    private static void detectBetweenCommits(String[] args) throws Exception {
        if (!(args.length == 3 || args.length == 4)) {
            System.out.println("Incorrect arguments. Please, use '-h' option for help.\n");
        }
        String folder = args[1];
        String startCommit = args[2];
        String endCommit = (args.length == 4) ? args[3] : null;
        GitService gitService = new GitService();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryKotlinRMiner detector = new GitHistoryKotlinRMiner();
            StringBuilder sb = new StringBuilder();
            JsonUtil.startJSON(sb);
            detector.detectBetweenCommits(repo, startCommit, endCommit, new RefactoringHandler() {
                private int commitCount = 0;

                @Override
                public void handle(String commitId, List<Refactoring> refactorings) {
                    if (commitCount > 0) {
                        sb.append(",").append("\n");
                    }
                    JsonUtil.commitJSON(sb, gitURL, commitId, refactorings);
                    commitCount++;
                }

                @Override
                public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
                    System.out.printf("Total count: [Commits: %d, Errors: %d, Refactorings: %d]%n",
                        commitsCount, errorCommitsCount, refactoringsCount);
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
            JsonUtil.endJSON(sb);
            System.out.println(sb);
        }
    }

    private static void printUsage() {
        System.out.println("-h Usage: kotlinRMiner-1.0.jar <args>");
        System.out.println(
            "-c <git-repo-folder> <commit-sha1>\t\t\t\tDetect refactorings at the specific commit <commit-sha1> for " +
                "project <git-repo-folder>.");
        System.out.println(
            "-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1>\tDetect refactorings between " +
                "<start-commit-sha1> and <end-commit-sha1> for a project <git-repo-folder>.");
        System.out.println(
            "-all <git-repo-folder> <branch>\t\t\t\t\tDetect all refactorings at the <branch> for <git-repo-folder>. " +
                "If <branch> is not specified, commits from master branch are analyzed.");
    }
}
