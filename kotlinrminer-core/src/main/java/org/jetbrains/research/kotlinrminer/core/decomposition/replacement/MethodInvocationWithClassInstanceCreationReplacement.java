package org.jetbrains.research.kotlinrminer.core.decomposition.replacement;

import org.jetbrains.research.kotlinrminer.core.decomposition.ObjectCreation;
import org.jetbrains.research.kotlinrminer.core.decomposition.OperationInvocation;

public class MethodInvocationWithClassInstanceCreationReplacement extends Replacement {
    private final OperationInvocation invokedOperationBefore;
    private final ObjectCreation objectCreationAfter;

    public MethodInvocationWithClassInstanceCreationReplacement(String before,
                                                                String after,
                                                                ReplacementType type,
                                                                OperationInvocation invokedOperationBefore,
                                                                ObjectCreation objectCreationAfter) {
        super(before, after, type);
        this.invokedOperationBefore = invokedOperationBefore;
        this.objectCreationAfter = objectCreationAfter;
    }

    public OperationInvocation getInvokedOperationBefore() {
        return invokedOperationBefore;
    }

    public ObjectCreation getObjectCreationAfter() {
        return objectCreationAfter;
    }

}
