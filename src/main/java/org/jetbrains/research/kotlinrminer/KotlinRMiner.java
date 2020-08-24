package org.jetbrains.research.kotlinrminer;

import org.eclipse.jgit.lib.Repository;
import org.jetbrains.research.kotlinrminer.api.GitHistoryRefactoringMiner;
import org.jetbrains.research.kotlinrminer.api.GitService;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringHandler;

import java.util.List;

public class KotlinRMiner {
    public static void main(String[] args) throws Exception {
        detectAtCommit(args);
    }

    /**
     * Detects refactorings at the specific commit
     */
    private static void detectAtCommit(String[] args) throws Exception {
        String folder = args[0];
        String commitId = args[1];
        GitService gitService = new GitService();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMiner();
            StringBuilder sb = new StringBuilder();
            startJSON(sb);
            detector.detectAtCommit(repo, commitId, new RefactoringHandler() {
                @Override
                public void handle(String commitId, List<Refactoring> refactorings) {
                    commitJSON(sb, gitURL, commitId, refactorings);
                }
            });
            endJSON(sb);
            System.out.println(sb.toString());
        }
    }

    private static void commitJSON(StringBuilder sb,
                                   String cloneURL,
                                   String currentCommitId,
                                   List<Refactoring> refactoringsAtRevision) {
        sb.append("{").append("\n");
        sb.append("\t").append("\"").append("repository").append("\"").append(": ").append("\"").append(
                cloneURL).append("\"").append(",").append("\n");
        sb.append("\t").append("\"").append("sha1").append("\"").append(": ").append("\"").append(
                currentCommitId).append("\"").append(",").append("\n");
        String url = GitHistoryRefactoringMiner.extractCommitURL(cloneURL, currentCommitId);
        sb.append("\t").append("\"").append("url").append("\"").append(": ").append("\"").append(url).append(
                "\"").append(",").append("\n");
        sb.append("\t").append("\"").append("refactorings").append("\"").append(": ");
        sb.append("[");
        int counter = 0;
        for (Refactoring refactoring : refactoringsAtRevision) {
            sb.append(refactoring.toJSON());
            if (counter < refactoringsAtRevision.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
            counter++;
        }
        sb.append("]").append("\n");
        sb.append("}");
    }

    private static void startJSON(StringBuilder sb) {
        sb.append("{").append("\n");
        sb.append("\"").append("commits").append("\"").append(": ");
        sb.append("[").append("\n");
    }

    private static void endJSON(StringBuilder sb) {
        sb.append("]").append("\n");
        sb.append("}");
    }
}
