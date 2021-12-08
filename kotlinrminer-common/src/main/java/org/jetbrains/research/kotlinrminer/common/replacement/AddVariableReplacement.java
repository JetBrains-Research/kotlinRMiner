package org.jetbrains.research.kotlinrminer.common.replacement;

import java.util.Set;

public class AddVariableReplacement extends Replacement {

    private final Set<String> addedVariables;

    public AddVariableReplacement(Set<String> addedVariables) {
        super("", addedVariables.toString(), ReplacementType.ADD_VARIABLE);
        this.addedVariables = addedVariables;
    }

    public Set<String> getAddedVariables() {
        return addedVariables;
    }
}