package org.jetbrains.research.kotlinrminer.diff;

import org.jetbrains.research.kotlinrminer.api.Refactoring;

import java.util.*;

public class UMLModelDiff {

    public List<Refactoring> getRefactorings() {
        Set<Refactoring> refactorings = new LinkedHashSet<>();
        //TODO: refactorings.addAll(getMoveClassRefactorings());
        return new ArrayList<>(refactorings);
    }

}
