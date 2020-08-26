package org.jetbrains.research.kotlinrminer.diff.refactoring;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.diff.RenamePattern;
import org.jetbrains.research.kotlinrminer.uml.UMLClass;
import org.jetbrains.research.kotlinrminer.util.PrefixSuffixUtils;

public class MoveClassRefactoring implements Refactoring {
    private final UMLClass originalClass;
    private final UMLClass movedClass;

    public MoveClassRefactoring(UMLClass originalClass, UMLClass movedClass) {
        this.originalClass = originalClass;
        this.movedClass = movedClass;
    }

    public String toString() {
        return getName() + "\t" +
            originalClass.getName() +
            " moved to " +
            movedClass.getName();
    }

    public RenamePattern getRenamePattern() {
        int separatorPos =
            PrefixSuffixUtils.separatorPosOfCommonSuffix('.', originalClass.getName(), movedClass.getName());
        if (separatorPos == -1) {
            return new RenamePattern(originalClass.getName(), movedClass.getName());
        }
        String originalPath = originalClass.getName().substring(0, originalClass.getName().length() - separatorPos);
        String movedPath = movedClass.getName().substring(0, movedClass.getName().length() - separatorPos);
        return new RenamePattern(originalPath, movedPath);
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MOVE_CLASS;
    }

    public String getOriginalClassName() {
        return originalClass.getName();
    }

    public String getMovedClassName() {
        return movedClass.getName();
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
            new ImmutablePair<>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getMovedClass().getLocationInfo().getFilePath(), getMovedClass().getName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalClass.codeRange()
            .setDescription("original type declaration")
            .setCodeElement(originalClass.getName()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(movedClass.codeRange()
            .setDescription("moved type declaration")
            .setCodeElement(movedClass.getName()));
        return ranges;
    }
}
