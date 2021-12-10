package org.jetbrains.research.kotlinrminer.diff.refactoring;

import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.decomposition.UMLOperationBodyMapper;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

import java.util.ArrayList;
import java.util.List;

public class PushDownOperationRefactoring extends MoveOperationRefactoring {

    public PushDownOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
        super(bodyMapper);
    }

    public PushDownOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
        super(originalOperation, movedOperation);
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.PUSH_DOWN_OPERATION;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(movedOperation.codeRange()
                       .setDescription("pushed down method declaration")
                       .setCodeElement(movedOperation.toString()));
        return ranges;
    }
}
