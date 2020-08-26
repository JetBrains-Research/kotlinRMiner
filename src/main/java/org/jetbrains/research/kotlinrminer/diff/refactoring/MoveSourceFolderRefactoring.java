package org.jetbrains.research.kotlinrminer.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.diff.MovedClassToAnotherSourceFolder;
import org.jetbrains.research.kotlinrminer.diff.RenamePattern;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MoveSourceFolderRefactoring implements Refactoring {
    private final List<MovedClassToAnotherSourceFolder> movedClassesToAnotherSourceFolder;
    private final RenamePattern pattern;

    public MoveSourceFolderRefactoring(MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder) {
        this.movedClassesToAnotherSourceFolder = new ArrayList<>();
        this.movedClassesToAnotherSourceFolder.add(movedClassToAnotherSourceFolder);
        this.pattern = movedClassToAnotherSourceFolder.getRenamePattern();
    }

    public void addMovedClassToAnotherSourceFolder(MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder) {
        movedClassesToAnotherSourceFolder.add(movedClassToAnotherSourceFolder);
    }

    public List<MovedClassToAnotherSourceFolder> getMovedClassesToAnotherSourceFolder() {
        return movedClassesToAnotherSourceFolder;
    }

    public RenamePattern getPattern() {
        return pattern;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        String originalPath =
                pattern.getBefore().endsWith("/") ? pattern.getBefore().substring(0, pattern.getBefore().length() - 1) :
                        pattern.getBefore();
        sb.append(originalPath);
        sb.append(" to ");
        String movedPath =
                pattern.getAfter().endsWith("/") ? pattern.getAfter().substring(0, pattern.getAfter().length() - 1) :
                        pattern.getAfter();
        sb.append(movedPath);
        return sb.toString();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MOVE_SOURCE_FOLDER;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        for (MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
            pairs.add(new ImmutablePair<>(ref.getOriginalClass().getLocationInfo().getFilePath(),
                                          ref.getOriginalClassName()));
        }
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        for (MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
            pairs.add(
                    new ImmutablePair<>(ref.getMovedClass().getLocationInfo().getFilePath(), ref.getMovedClassName()));
        }
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
            ranges.add(ref.getOriginalClass().codeRange()
                               .setDescription("original type declaration")
                               .setCodeElement(ref.getOriginalClass().getName()));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
            ranges.add(ref.getMovedClass().codeRange()
                               .setDescription("moved type declaration")
                               .setCodeElement(ref.getMovedClass().getName()));
        }
        return ranges;
    }
}
