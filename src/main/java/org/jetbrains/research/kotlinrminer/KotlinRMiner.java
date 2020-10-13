package org.jetbrains.research.kotlinrminer;

import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.jetbrains.research.kotlinrminer.api.GitHistoryRefactoringMiner;
import org.jetbrains.research.kotlinrminer.api.GitService;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringHandler;

import static org.jetbrains.research.kotlinrminer.util.JsonUtil.*;

public class KotlinRMiner {
    public static void main(String[] args) throws Exception {
        detectAtCommit(args);
    }

    /**
     * Detects refactorings at the specific commit.
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

}
