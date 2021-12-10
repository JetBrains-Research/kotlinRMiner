package org.jetbrains.research.kotlinrminer.diff.refactoring;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.uml.UMLParameter;

public class ReorderParameterRefactoring implements Refactoring {
    private final List<VariableDeclaration> parametersBefore;
    private final List<VariableDeclaration> parametersAfter;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public ReorderParameterRefactoring(UMLOperation operationBefore, UMLOperation operationAfter) {
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.parametersBefore = new ArrayList<>();
        for (UMLParameter parameter : operationBefore.getParametersWithoutReturnType()) {
            parametersBefore.add(parameter.getVariableDeclaration());
        }
        this.parametersAfter = new ArrayList<>();
        for (UMLParameter parameter : operationAfter.getParametersWithoutReturnType()) {
            parametersAfter.add(parameter.getVariableDeclaration());
        }
    }

    public List<VariableDeclaration> getParametersBefore() {
        return parametersBefore;
    }

    public List<VariableDeclaration> getParametersAfter() {
        return parametersAfter;
    }

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (VariableDeclaration parameter : parametersBefore) {
            ranges.add(parameter.codeRange()
                .setDescription("original parameter declaration")
                .setCodeElement(parameter.toString()));
        }
        ranges.add(operationBefore.codeRange()
            .setDescription("original method declaration")
            .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (VariableDeclaration parameter : parametersAfter) {
            ranges.add(parameter.codeRange()
                .setDescription("reordered parameter declaration")
                .setCodeElement(parameter.toString()));
        }
        ranges.add(operationAfter.codeRange()
            .setDescription("method declaration with reordered parameters")
            .setCodeElement(operationAfter.toString()));
        return ranges;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.REORDER_PARAMETER;
    }

    @Override
    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(
            new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(),
                                getOperationBefore().getClassName()));
        return pairs;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(),
                                      getOperationAfter().getClassName()));
        return pairs;
    }

    public String toString() {
        return getName() + " " +
            parametersBefore +
            " to " +
            parametersAfter +
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
        result = prime * result + ((parametersAfter == null) ? 0 : parametersAfter.hashCode());
        result = prime * result + ((parametersBefore == null) ? 0 : parametersBefore.hashCode());
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
        ReorderParameterRefactoring other = (ReorderParameterRefactoring) obj;
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
        if (parametersAfter == null) {
            if (other.parametersAfter != null) {
                return false;
            }
        } else if (!parametersAfter.equals(other.parametersAfter)) {
            return false;
        }
        if (parametersBefore == null) {
            return other.parametersBefore == null;
        } else {
            return parametersBefore.equals(other.parametersBefore);
        }
    }
}
