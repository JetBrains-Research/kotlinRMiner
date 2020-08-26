package org.jetbrains.research.kotlinrminer.decomposition.replacement;

import java.util.Set;

public class IntersectionReplacement extends Replacement {
    private final Set<String> commonElements;

    public IntersectionReplacement(String before, String after, Set<String> commonElements, ReplacementType type) {
        super(before, after, type);
        this.commonElements = commonElements;
    }

    public Set<String> getCommonElements() {
        return commonElements;
    }
}