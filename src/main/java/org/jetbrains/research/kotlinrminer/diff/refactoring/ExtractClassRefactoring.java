package org.jetbrains.research.kotlinrminer.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.diff.UMLClassBaseDiff;
import org.jetbrains.research.kotlinrminer.uml.UMLAttribute;
import org.jetbrains.research.kotlinrminer.uml.UMLClass;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtractClassRefactoring implements Refactoring {
    private final UMLClass extractedClass;
    private final UMLClassBaseDiff classDiff;
    private final Set<UMLOperation> extractedOperations;
    private final Set<UMLAttribute> extractedAttributes;
    private final UMLAttribute attributeOfExtractedClassTypeInOriginalClass;

    public ExtractClassRefactoring(UMLClass extractedClass,
                                   UMLClassBaseDiff classDiff,
                                   Set<UMLOperation> extractedOperations,
                                   Set<UMLAttribute> extractedAttributes,
                                   UMLAttribute attributeOfExtractedClassType) {
        this.extractedClass = extractedClass;
        this.classDiff = classDiff;
        this.extractedOperations = extractedOperations;
        this.extractedAttributes = extractedAttributes;
        this.attributeOfExtractedClassTypeInOriginalClass = attributeOfExtractedClassType;
    }

    public String toString() {
        return getName() + "\t" +
            extractedClass +
            " from class " +
            classDiff.getOriginalClass();
    }

    public RefactoringType getRefactoringType() {
        if (extractedClass.isSubTypeOf(classDiff.getOriginalClass()) || extractedClass.isSubTypeOf(
            classDiff.getNextClass()))
            return RefactoringType.EXTRACT_SUBCLASS;
        return RefactoringType.EXTRACT_CLASS;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public UMLClass getExtractedClass() {
        return extractedClass;
    }

    public UMLClass getOriginalClass() {
        return classDiff.getOriginalClass();
    }

    public Set<UMLOperation> getExtractedOperations() {
        return extractedOperations;
    }

    public Set<UMLAttribute> getExtractedAttributes() {
        return extractedAttributes;
    }

    public UMLAttribute getAttributeOfExtractedClassTypeInOriginalClass() {
        return attributeOfExtractedClassTypeInOriginalClass;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOriginalClass().getLocationInfo().getFilePath(),
                                      getOriginalClass().getName()));
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
        ranges.add(classDiff.getOriginalClass().codeRange()
                       .setDescription("original type declaration")
                       .setCodeElement(classDiff.getOriginalClass().getName()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(extractedClass.codeRange()
                       .setDescription("extracted type declaration")
                       .setCodeElement(extractedClass.getName()));
        return ranges;
    }
}
