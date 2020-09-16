package org.jetbrains.research.kotlinrminer.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtractSuperClassRefactoring implements Refactoring {
    private final UMLClass extractedClass;
    private final Set<UMLClass> subclassSet;

    public ExtractSuperClassRefactoring(UMLClass extractedClass, Set<UMLClass> subclassSet) {
        this.extractedClass = extractedClass;
        this.subclassSet = subclassSet;
    }

    public String toString() {
        return getName() + "\t" +
            extractedClass +
            " from classes " +
            subclassSet;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        if (extractedClass.isInterface())
            return RefactoringType.EXTRACT_INTERFACE;
        else
            return RefactoringType.EXTRACT_SUPERCLASS;
    }

    public UMLClass getExtractedClass() {
        return extractedClass;
    }

    public Set<String> getSubclassSet() {
        Set<String> subclassSet = new LinkedHashSet<>();
        for (UMLClass umlClass : this.subclassSet) {
            subclassSet.add(umlClass.getName());
        }
        return subclassSet;
    }

    public Set<UMLClass> getUMLSubclassSet() {
        return new LinkedHashSet<>(subclassSet);
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        for (UMLClass umlClass : this.subclassSet) {
            pairs.add(new ImmutablePair<>(umlClass.getLocationInfo().getFilePath(), umlClass.getName()));
        }
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getExtractedClass().getLocationInfo().getFilePath(),
                                      getExtractedClass().getName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (UMLClass subclass : subclassSet) {
            ranges.add(subclass.codeRange()
                           .setDescription("sub-type declaration")
                           .setCodeElement(subclass.getName()));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(extractedClass.codeRange()
                       .setDescription("extracted super-type declaration")
                       .setCodeElement(extractedClass.getName()));
        return ranges;
    }
}
