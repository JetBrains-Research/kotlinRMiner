package org.jetbrains.research.kotlinrminer.ide.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.ide.Refactoring;
import org.jetbrains.research.kotlinrminer.common.RefactoringType;
import org.jetbrains.research.kotlinrminer.ide.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLAttribute;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RenameAttributeRefactoring implements Refactoring {
    private final UMLAttribute originalAttribute;
    private final UMLAttribute renamedAttribute;
    private final Set<CandidateAttributeRefactoring> attributeRenames;
    private final String classNameBefore;
    private final String classNameAfter;

    public RenameAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute renamedAttribute,
                                      Set<CandidateAttributeRefactoring> attributeRenames) {
        this.originalAttribute = originalAttribute;
        this.renamedAttribute = renamedAttribute;
        this.classNameBefore = originalAttribute.getClassName();
        this.classNameAfter = renamedAttribute.getClassName();
        this.attributeRenames = attributeRenames;
    }

    public UMLAttribute getOriginalAttribute() {
        return originalAttribute;
    }

    public UMLAttribute getRenamedAttribute() {
        return renamedAttribute;
    }

    public Set<CandidateAttributeRefactoring> getAttributeRenames() {
        return attributeRenames;
    }

    public String getClassNameBefore() {
        return classNameBefore;
    }

    public String getClassNameAfter() {
        return classNameAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_ATTRIBUTE;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        return getName() + "\t" +
            originalAttribute.getVariableDeclaration() +
            " to " +
            renamedAttribute.getVariableDeclaration() +
            " in class " + classNameAfter;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
        result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
        result =
            prime * result + ((originalAttribute == null || originalAttribute.getVariableDeclaration() == null) ? 0 :
                originalAttribute.getVariableDeclaration().hashCode());
        result = prime * result + ((renamedAttribute == null || renamedAttribute.getVariableDeclaration() == null) ? 0 :
            renamedAttribute.getVariableDeclaration().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RenameAttributeRefactoring other = (RenameAttributeRefactoring) obj;
        if (classNameAfter == null) {
            if (other.classNameAfter != null)
                return false;
        } else if (!classNameAfter.equals(other.classNameAfter))
            return false;
        if (classNameBefore == null) {
            if (other.classNameBefore != null)
                return false;
        } else if (!classNameBefore.equals(other.classNameBefore))
            return false;
        if (originalAttribute == null) {
            if (other.originalAttribute != null)
                return false;
        } else if (originalAttribute.getVariableDeclaration() == null) {
            if (other.originalAttribute.getVariableDeclaration() != null)
                return false;
        } else if (!originalAttribute.getVariableDeclaration().equals(other.originalAttribute.getVariableDeclaration()))
            return false;
        if (renamedAttribute == null) {
            return other.renamedAttribute == null;
        } else if (renamedAttribute.getVariableDeclaration() == null) {
            return other.renamedAttribute.getVariableDeclaration() == null;
        } else return renamedAttribute.getVariableDeclaration().equals(other.renamedAttribute.getVariableDeclaration());
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOriginalAttribute().getLocationInfo().getFilePath(),
                                      getClassNameBefore()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getRenamedAttribute().getLocationInfo().getFilePath(),
                                      getClassNameAfter()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalAttribute.getVariableDeclaration().codeRange()
                       .setDescription("original attribute declaration")
                       .setCodeElement(originalAttribute.getVariableDeclaration().toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(renamedAttribute.getVariableDeclaration().codeRange()
                       .setDescription("renamed attribute declaration")
                       .setCodeElement(renamedAttribute.getVariableDeclaration().toString()));
        return ranges;
    }
}

