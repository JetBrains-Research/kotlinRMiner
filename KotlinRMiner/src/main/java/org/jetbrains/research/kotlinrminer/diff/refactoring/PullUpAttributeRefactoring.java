package org.jetbrains.research.kotlinrminer.diff.refactoring;

import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLAttribute;

import java.util.ArrayList;
import java.util.List;

public class PullUpAttributeRefactoring extends MoveAttributeRefactoring {

    public PullUpAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute movedAttribute) {
        super(originalAttribute, movedAttribute);
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

    public RefactoringType getRefactoringType() {
        return RefactoringType.PULL_UP_ATTRIBUTE;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(movedAttribute.codeRange()
                       .setDescription("pulled up attribute declaration")
                       .setCodeElement(movedAttribute.toString()));
        return ranges;
    }
}