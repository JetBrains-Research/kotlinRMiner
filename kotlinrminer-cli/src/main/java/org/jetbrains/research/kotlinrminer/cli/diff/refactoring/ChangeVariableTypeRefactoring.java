package org.jetbrains.research.kotlinrminer.cli.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.cli.Refactoring;
import org.jetbrains.research.kotlinrminer.cli.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.cli.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.cli.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.cli.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.common.RefactoringType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChangeVariableTypeRefactoring implements Refactoring {
    private final VariableDeclaration originalVariable;
    private final VariableDeclaration changedTypeVariable;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> variableReferences;
    private final Set<Refactoring> relatedRefactorings;

    public ChangeVariableTypeRefactoring(VariableDeclaration originalVariable,
                                         VariableDeclaration changedTypeVariable,
                                         UMLOperation operationBefore,
                                         UMLOperation operationAfter,
                                         Set<AbstractCodeMapping> variableReferences) {
        this.originalVariable = originalVariable;
        this.changedTypeVariable = changedTypeVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.variableReferences = variableReferences;
        this.relatedRefactorings = new LinkedHashSet<>();
    }

    public void addRelatedRefactoring(Refactoring refactoring) {
        this.relatedRefactorings.add(refactoring);
    }

    public Set<Refactoring> getRelatedRefactorings() {
        return relatedRefactorings;
    }

    public RefactoringType getRefactoringType() {
        if (originalVariable.isParameter() && changedTypeVariable.isParameter()) {
            return RefactoringType.CHANGE_PARAMETER_TYPE;
        }
        return RefactoringType.CHANGE_VARIABLE_TYPE;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public VariableDeclaration getOriginalVariable() {
        return originalVariable;
    }

    public VariableDeclaration getChangedTypeVariable() {
        return changedTypeVariable;
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
        StringBuilder sb = new StringBuilder();
        boolean qualified = originalVariable.getType().equals(
            changedTypeVariable.getType()) && !originalVariable.getType().equalsQualified(
            changedTypeVariable.getType());
        sb.append(getName()).append(" ");
        sb.append(qualified ? originalVariable.toQualifiedString() : originalVariable.toString());
        sb.append(" to ");
        sb.append(qualified ? changedTypeVariable.toQualifiedString() : changedTypeVariable.toString());
        sb.append(" in method ");
        sb.append(qualified ? operationAfter.toQualifiedString() : operationAfter.toString());
        sb.append(" in class ").append(operationAfter.getClassName());
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changedTypeVariable == null) ? 0 : changedTypeVariable.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
        result = prime * result + ((originalVariable == null) ? 0 : originalVariable.hashCode());
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
        ChangeVariableTypeRefactoring other = (ChangeVariableTypeRefactoring) obj;
        if (changedTypeVariable == null) {
            if (other.changedTypeVariable != null) {
                return false;
            }
        } else if (!changedTypeVariable.equals(other.changedTypeVariable)) {
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
            if (other.operationBefore != null) {
                return false;
            }
        } else if (!operationBefore.equals(other.operationBefore)) {
            return false;
        }
        if (originalVariable == null) {
            return other.originalVariable == null;
        } else {
            return originalVariable.equals(other.originalVariable);
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
        pairs.add(new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(),
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
        ranges.add(changedTypeVariable.codeRange()
            .setDescription("changed-type variable declaration")
            .setCodeElement(changedTypeVariable.toString()));
        return ranges;
    }
}

