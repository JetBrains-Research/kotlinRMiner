package org.jetbrains.research.kotlinrminer.core.decomposition.replacement;

import java.util.LinkedHashSet;
import java.util.Set;

public class SplitVariableReplacement extends Replacement {

    private final Set<String> splitVariables;


    public SplitVariableReplacement(String oldVariable, Set<String> newVariables) {
        super(oldVariable, newVariables.toString(), ReplacementType.SPLIT_VARIABLE);
        this.splitVariables = newVariables;
    }

    public Set<String> getSplitVariables() {
        return splitVariables;
    }

    public boolean equal(SplitVariableReplacement other) {
        return this.getBefore().equals(other.getBefore()) &&
                this.splitVariables.containsAll(other.splitVariables) &&
                other.splitVariables.containsAll(this.splitVariables);
    }

    public boolean commonBefore(SplitVariableReplacement other) {
        Set<String> intersection = new LinkedHashSet<>(this.splitVariables);
        intersection.retainAll(other.splitVariables);
        return this.getBefore().equals(other.getBefore()) && intersection.size() == 0;
    }

    public boolean subsumes(SplitVariableReplacement other) {
        return this.getBefore().equals(other.getBefore()) &&
                this.splitVariables.containsAll(other.splitVariables) &&
                this.splitVariables.size() > other.splitVariables.size();
    }
}
