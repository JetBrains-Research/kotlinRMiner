package org.jetbrains.research.kotlinrminer.diff;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import org.jetbrains.research.kotlinrminer.decomposition.CodeElementType;
import org.jetbrains.research.kotlinrminer.decomposition.LocationInfo;
import org.jetbrains.research.kotlinrminer.decomposition.AbstractCodeFragment;

import java.util.Set;

public class CodeRange {
    private final String filePath;
    private final int startLine;
    private final int endLine;
    private final int startColumn;
    private final int endColumn;
    private final CodeElementType codeElementType;
    private String description;
    private String codeElement;

    public CodeRange(String filePath, int startLine, int endLine,
                     int startColumn, int endColumn, CodeElementType codeElementType) {
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
        this.codeElementType = codeElementType;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public CodeElementType getCodeElementType() {
        return codeElementType;
    }

    public String getDescription() {
        return description;
    }

    public CodeRange setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCodeElement() {
        return codeElement;
    }

    public CodeRange setCodeElement(String codeElement) {
        this.codeElement = codeElement;
        return this;
    }

    public boolean subsumes(CodeRange other) {
        return this.filePath.equals(other.filePath) &&
            this.startLine <= other.startLine &&
            this.endLine >= other.endLine;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n");
        encodeStringProperty(sb, "filePath", filePath, false);
        encodeIntProperty(sb, "startLine", startLine, false);
        encodeIntProperty(sb, "endLine", endLine, false);
        encodeIntProperty(sb, "startColumn", startColumn, false);
        encodeIntProperty(sb, "endColumn", endColumn, false);
        encodeStringProperty(sb, "codeElementType", codeElementType.name(), false);
        encodeStringProperty(sb, "description", description, false);
        encodeStringProperty(sb, "codeElement", escapeQuotes(codeElement), true);
        sb.append("}");
        return sb.toString();
    }

    private String escapeQuotes(String s) {
        if (s != null) {
            StringBuilder sb = new StringBuilder();
            JsonStringEncoder encoder = BufferRecyclers.getJsonStringEncoder();
            encoder.quoteAsString(s, sb);
            return sb.toString();
        }
        return s;
    }

    private void encodeStringProperty(StringBuilder sb, String propertyName, String value, boolean last) {
        if (value != null)
            sb.append("\t").append("\t").append("\"").append(propertyName).append("\"").append(": ").append(
                "\"").append(value).append("\"");
        else
            sb.append("\t").append("\t").append("\"").append(propertyName).append("\"").append(": ").append(value);
        insertNewLine(sb, last);
    }

    private void encodeIntProperty(StringBuilder sb, String propertyName, int value, boolean last) {
        sb.append("\t").append("\t").append("\"").append(propertyName).append("\"").append(": ").append(value);
        insertNewLine(sb, last);
    }

    private void insertNewLine(StringBuilder sb, boolean last) {
        if (last)
            sb.append("\n");
        else
            sb.append(",").append("\n");
    }

    public static CodeRange computeRange(Set<AbstractCodeFragment> codeFragments) {
        String filePath = null;
        int minStartLine = 0;
        int maxEndLine = 0;
        int startColumn = 0;
        int endColumn = 0;

        for (AbstractCodeFragment fragment : codeFragments) {
            LocationInfo info = fragment.getLocationInfo();
            filePath = info.getFilePath();
            if (minStartLine == 0 || info.getStartLine() < minStartLine) {
                minStartLine = info.getStartLine();
                startColumn = info.getStartColumn();
            }
            if (info.getEndLine() > maxEndLine) {
                maxEndLine = info.getEndLine();
                endColumn = info.getEndColumn();
            }
        }
        return new CodeRange(filePath, minStartLine, maxEndLine, startColumn, endColumn,
                             CodeElementType.LIST_OF_STATEMENTS);
    }

}