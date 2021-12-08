package org.jetbrains.research.kotlinrminer.cli.diff.refactoring;

import org.jetbrains.research.kotlinrminer.common.RefactoringType;
import org.jetbrains.research.kotlinrminer.cli.decomposition.UMLOperationBodyMapper;
import org.jetbrains.research.kotlinrminer.cli.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.cli.uml.UMLOperation;

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

