package org.jetbrains.research.kotlinrminer.util;

import org.jetbrains.research.kotlinrminer.api.GitHistoryRefactoringMiner;
import org.jetbrains.research.kotlinrminer.api.Refactoring;

import java.util.List;

/**
 * Presents the detection results in the JSON format.
 */
public class JsonUtil {

    public static void commitJSON(StringBuilder sb,
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

    public static void startJSON(StringBuilder sb) {
        sb.append("{").append("\n");
        sb.append("\"").append("commits").append("\"").append(": ");
        sb.append("[").append("\n");
    }

    public static void endJSON(StringBuilder sb) {
        sb.append("]").append("\n");
        sb.append("}");
    }

}
