package org.jetbrains.research.kotlinrminer.ide.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.ide.Refactoring;
import org.jetbrains.research.kotlinrminer.common.RefactoringType;
import org.jetbrains.research.kotlinrminer.ide.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RenameClassRefactoring implements Refactoring {
    private final UMLClass originalClass;
    private final UMLClass renamedClass;

    public RenameClassRefactoring(UMLClass originalClass, UMLClass renamedClass) {
        this.originalClass = originalClass;
        this.renamedClass = renamedClass;
    }

    public String toString() {
        return getName() + " " +
            originalClass.getQualifiedName() +
            " renamed to " +
            renamedClass.getQualifiedName();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_CLASS;
    }

    public String getOriginalClassName() {
        return originalClass.getQualifiedName();
    }

    public String getRenamedClassName() {
        return renamedClass.getQualifiedName();
    }

    public UMLClass getOriginalClass() {
        return originalClass;
    }

    public UMLClass getRenamedClass() {
        return renamedClass;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(
            new ImmutablePair<>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getQualifiedName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getRenamedClass().getLocationInfo().getFilePath(), getRenamedClass().getQualifiedName()));
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
        ranges.add(renamedClass.codeRange()
            .setDescription("renamed type declaration")
            .setCodeElement(renamedClass.getQualifiedName()));
        return ranges;
    }
}