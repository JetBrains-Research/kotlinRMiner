package org.jetbrains.research.kotlinrminer.diff;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.research.kotlinrminer.decomposition.AbstractExpression;
import org.jetbrains.research.kotlinrminer.uml.UMLAnnotation;

public class UMLAnnotationDiff {
    private final UMLAnnotation removedAnnotation;
    private final UMLAnnotation addedAnnotation;
    private boolean typeNameChanged = false;
    private boolean valueChanged = false;
    private boolean valueRemoved = false;
    private boolean valueAdded = false;
    private final List<AbstractMap.SimpleEntry<String, AbstractExpression>> removedMemberValuePairs;
    private final List<AbstractMap.SimpleEntry<String, AbstractExpression>> addedMemberValuePairs;
    private final Map<AbstractMap.SimpleEntry<String, AbstractExpression>, AbstractMap.SimpleEntry<String, AbstractExpression>>
            matchedMemberValuePairsWithDifferentExpressions;

    public UMLAnnotationDiff(UMLAnnotation removedAnnotation, UMLAnnotation addedAnnotation) {
        this.removedAnnotation = removedAnnotation;
        this.addedAnnotation = addedAnnotation;
        this.removedMemberValuePairs = new ArrayList<>();
        this.addedMemberValuePairs = new ArrayList<>();
        this.matchedMemberValuePairsWithDifferentExpressions = new LinkedHashMap<>();
        Map<AbstractMap.SimpleEntry<String, AbstractExpression>, AbstractMap.SimpleEntry<String, AbstractExpression>>
                matchedMemberValuePairs =
                new LinkedHashMap<>();
        if (!removedAnnotation.getTypeName().equals(addedAnnotation.getTypeName())) {
            typeNameChanged = true;
        }
        AbstractExpression value1 = removedAnnotation.getValue();
        AbstractExpression value2 = addedAnnotation.getValue();
        if (value1 != null && value2 != null) {
            if (!value1.getExpression().equals(value2.getExpression())) {
                valueChanged = true;
            }
        } else if (value1 != null) {
            valueRemoved = true;
        } else if (value2 != null) {
            valueAdded = true;
        }
        Map<String, AbstractExpression> memberValuePairs1 = removedAnnotation.getMemberValuePairs();
        Map<String, AbstractExpression> memberValuePairs2 = addedAnnotation.getMemberValuePairs();
        if (!memberValuePairs1.isEmpty() || !memberValuePairs2.isEmpty()) {
            for (String key1 : memberValuePairs1.keySet()) {
                if (memberValuePairs2.containsKey(key1)) {
                    matchedMemberValuePairs.put(new AbstractMap.SimpleEntry<>(key1, memberValuePairs1.get(key1)),
                                                new AbstractMap.SimpleEntry<>(key1, memberValuePairs2.get(key1)));
                } else {
                    removedMemberValuePairs.add(new AbstractMap.SimpleEntry<>(key1, memberValuePairs1.get(key1)));
                }
            }
            for (String key2 : memberValuePairs2.keySet()) {
                if (memberValuePairs1.containsKey(key2)) {
                    matchedMemberValuePairs.put(new AbstractMap.SimpleEntry<>(key2, memberValuePairs1.get(key2)),
                                                new AbstractMap.SimpleEntry<>(key2, memberValuePairs2.get(key2)));
                } else {
                    addedMemberValuePairs.add(new AbstractMap.SimpleEntry<>(key2, memberValuePairs2.get(key2)));
                }
            }
        }
        for (AbstractMap.SimpleEntry<String, AbstractExpression> key : matchedMemberValuePairs.keySet()) {
            AbstractMap.SimpleEntry<String, AbstractExpression> value = matchedMemberValuePairs.get(key);
            if (!key.getValue().getExpression().equals(value.getValue().getExpression())) {
                matchedMemberValuePairsWithDifferentExpressions.put(key, value);
            }
        }
    }

    public UMLAnnotation getRemovedAnnotation() {
        return removedAnnotation;
    }

    public UMLAnnotation getAddedAnnotation() {
        return addedAnnotation;
    }

    public boolean isEmpty() {
        return !typeNameChanged && !valueChanged && !valueAdded && !valueRemoved &&
                removedMemberValuePairs.isEmpty() && addedMemberValuePairs.isEmpty() &&
                matchedMemberValuePairsWithDifferentExpressions.isEmpty();
    }
}
