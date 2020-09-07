package org.jetbrains.research.kotlinrminer.diff.refactoring;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.decomposition.UMLOperationBodyMapper;
import org.jetbrains.research.kotlinrminer.decomposition.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

public class RenameOperationRefactoring implements Refactoring {
    private final UMLOperation originalOperation;
    private final UMLOperation renamedOperation;
    private final Set<Replacement> replacements;
    private UMLOperationBodyMapper bodyMapper;

    public RenameOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
        this.bodyMapper = bodyMapper;
        this.originalOperation = bodyMapper.getOperation1();
        this.renamedOperation = bodyMapper.getOperation2();
        this.replacements = bodyMapper.getReplacements();
    }

    public RenameOperationRefactoring(UMLOperation originalOperation, UMLOperation renamedOperation) {
        this.originalOperation = originalOperation;
        this.renamedOperation = renamedOperation;
        this.replacements = new LinkedHashSet<>();
    }

    private static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return getName() +
            originalOperation +
            " renamed to " +
            renamedOperation +
            " in class " + getClassName();
    }

    private String getClassName() {
        String sourceClassName = originalOperation.getClassName();
        String targetClassName = renamedOperation.getClassName();
        boolean targetIsAnonymousInsideSource = false;
        if (targetClassName.startsWith(sourceClassName + ".")) {
            String targetClassNameSuffix =
                targetClassName.substring(sourceClassName.length() + 1);
            targetIsAnonymousInsideSource = isNumeric(targetClassNameSuffix);
        }
        return sourceClassName.equals(targetClassName) || targetIsAnonymousInsideSource ? sourceClassName :
            targetClassName;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_METHOD;
    }

    public UMLOperationBodyMapper getBodyMapper() {
        return bodyMapper;
    }

    public UMLOperation getOriginalOperation() {
        return originalOperation;
    }

    public UMLOperation getRenamedOperation() {
        return renamedOperation;
    }

    public Set<Replacement> getReplacements() {
        return replacements;
    }

    /**
     * @return the code range of the source method in the <b>parent</b> commit
     */
    public CodeRange getSourceOperationCodeRangeBeforeRename() {
        return originalOperation.codeRange();
    }

    /**
     * @return the code range of the target method in the <b>child</b> commit
     */
    public CodeRange getTargetOperationCodeRangeAfterRename() {
        return renamedOperation.codeRange();
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOriginalOperation().getLocationInfo().getFilePath(),
            getOriginalOperation().getClassName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getRenamedOperation().getLocationInfo().getFilePath(),
            getRenamedOperation().getClassName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalOperation.codeRange()
            .setDescription("original method declaration")
            .setCodeElement(originalOperation.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(renamedOperation.codeRange()
            .setDescription("renamed method declaration")
            .setCodeElement(renamedOperation.toString()));
        return ranges;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((renamedOperation == null) ? 0 : renamedOperation.hashCode());
        result = prime * result + ((renamedOperation == null) ? 0 : renamedOperation.getLocationInfo().hashCode());
        result = prime * result + ((originalOperation == null) ? 0 : originalOperation.hashCode());
        result = prime * result + ((originalOperation == null) ? 0 : originalOperation.getLocationInfo().hashCode());
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
        RenameOperationRefactoring other = (RenameOperationRefactoring) obj;
        if (renamedOperation == null) {
            if (other.renamedOperation != null) {
                return false;
            }
        } else if (!renamedOperation.equals(other.renamedOperation)) {
            return false;
        } else if (!renamedOperation.getLocationInfo().equals(other.renamedOperation.getLocationInfo())) {
            return false;
        }
        if (originalOperation == null) {
            return other.originalOperation == null;
        } else if (!originalOperation.equals(other.originalOperation)) {
            return false;
        } else {
            return originalOperation.getLocationInfo().equals(other.originalOperation.getLocationInfo());
        }
    }
}

