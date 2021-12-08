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

public class MergeVariableRefactoring implements Refactoring {
    private final Set<VariableDeclaration> mergedVariables;
    private final VariableDeclaration newVariable;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> variableReferences;

    public MergeVariableRefactoring(Set<VariableDeclaration> mergedVariables,
                                    VariableDeclaration newVariable,
                                    UMLOperation operationBefore, UMLOperation operationAfter,
                                    Set<AbstractCodeMapping> variableReferences) {
        this.mergedVariables = mergedVariables;
        this.newVariable = newVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.variableReferences = variableReferences;
    }

    public Set<VariableDeclaration> getMergedVariables() {
        return mergedVariables;
    }

    public VariableDeclaration getNewVariable() {
        return newVariable;
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

    private boolean allVariablesAreParameters() {
        for (VariableDeclaration declaration : mergedVariables) {
            if (!declaration.isParameter()) {
                return false;
            }
        }
        return newVariable.isParameter();
    }

    public RefactoringType getRefactoringType() {
        if (allVariablesAreParameters()) {
            return RefactoringType.MERGE_PARAMETER;
        }
        return RefactoringType.MERGE_VARIABLE;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(
            new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(),
                getOperationBefore().getClassName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(),
            getOperationAfter().getClassName()));
        return pairs;
    }

    public String toString() {
        return getName() + "\t" +
            mergedVariables +
            " to " +
            newVariable +
            " in method " +
            operationAfter +
            " in class " + operationAfter.getClassName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mergedVariables == null) ? 0 : mergedVariables.hashCode());
        result = prime * result + ((newVariable == null) ? 0 : newVariable.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
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
        MergeVariableRefactoring other = (MergeVariableRefactoring) obj;
        if (mergedVariables == null) {
            if (other.mergedVariables != null) {
                return false;
            }
        } else if (!mergedVariables.equals(other.mergedVariables)) {
            return false;
        }
        if (newVariable == null) {
            if (other.newVariable != null) {
                return false;
            }
        } else if (!newVariable.equals(other.newVariable)) {
            return false;
        }
        if (operationAfter == null) {
            if (other.operationAfter != null) {
                return false;
            }
        } else if (!operationAfter.equals(other.operationAfter)) {
            return false;
        }
        if (operationBefore == null) {
            return other.operationBefore == null;
        } else {
            return operationBefore.equals(other.operationBefore);
        }
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (VariableDeclaration mergedVariable : mergedVariables) {
            ranges.add(mergedVariable.codeRange()
                .setDescription("merged variable declaration")
                .setCodeElement(mergedVariable.toString()));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(newVariable.codeRange()
            .setDescription("new variable declaration")
            .setCodeElement(newVariable.toString()));
        return ranges;
    }
}