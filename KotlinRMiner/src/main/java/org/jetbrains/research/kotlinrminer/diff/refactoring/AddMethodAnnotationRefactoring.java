package org.jetbrains.research.kotlinrminer.diff.refactoring;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLAnnotation;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

public class AddMethodAnnotationRefactoring implements Refactoring {
    private final UMLAnnotation annotation;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public AddMethodAnnotationRefactoring(UMLAnnotation annotation, UMLOperation operationBefore,
                                          UMLOperation operationAfter) {
        this.annotation = annotation;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public UMLAnnotation getAnnotation() {
        return annotation;
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
        ranges.add(operationBefore.codeRange()
                       .setDescription("original method declaration")
                       .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(annotation.codeRange()
                       .setDescription("added annotation")
                       .setCodeElement(annotation.toString()));
        ranges.add(operationAfter.codeRange()
                       .setDescription("method declaration with added annotation")
                       .setCodeElement(operationAfter.toString()));
        return ranges;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.ADD_METHOD_ANNOTATION;
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
        return getName() + "\t" +
            annotation +
            " in method " +
            operationAfter +
            " from class " +
            operationAfter.getClassName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
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
        AddMethodAnnotationRefactoring other = (AddMethodAnnotationRefactoring) obj;
        if (annotation == null) {
            if (other.annotation != null) {
                return false;
            }
        } else if (!annotation.equals(other.annotation)) {
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
        } else return operationBefore.equals(other.operationBefore);
    }
}
