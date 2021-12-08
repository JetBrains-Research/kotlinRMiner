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

public class MoveAttributeRefactoring implements Refactoring {
    protected UMLAttribute originalAttribute;
    protected UMLAttribute movedAttribute;
    private volatile int hashCode = 0;

    public MoveAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute movedAttribute) {
        this.originalAttribute = originalAttribute;
        this.movedAttribute = movedAttribute;
    }

    public String toString() {
        return getName() + " " +
            getOriginalAttribute().toQualifiedString() +
            " from class " +
            getSourceClassName() +
            " to " +
            getMovedAttribute().toQualifiedString() +
            " from class " +
            getTargetClassName();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MOVE_ATTRIBUTE;
    }

    public UMLAttribute getOriginalAttribute() {
        return originalAttribute;
    }

    public UMLAttribute getMovedAttribute() {
        return movedAttribute;
    }

    public String getSourceClassName() {
        return originalAttribute.getClassName();
    }

    public String getTargetClassName() {
        return movedAttribute.getClassName();
    }

    /**
     * @return the code range of the source attribute in the <b>parent</b> commit
     */
    public CodeRange getSourceAttributeCodeRangeBeforeMove() {
        return originalAttribute.codeRange();
    }

    /**
     * @return the code range of the target attribute in the <b>child</b> commit
     */
    public CodeRange getTargetAttributeCodeRangeAfterMove() {
        return movedAttribute.codeRange();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof MoveAttributeRefactoring) {
            MoveAttributeRefactoring other = (MoveAttributeRefactoring) o;
            return this.originalAttribute.equals(other.originalAttribute) &&
                this.movedAttribute.equals(other.movedAttribute) &&
                this.getSourceClassName().equals(other.getSourceClassName()) &&
                this.getTargetClassName().equals(other.getTargetClassName()) &&
                this.originalAttribute.getLocationInfo().equals(other.originalAttribute.getLocationInfo()) &&
                this.movedAttribute.getLocationInfo().equals(other.movedAttribute.getLocationInfo());
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + originalAttribute.hashCode();
            result = 37 * result + movedAttribute.hashCode();
            result = 37 * result + getSourceClassName().hashCode();
            result = 37 * result + getTargetClassName().hashCode();
            result = 37 * result + originalAttribute.getLocationInfo().hashCode();
            result = 37 * result + movedAttribute.getLocationInfo().hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOriginalAttribute().getLocationInfo().getFilePath(),
            getOriginalAttribute().getClassName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getMovedAttribute().getLocationInfo().getFilePath(),
            getMovedAttribute().getClassName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalAttribute.codeRange()
            .setDescription("original attribute declaration")
            .setCodeElement(originalAttribute.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(movedAttribute.codeRange()
            .setDescription("moved attribute declaration")
            .setCodeElement(movedAttribute.toString()));
        return ranges;
    }
}