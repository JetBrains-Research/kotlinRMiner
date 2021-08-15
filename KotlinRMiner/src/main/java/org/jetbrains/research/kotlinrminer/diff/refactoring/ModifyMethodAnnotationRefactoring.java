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

public class ModifyMethodAnnotationRefactoring implements Refactoring {
    private final UMLAnnotation annotationBefore;
    private final UMLAnnotation annotationAfter;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public ModifyMethodAnnotationRefactoring(UMLAnnotation annotationBefore,
                                             UMLAnnotation annotationAfter,
                                             UMLOperation operationBefore,
                                             UMLOperation operationAfter) {
        this.annotationBefore = annotationBefore;
        this.annotationAfter = annotationAfter;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public UMLAnnotation getAnnotationBefore() {
        return annotationBefore;
    }

    public UMLAnnotation getAnnotationAfter() {
        return annotationAfter;
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
        ranges.add(annotationBefore.codeRange()
                       .setDescription("original annotation")
                       .setCodeElement(annotationBefore.toString()));
        ranges.add(operationBefore.codeRange()
                       .setDescription("original method declaration")
                       .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(annotationAfter.codeRange()
                       .setDescription("modified annotation")
                       .setCodeElement(annotationAfter.toString()));
        ranges.add(operationAfter.codeRange()
                       .setDescription("method declaration with modified annotation")
                       .setCodeElement(operationAfter.toString()));
        return ranges;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.MODIFY_METHOD_ANNOTATION;
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
            annotationBefore +
            " to " +
            annotationAfter +
            " in method " +
            operationAfter +
            " from class " +
            operationAfter.getClassName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((annotationAfter == null) ? 0 : annotationAfter.hashCode());
        result = prime * result + ((annotationBefore == null) ? 0 : annotationBefore.hashCode());
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
        ModifyMethodAnnotationRefactoring other = (ModifyMethodAnnotationRefactoring) obj;
        if (annotationAfter == null) {
            if (other.annotationAfter != null) {
                return false;
            }
        } else if (!annotationAfter.equals(other.annotationAfter)) {
            return false;
        }
        if (annotationBefore == null) {
            if (other.annotationBefore != null) {
                return false;
            }
        } else if (!annotationBefore.equals(other.annotationBefore)) {
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
