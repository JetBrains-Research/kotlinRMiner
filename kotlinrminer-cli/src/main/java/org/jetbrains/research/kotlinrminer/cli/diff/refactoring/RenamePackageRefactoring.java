package org.jetbrains.research.kotlinrminer.cli.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.common.RefactoringType;
import org.jetbrains.research.kotlinrminer.cli.Refactoring;
import org.jetbrains.research.kotlinrminer.cli.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.cli.diff.RenamePattern;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RenamePackageRefactoring implements Refactoring {
    private final List<MoveClassRefactoring> moveClassRefactorings;
    private final RenamePattern pattern;

    public RenamePackageRefactoring(MoveClassRefactoring moveClassRefactoring) {
        this.moveClassRefactorings = new ArrayList<>();
        this.moveClassRefactorings.add(moveClassRefactoring);
        this.pattern = moveClassRefactoring.getRenamePattern();
    }

    public void addMoveClassRefactoring(MoveClassRefactoring moveClassRefactoring) {
        moveClassRefactorings.add(moveClassRefactoring);
    }

    public RenamePattern getPattern() {
        return pattern;
    }

    public List<MoveClassRefactoring> getMoveClassRefactorings() {
        return moveClassRefactorings;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_PACKAGE;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" ");
        String originalPath =
            pattern.getBefore().endsWith(".") ? pattern.getBefore().substring(0, pattern.getBefore().length() - 1) :
                pattern.getBefore();
        sb.append(originalPath);
        sb.append(" to ");
        String movedPath =
            pattern.getAfter().endsWith(".") ? pattern.getAfter().substring(0, pattern.getAfter().length() - 1) :
                pattern.getAfter();
        sb.append(movedPath);
        return sb.toString();
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        for (MoveClassRefactoring ref : moveClassRefactorings) {
            pairs.add(new ImmutablePair<>(ref.getOriginalClass().getLocationInfo().getFilePath(),
                ref.getOriginalClassName()));
        }
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        for (MoveClassRefactoring ref : moveClassRefactorings) {
            pairs.add(
                new ImmutablePair<>(ref.getMovedClass().getLocationInfo().getFilePath(), ref.getMovedClassName()));
        }
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (MoveClassRefactoring ref : moveClassRefactorings) {
            ranges.add(ref.getOriginalClass().codeRange()
                .setDescription("original type declaration")
                .setCodeElement(ref.getOriginalClass().getQualifiedName()));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (MoveClassRefactoring ref : moveClassRefactorings) {
            ranges.add(ref.getMovedClass().codeRange()
                .setDescription("moved type declaration")
                .setCodeElement(ref.getMovedClass().getQualifiedName()));
        }
        return ranges;
    }
}
