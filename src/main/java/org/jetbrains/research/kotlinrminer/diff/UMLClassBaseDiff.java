package org.jetbrains.research.kotlinrminer.diff;

import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.uml.UMLClass;

import java.util.*;

public abstract class UMLClassBaseDiff implements Comparable<UMLClassBaseDiff> {
    protected UMLClass originalClass;
    protected UMLClass nextClass;
    protected List<Refactoring> refactorings;
    private UMLModelDiff modelDiff;

    public UMLClassBaseDiff(UMLClass originalClass, UMLClass nextClass, UMLModelDiff modelDiff) {
        this.originalClass = originalClass;
        this.nextClass = nextClass;
        this.refactorings = new ArrayList<>();
        this.modelDiff = modelDiff;
    }

    public String getOriginalClassName() {
        return originalClass.getName();
    }

    public String getNextClassName() {
        return nextClass.getName();
    }

    public UMLClass getOriginalClass() {
        return originalClass;
    }

    public UMLClass getNextClass() {
        return nextClass;
    }

    public boolean isInnerClassMove(UMLClassBaseDiff classDiff) {
        return this.originalClass.isInnerClass(classDiff.originalClass) && this.nextClass.isInnerClass(classDiff.nextClass);
    }

    public int compareTo(UMLClassBaseDiff other) {
        return this.originalClass.getName().compareTo(other.originalClass.getName());
    }

    public UMLModelDiff getModelDiff() {
        return modelDiff;
    }
}