package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.FileViewProvider;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

import static org.jetbrains.research.kotlinrminer.util.PsiUtils.countColumn;

/**
 * Provides an information about the element's location in the file.
 */
public class LocationInfo {
    private final String filePath;
    private final int startOffset;
    private final int endOffset;
    private final int length;
    private final int startLine;
    private final int endLine;
    private final CodeElementType codeElementType;
    private final int startColumn;
    private final int endColumn;

    public LocationInfo(@NotNull KtFile ktFile, @NotNull String filePath, @NotNull KtElement node,
                        @NotNull CodeElementType codeElementType) {
        this.filePath = filePath;
        this.codeElementType = codeElementType;
        TextRange range = node.getTextRange();
        this.startOffset = range.getStartOffset();
        this.length = range.getLength();
        this.endOffset = range.getEndOffset();

        FileViewProvider fileViewProvider = ktFile.getViewProvider();
        Document document = fileViewProvider.getDocument();

        if (document != null) {
            this.startLine = document.getLineNumber(startOffset) + 1;
            this.endLine = document.getLineNumber(endOffset) + 1;
            this.startColumn = countColumn(startLine - 1, document) + 1;
            this.endColumn = countColumn(endLine - 1, document) + 1;
        } else {
            this.startLine = 0;
            this.endLine = 0;
            this.startColumn = 0;
            this.endColumn = 0;
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getLength() {
        return length;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public CodeElementType getCodeElementType() {
        return codeElementType;
    }

    public CodeRange codeRange() {
        return new CodeRange(getFilePath(),
                             getStartLine(), getEndLine(),
                             getStartColumn(), getEndColumn(), getCodeElementType());
    }

    public boolean subsumes(LocationInfo other) {
        return this.filePath.equals(other.filePath) &&
            this.startOffset <= other.startOffset &&
            this.endOffset >= other.endOffset;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endColumn;
        result = prime * result + endLine;
        result = prime * result + endOffset;
        result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
        result = prime * result + length;
        result = prime * result + startColumn;
        result = prime * result + startLine;
        result = prime * result + startOffset;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LocationInfo other = (LocationInfo) obj;
        if (endColumn != other.endColumn) {
            return false;
        }
        if (endLine != other.endLine) {
            return false;
        }
        if (endOffset != other.endOffset) {
            return false;
        }
        if (filePath == null) {
            if (other.filePath != null) {
                return false;
            }
        } else if (!filePath.equals(other.filePath)) {
            return false;
        }
        if (length != other.length) {
            return false;
        }
        if (startColumn != other.startColumn) {
            return false;
        }
        if (startLine != other.startLine) {
            return false;
        }
        return startOffset == other.startOffset;
    }
}
