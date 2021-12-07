package org.jetbrains.research.kotlinrminer.core.diff.refactoring;

import org.jetbrains.research.kotlinrminer.core.RefactoringType;
import org.jetbrains.research.kotlinrminer.core.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.core.decomposition.UMLOperationBodyMapper;
import org.jetbrains.research.kotlinrminer.core.diff.CodeRange;

import java.util.ArrayList;
import java.util.List;

public class PullUpOperationRefactoring extends MoveOperationRefactoring {

    public PullUpOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
        super(bodyMapper);
    }

    public PullUpOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
        super(originalOperation, movedOperation);
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.PULL_UP_OPERATION;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(movedOperation.codeRange()
            .setDescription("pulled up method declaration")
            .setCodeElement(movedOperation.toString()));
        return ranges;
    }
}

