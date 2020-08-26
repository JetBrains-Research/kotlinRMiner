package org.jetbrains.research.kotlinrminer.decomposition.replacement;

import org.jetbrains.research.kotlinrminer.decomposition.AbstractCodeFragment;

import java.util.Set;

public class CompositeReplacement extends Replacement {
    private final Set<AbstractCodeFragment> additionallyMatchedStatements1;
    private final Set<AbstractCodeFragment> additionallyMatchedStatements2;

    public CompositeReplacement(String before,
                                String after,
                                Set<AbstractCodeFragment> additionallyMatchedStatements1,
                                Set<AbstractCodeFragment> additionallyMatchedStatements2) {
        super(before, after, ReplacementType.COMPOSITE);
        this.additionallyMatchedStatements1 = additionallyMatchedStatements1;
        this.additionallyMatchedStatements2 = additionallyMatchedStatements2;
    }

    public Set<AbstractCodeFragment> getAdditionallyMatchedStatements1() {
        return additionallyMatchedStatements1;
    }

    public Set<AbstractCodeFragment> getAdditionallyMatchedStatements2() {
        return additionallyMatchedStatements2;
    }
}