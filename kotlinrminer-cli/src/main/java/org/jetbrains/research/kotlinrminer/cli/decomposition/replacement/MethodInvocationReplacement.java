package org.jetbrains.research.kotlinrminer.cli.decomposition.replacement;

import org.jetbrains.research.kotlinrminer.common.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.cli.decomposition.OperationInvocation;

public class MethodInvocationReplacement extends Replacement {
    private final OperationInvocation invokedOperationBefore;
    private final OperationInvocation invokedOperationAfter;

    public MethodInvocationReplacement(String before,
                                       String after,
                                       OperationInvocation invokedOperationBefore,
                                       OperationInvocation invokedOperationAfter,
                                       ReplacementType type) {
        super(before, after, type);
        this.invokedOperationBefore = invokedOperationBefore;
        this.invokedOperationAfter = invokedOperationAfter;
    }

    public OperationInvocation getInvokedOperationBefore() {
        return invokedOperationBefore;
    }

    public OperationInvocation getInvokedOperationAfter() {
        return invokedOperationAfter;
    }

    public boolean differentExpressionNameAndArguments() {
        return invokedOperationBefore.differentExpressionNameAndArguments(invokedOperationAfter);
    }
}