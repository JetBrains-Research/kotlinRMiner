package org.jetbrains.research.kotlinrminer.ide.util;

//import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
//import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;

public class PsiUtils {
    public static int countColumn(int lineNumber, Document doc) {
        TextRange textRange = new TextRange(doc.getLineStartOffset(lineNumber), doc.getLineEndOffset(lineNumber));
        final String line =
            textRange.substring(doc.getText());
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ')
                count++;
            else if (c == '\t')
                count += 4;
            else
                return count;
        }
        return count;
    }
}
