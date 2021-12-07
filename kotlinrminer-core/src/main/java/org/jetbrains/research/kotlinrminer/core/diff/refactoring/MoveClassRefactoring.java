package org.jetbrains.research.kotlinrminer.core.diff.refactoring;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.core.Refactoring;
import org.jetbrains.research.kotlinrminer.core.RefactoringType;
import org.jetbrains.research.kotlinrminer.core.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.core.diff.RenamePattern;
import org.jetbrains.research.kotlinrminer.core.uml.UMLClass;
import org.jetbrains.research.kotlinrminer.core.util.PrefixSuffixUtils;

public class MoveClassRefactoring implements Refactoring {
    private final UMLClass originalClass;
    private final UMLClass movedClass;

    public MoveClassRefactoring(UMLClass originalClass, UMLClass movedClass) {
        this.originalClass = originalClass;
        this.movedClass = movedClass;
    }

    public String toString() {
        return getName() + " " +
            originalClass.getQualifiedName() +
            " moved to " +
            movedClass.getQualifiedName();
    }

    public RenamePattern getRenamePattern() {
        int separatorPos =
            PrefixSuffixUtils.separatorPosOfCommonSuffix('.', originalClass.getQualifiedName(), movedClass.getQualifiedName());
        if (separatorPos == -1) {
            return new RenamePattern(originalClass.getQualifiedName(), movedClass.getQualifiedName());
        }
        String originalPath = originalClass.getQualifiedName().substring(0, originalClass.getQualifiedName().length() - separatorPos);
        String movedPath = movedClass.getQualifiedName().substring(0, movedClass.getQualifiedName().length() - separatorPos);
        return new RenamePattern(originalPath, movedPath);
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MOVE_CLASS;
    }

    public String getOriginalClassName() {
        return originalClass.getQualifiedName();
    }

    public String getMovedClassName() {
        return movedClass.getQualifiedName();
    }

    public UMLClass getOriginalClass() {
        return originalClass;
    }

    public UMLClass getMovedClass() {
        return movedClass;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(
            new ImmutablePair<>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getQualifiedName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getMovedClass().getLocationInfo().getFilePath(), getMovedClass().getQualifiedName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalClass.codeRange()
            .setDescription("original type declaration")
            .setCodeElement(originalClass.getQualifiedName()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(movedClass.codeRange()
            .setDescription("moved type declaration")
            .setCodeElement(movedClass.getQualifiedName()));
        return ranges;
    }
}
