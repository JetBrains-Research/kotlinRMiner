package org.jetbrains.research.kotlinrminer.diff.refactoring;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

public class SplitVariableRefactoring implements Refactoring {
    private final Set<VariableDeclaration> splitVariables;
    private final VariableDeclaration oldVariable;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> variableReferences;

    public SplitVariableRefactoring(VariableDeclaration oldVariable,
                                    Set<VariableDeclaration> splitVariables,
                                    UMLOperation operationBefore,
                                    UMLOperation operationAfter,
                                    Set<AbstractCodeMapping> variableReferences) {
        this.splitVariables = splitVariables;
        this.oldVariable = oldVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.variableReferences = variableReferences;
    }

    public Set<VariableDeclaration> getSplitVariables() {
        return splitVariables;
    }

    public VariableDeclaration getOldVariable() {
        return oldVariable;
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
        for (VariableDeclaration declaration : splitVariables) {
            if (!declaration.isParameter()) {
                return false;
            }
        }
        return oldVariable.isParameter();
    }

    @Override
    public RefactoringType getRefactoringType() {
        if (allVariablesAreParameters())
            return RefactoringType.SPLIT_PARAMETER;
        return RefactoringType.SPLIT_VARIABLE;
    }

    @Override
    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(),
                                      getOperationBefore().getClassName()));
        return pairs;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(
                new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(),
                                    getOperationAfter().getClassName()));
        return pairs;
    }

    public String toString() {
        return getName() + "\t" +
                oldVariable +
                " to " +
                splitVariables +
                " in method " +
                operationAfter +
                " in class " + operationAfter.getClassName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oldVariable == null) ? 0 : oldVariable.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
        result = prime * result + ((splitVariables == null) ? 0 : splitVariables.hashCode());
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
        SplitVariableRefactoring other = (SplitVariableRefactoring) obj;
        if (oldVariable == null) {
            if (other.oldVariable != null)
                return false;
        } else if (!oldVariable.equals(other.oldVariable))
            return false;
        if (operationAfter == null) {
            if (other.operationAfter != null)
                return false;
        } else if (!operationAfter.equals(other.operationAfter))
            return false;
        if (operationBefore == null) {
            if (other.operationBefore != null)
                return false;
        } else if (!operationBefore.equals(other.operationBefore))
            return false;
        if (splitVariables == null) {
            return other.splitVariables == null;
        } else return splitVariables.equals(other.splitVariables);
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(oldVariable.codeRange()
                           .setDescription("original variable declaration")
                           .setCodeElement(oldVariable.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (VariableDeclaration splitVariable : splitVariables) {
            ranges.add(splitVariable.codeRange()
                               .setDescription("split variable declaration")
                               .setCodeElement(splitVariable.toString()));
        }
        return ranges;
    }
}
