package org.jetbrains.research.kotlinrminer.ide.decomposition.replacement;

import org.jetbrains.research.kotlinrminer.common.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.ide.decomposition.OperationInvocation;

public class VariableReplacementWithMethodInvocation extends Replacement {
    private final OperationInvocation invokedOperation;
    private final Direction direction;

    public VariableReplacementWithMethodInvocation(String before,
                                                   String after,
                                                   OperationInvocation invocation,
                                                   Direction direction) {
        super(before, after, Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION);
        this.invokedOperation = invocation;
        this.direction = direction;
    }

    public OperationInvocation getInvokedOperation() {
        return invokedOperation;
    }

    public Direction getDirection() {
        return direction;
    }

    public enum Direction {
        VARIABLE_TO_INVOCATION,
        INVOCATION_TO_VARIABLE
    }
}
