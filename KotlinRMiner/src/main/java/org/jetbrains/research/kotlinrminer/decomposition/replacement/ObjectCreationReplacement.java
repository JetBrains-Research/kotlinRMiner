package org.jetbrains.research.kotlinrminer.decomposition.replacement;

import org.jetbrains.research.kotlinrminer.decomposition.ObjectCreation;

public class ObjectCreationReplacement extends Replacement {
    private final ObjectCreation createdObjectBefore;
    private final ObjectCreation createdObjectAfter;

    public ObjectCreationReplacement(String before, String after,
                                     ObjectCreation createdObjectBefore, ObjectCreation createdObjectAfter,
                                     ReplacementType type) {
        super(before, after, type);
        this.createdObjectBefore = createdObjectBefore;
        this.createdObjectAfter = createdObjectAfter;
    }

    public ObjectCreation getCreatedObjectBefore() {
        return createdObjectBefore;
    }

    public ObjectCreation getCreatedObjectAfter() {
        return createdObjectAfter;
    }

}