package org.jetbrains.research.kotlinrminer.cli.decomposition;

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.FileViewProvider;
import org.jetbrains.kotlin.psi.KtFile;

import static org.jetbrains.research.kotlinrminer.cli.util.PsiUtils.countColumn;

public class VariableScope {
    private final String filePath;
    private final int startOffset;
    private final int endOffset;
    private final int startLine;
    private int startColumn;
    private final int endLine;
    private int endColumn;

    public VariableScope(KtFile ktFile, String filePath, int startOffset, int endOffset) {
        this.filePath = filePath;
        this.startOffset = startOffset;
        this.endOffset = endOffset;

        FileViewProvider fileViewProvider = ktFile.getViewProvider();
        Document document = fileViewProvider.getDocument();

        this.startLine = document != null ? document.getLineNumber(startOffset) : 0;
        this.endLine = document.getLineNumber(endOffset);
        //columns are 0-based
        this.startColumn = countColumn(startLine, document);
        //convert to 1-based
        if (this.startColumn > 0) {
            this.startColumn += 1;
        }
        this.endColumn = countColumn(endLine, document);
        //convert to 1-based
        if (this.endColumn > 0) {
            this.endColumn += 1;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endColumn;
        result = prime * result + endLine;
        result = prime * result + endOffset;
        result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
        result = prime * result + startColumn;
        result = prime * result + startLine;
        result = prime * result + startOffset;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VariableScope other = (VariableScope) obj;
        if (endColumn != other.endColumn)
            return false;
        if (endLine != other.endLine)
            return false;
        if (endOffset != other.endOffset)
            return false;
        if (filePath == null) {
            if (other.filePath != null)
                return false;
        } else if (!filePath.equals(other.filePath))
            return false;
        if (startColumn != other.startColumn)
            return false;
        if (startLine != other.startLine)
            return false;
        return startOffset == other.startOffset;
    }

    public String toString() {
        return startLine + ":" + startColumn +
            "-" +
            endLine + ":" + endColumn;
    }

    public boolean subsumes(LocationInfo other) {
        return this.filePath.equals(other.getFilePath()) &&
            this.startOffset <= other.getStartOffset() &&
            this.endOffset >= other.getEndOffset();
    }
}