package org.jetbrains.research.kotlinrminer;

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.FileViewProvider;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

public class LocationInfo {
    private final String filePath;
    private final int startOffset;
    private final int endOffset;
    private final int length;
    private final int startLine;
    private int startColumn;
    private final int endLine;
    private int endColumn;
    private final CodeElementType codeElementType;

    public LocationInfo(KtFile ktFile, String filePath, KtElement node, CodeElementType codeElementType) {
        this.filePath = filePath;
        this.codeElementType = codeElementType;
        TextRange range = node.getTextRange();
        this.startOffset = range.getStartOffset();
        this.length = range.getLength();
        this.endOffset = range.getEndOffset();

        FileViewProvider fileViewProvider = ktFile.getViewProvider();
        Document document = fileViewProvider.getDocument();

        this.startLine = document != null ? document.getLineNumber(startOffset) : 0;
        this.endLine = document != null ? document.getLineNumber(endOffset) : 0;
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

    private int countColumn(int lineNumber, Document doc) {
        int count = 0;
        final String line = doc.getText(new TextRange(doc.getLineStartOffset(lineNumber), doc.getLineEndOffset(lineNumber)));
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LocationInfo other = (LocationInfo) obj;
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
        if (length != other.length)
            return false;
        if (startColumn != other.startColumn)
            return false;
        if (startLine != other.startLine)
            return false;
        return startOffset == other.startOffset;
    }

    public enum CodeElementType {
        WHEN_EXPRESSION,
        COMPANION_OBJECT,
        OBJECT,
        TYPE_DECLARATION,
        METHOD_DECLARATION,
        FIELD_DECLARATION,
        SINGLE_VARIABLE_DECLARATION,
        VARIABLE_DECLARATION_STATEMENT,
        VARIABLE_DECLARATION_EXPRESSION,
        VARIABLE_DECLARATION_INITIALIZER,
        ANONYMOUS_CLASS_DECLARATION,
        LAMBDA_EXPRESSION,
        LAMBDA_EXPRESSION_BODY,
        CLASS_INSTANCE_CREATION,
        ARRAY_CREATION,
        METHOD_INVOCATION,
        SUPER_METHOD_INVOCATION,
        LABELED_STATEMENT,
        FOR_STATEMENT("for"),
        FOR_STATEMENT_CONDITION,
        FOR_STATEMENT_INITIALIZER,
        FOR_STATEMENT_UPDATER,
        ENHANCED_FOR_STATEMENT("for"),
        ENHANCED_FOR_STATEMENT_PARAMETER_NAME,
        ENHANCED_FOR_STATEMENT_EXPRESSION,
        WHILE_STATEMENT("while"),
        WHILE_STATEMENT_CONDITION,
        IF_STATEMENT("if"),
        IF_STATEMENT_CONDITION,
        DO_STATEMENT("do"),
        DO_STATEMENT_CONDITION,
        SYNCHRONIZED_STATEMENT("synchronized"),
        SYNCHRONIZED_STATEMENT_EXPRESSION,
        TRY_STATEMENT("try"),
        TRY_STATEMENT_RESOURCE,
        CATCH_CLAUSE("catch"),
        CATCH_CLAUSE_EXCEPTION_NAME,
        EXPRESSION_STATEMENT,
        ASSERT_STATEMENT,
        RETURN_STATEMENT,
        THROW_STATEMENT,
        CONSTRUCTOR_INVOCATION,
        SUPER_CONSTRUCTOR_INVOCATION,
        BREAK_STATEMENT,
        CONTINUE_STATEMENT,
        EMPTY_STATEMENT,
        BLOCK("{"),
        FINALLY_BLOCK("finally"),
        TYPE,
        LIST_OF_STATEMENTS,
        ANNOTATION,
        SINGLE_MEMBER_ANNOTATION_VALUE,
        NORMAL_ANNOTATION_MEMBER_VALUE_PAIR;

        private String name;

        CodeElementType() {

        }

        CodeElementType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public CodeElementType setName(String name) {
            this.name = name;
            return this;
        }
    }
}
