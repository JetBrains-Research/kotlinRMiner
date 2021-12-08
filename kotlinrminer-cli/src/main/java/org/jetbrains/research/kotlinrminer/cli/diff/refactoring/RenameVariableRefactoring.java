package org.jetbrains.research.kotlinrminer.cli.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.common.RefactoringType;
import org.jetbrains.research.kotlinrminer.cli.Refactoring;
import org.jetbrains.research.kotlinrminer.cli.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.cli.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.cli.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.cli.uml.UMLOperation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RenameVariableRefactoring implements Refactoring {
    private final VariableDeclaration originalVariable;
    private final VariableDeclaration renamedVariable;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> variableReferences;

    public RenameVariableRefactoring(
        VariableDeclaration originalVariable,
        VariableDeclaration renamedVariable,
        UMLOperation operationBefore,
        UMLOperation operationAfter,
        Set<AbstractCodeMapping> variableReferences) {
        this.originalVariable = originalVariable;
        this.renamedVariable = renamedVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.variableReferences = variableReferences;
    }

    public RefactoringType getRefactoringType() {
        if (originalVariable.isParameter() && renamedVariable.isParameter()) {
            return RefactoringType.RENAME_PARAMETER;
        }
        if (!originalVariable.isParameter() && renamedVariable.isParameter()) {
            return RefactoringType.PARAMETERIZE_VARIABLE;
        }
        if (!originalVariable.isAttribute() && renamedVariable.isAttribute()) {
            return RefactoringType.REPLACE_VARIABLE_WITH_ATTRIBUTE;
        }
        return RefactoringType.RENAME_VARIABLE;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public VariableDeclaration getOriginalVariable() {
        return originalVariable;
    }

    public VariableDeclaration getRenamedVariable() {
        return renamedVariable;
    }

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    public Set<AbstractCodeMapping> getVariableReferences() {
        return variableReferences;
    }

    public String toString() {
        return getName() + " " +
            originalVariable +
            " to " +
            renamedVariable +
            " in method " +
            operationAfter +
            " in class " + operationAfter.getClassName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
        result = prime * result + ((originalVariable == null) ? 0 : originalVariable.hashCode());
        result = prime * result + ((renamedVariable == null) ? 0 : renamedVariable.hashCode());
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
        RenameVariableRefactoring other = (RenameVariableRefactoring) obj;
        if (operationAfter == null) {
            if (other.operationAfter != null) {
                return false;
            }
        } else if (!operationAfter.equals(other.operationAfter)) {
            return false;
        }
        if (operationBefore == null) {
            if (other.operationBefore != null) {
                return false;
            }
        } else if (!operationBefore.equals(other.operationBefore)) {
            return false;
        }
        if (originalVariable == null) {
            if (other.originalVariable != null) {
                return false;
            }
        } else if (!originalVariable.equals(other.originalVariable)) {
            return false;
        }
        if (renamedVariable == null) {
            return other.renamedVariable == null;
        } else {
            return renamedVariable.equals(other.renamedVariable);
        }
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(),
            getOperationBefore().getClassName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(
            new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(),
                getOperationAfter().getClassName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalVariable.codeRange()
            .setDescription("original variable declaration")
            .setCodeElement(originalVariable.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(renamedVariable.codeRange()
            .setDescription("renamed variable declaration")
            .setCodeElement(renamedVariable.toString()));
        return ranges;
    }
}

