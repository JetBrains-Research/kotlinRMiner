package org.jetbrains.research.kotlinrminer.diff;

import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.decomposition.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.decomposition.UMLOperationBodyMapper;
import org.jetbrains.research.kotlinrminer.uml.UMLAttribute;
import org.jetbrains.research.kotlinrminer.uml.UMLClass;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

import java.util.*;

public abstract class UMLClassBaseDiff implements Comparable<UMLClassBaseDiff> {
    protected UMLClass originalClass;
    protected UMLClass nextClass;
    protected List<Refactoring> refactorings;
    private UMLModelDiff modelDiff;
    protected List<UMLOperation> addedOperations;
    protected List<UMLOperation> removedOperations;
    protected List<UMLAttribute> addedAttributes;
    protected List<UMLAttribute> removedAttributes;
    private List<UMLOperationDiff> operationDiffList;
    public static final double MAX_OPERATION_NAME_DISTANCE = 0.4;

    public UMLClassBaseDiff(UMLClass originalClass, UMLClass nextClass, UMLModelDiff modelDiff) {
        this.originalClass = originalClass;
        this.nextClass = nextClass;
        this.refactorings = new ArrayList<>();
        this.addedOperations = new ArrayList<>();
        this.removedOperations = new ArrayList<>();
        this.addedAttributes = new ArrayList<>();
        this.removedAttributes = new ArrayList<>();
        this.operationDiffList = new ArrayList<>();
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

    public List<UMLOperation> getAddedOperations() {
        return addedOperations;
    }

    public List<UMLOperation> getRemovedOperations() {
        return removedOperations;
    }

    public List<UMLAttribute> getAddedAttributes() {
        return addedAttributes;
    }

    public List<UMLAttribute> getRemovedAttributes() {
        return removedAttributes;
    }

    public UMLOperationDiff getOperationDiff(UMLOperation operation1, UMLOperation operation2) {
        for (UMLOperationDiff diff : operationDiffList) {
            if (diff.getRemovedOperation().equals(operation1) && diff.getAddedOperation().equals(operation2)) {
                return diff;
            }
        }
        return null;
    }

    public boolean isInnerClassMove(UMLClassBaseDiff classDiff) {
        return this.originalClass.isInnerClass(classDiff.originalClass) && this.nextClass.isInnerClass(
                classDiff.nextClass);
    }

    public int compareTo(UMLClassBaseDiff other) {
        return this.originalClass.getName().compareTo(other.originalClass.getName());
    }

    public UMLModelDiff getModelDiff() {
        return modelDiff;
    }

    public static boolean allMappingsAreExactMatches(UMLOperationBodyMapper operationBodyMapper) {
        int mappings = operationBodyMapper.mappingsWithoutBlocks();
        int tryMappings = 0;
        int mappingsWithTypeReplacement = 0;
        for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
            if (mapping.getFragment1().getString().equals("try") && mapping.getFragment2().getString().equals("try")) {
                tryMappings++;
            }
            if (mapping.containsReplacement(Replacement.ReplacementType.TYPE)) {
                mappingsWithTypeReplacement++;
            }
        }
        if (mappings == operationBodyMapper.exactMatches() + tryMappings) {
            return true;
        }
        return mappings == operationBodyMapper.exactMatches() + tryMappings + mappingsWithTypeReplacement && mappings > mappingsWithTypeReplacement;
    }
}