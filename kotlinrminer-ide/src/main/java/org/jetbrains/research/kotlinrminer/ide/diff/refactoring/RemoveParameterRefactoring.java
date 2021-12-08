package org.jetbrains.research.kotlinrminer.ide.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.ide.Refactoring;
import org.jetbrains.research.kotlinrminer.common.RefactoringType;
import org.jetbrains.research.kotlinrminer.ide.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLParameter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RemoveParameterRefactoring implements Refactoring {
    private final UMLParameter parameter;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public RemoveParameterRefactoring(UMLParameter parameter, UMLOperation operationBefore,
                                      UMLOperation operationAfter) {
        this.parameter = parameter;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public UMLParameter getParameter() {
        return parameter;
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
        ranges.add(parameter.getVariableDeclaration().codeRange()
            .setDescription("removed parameter")
            .setCodeElement(parameter.getVariableDeclaration().toString()));
        ranges.add(operationBefore.codeRange()
            .setDescription("original method declaration")
            .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(operationAfter.codeRange()
            .setDescription("method declaration with removed parameter")
            .setCodeElement(operationAfter.toString()));
        return ranges;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.REMOVE_PARAMETER;
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
            parameter.getVariableDeclaration() +
            " in method " +
            operationBefore +
            " from class " +
            operationBefore.getClassName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((parameter.getVariableDeclaration() == null) ? 0 :
            parameter.getVariableDeclaration().hashCode());
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
        RemoveParameterRefactoring other = (RemoveParameterRefactoring) obj;
        if (parameter == null) {
            if (other.parameter != null) {
                return false;
            }
        } else if (!parameter.getVariableDeclaration()
            .equals(other.parameter.getVariableDeclaration())) {
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
}
