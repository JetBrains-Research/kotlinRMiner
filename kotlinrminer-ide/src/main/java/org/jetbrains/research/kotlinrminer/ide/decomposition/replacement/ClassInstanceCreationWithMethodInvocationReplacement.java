package org.jetbrains.research.kotlinrminer.ide.decomposition.replacement;

import org.jetbrains.research.kotlinrminer.common.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.ide.decomposition.ObjectCreation;
import org.jetbrains.research.kotlinrminer.ide.decomposition.OperationInvocation;

public class ClassInstanceCreationWithMethodInvocationReplacement extends Replacement {
    private final ObjectCreation objectCreationBefore;
    private final OperationInvocation invokedOperationAfter;

    public ClassInstanceCreationWithMethodInvocationReplacement(String before,
                                                                String after,
                                                                Replacement.ReplacementType type,
                                                                ObjectCreation objectCreationBefore,
                                                                OperationInvocation invokedOperationAfter) {
        super(before, after, type);
        this.objectCreationBefore = objectCreationBefore;
        this.invokedOperationAfter = invokedOperationAfter;
    }

    public ObjectCreation getObjectCreationBefore() {
        return objectCreationBefore;
    }

    public OperationInvocation getInvokedOperationAfter() {
        return invokedOperationAfter;
    }

}
