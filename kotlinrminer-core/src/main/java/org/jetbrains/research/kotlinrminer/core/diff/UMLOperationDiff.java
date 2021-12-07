package org.jetbrains.research.kotlinrminer.core.diff;

import org.jetbrains.research.kotlinrminer.core.Refactoring;
import org.jetbrains.research.kotlinrminer.core.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.core.diff.refactoring.*;
import org.jetbrains.research.kotlinrminer.core.uml.UMLAnnotation;
import org.jetbrains.research.kotlinrminer.core.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.core.uml.UMLParameter;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;

public class UMLOperationDiff {
    private final UMLOperation removedOperation;
    private final UMLOperation addedOperation;
    private final List<UMLParameter> addedParameters;
    private final List<UMLParameter> removedParameters;
    private final List<UMLParameterDiff> parameterDiffList;
    private final UMLAnnotationListDiff annotationListDiff;
    private boolean visibilityChanged;
    private boolean abstractionChanged;
    private boolean returnTypeChanged;
    private boolean qualifiedReturnTypeChanged;
    private final boolean operationRenamed;
    private boolean parametersReordered;
    private Set<AbstractCodeMapping> mappings = new LinkedHashSet<>();

    public UMLOperationDiff(UMLOperation removedOperation, UMLOperation addedOperation) {
        this.removedOperation = removedOperation;
        this.addedOperation = addedOperation;
        this.addedParameters = new ArrayList<>();
        this.removedParameters = new ArrayList<>();
        this.parameterDiffList = new ArrayList<>();
        this.visibilityChanged = false;
        this.abstractionChanged = false;
        this.returnTypeChanged = false;
        operationRenamed = !removedOperation.getName().equals(addedOperation.getName());
        if (!removedOperation.getVisibility().equals(addedOperation.getVisibility())) {
            visibilityChanged = true;
        }
        if (removedOperation.isAbstract() != addedOperation.isAbstract()) {
            abstractionChanged = true;
        }
        if (!removedOperation.equalReturnParameter(addedOperation)) {
            returnTypeChanged = true;
        } else if (!removedOperation.equalQualifiedReturnParameter(addedOperation)) {
            qualifiedReturnTypeChanged = true;
        }
        this.annotationListDiff =
            new UMLAnnotationListDiff(removedOperation.getAnnotations(), addedOperation.getAnnotations());
        List<SimpleEntry<UMLParameter, UMLParameter>>
            matchedParameters = updateAddedRemovedParameters(removedOperation, addedOperation);
        for (SimpleEntry<UMLParameter, UMLParameter> matchedParameter : matchedParameters) {
            UMLParameter parameter1 = matchedParameter.getKey();
            UMLParameter parameter2 = matchedParameter.getValue();
            UMLParameterDiff parameterDiff =
                new UMLParameterDiff(parameter1, parameter2, removedOperation, addedOperation, mappings);
            parameterDiffList.add(parameterDiff);
        }
        int matchedParameterCount = matchedParameters.size() / 2;
        List<String> parameterNames1 = removedOperation.getParameterNameList();
        List<String> parameterNames2 = addedOperation.getParameterNameList();
        if (removedParameters.isEmpty() && addedParameters.isEmpty() &&
            matchedParameterCount == parameterNames1.size() && matchedParameterCount == parameterNames2.size() &&
            parameterNames1.size() > 1 && !parameterNames1.equals(parameterNames2)) {
            parametersReordered = true;
        }
        //first round match parameters with the same name
        for (Iterator<UMLParameter> removedParameterIterator =
             removedParameters.iterator(); removedParameterIterator.hasNext(); ) {
            UMLParameter removedParameter = removedParameterIterator.next();
            for (Iterator<UMLParameter> addedParameterIterator =
                 addedParameters.iterator(); addedParameterIterator.hasNext(); ) {
                UMLParameter addedParameter = addedParameterIterator.next();
                if (removedParameter.getName().equals(addedParameter.getName())) {
                    UMLParameterDiff parameterDiff =
                        new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation,
                            mappings);
                    parameterDiffList.add(parameterDiff);
                    addedParameterIterator.remove();
                    removedParameterIterator.remove();
                    break;
                }
            }
        }
        //second round match parameters with the same type
        for (Iterator<UMLParameter> removedParameterIterator =
             removedParameters.iterator(); removedParameterIterator.hasNext(); ) {
            UMLParameter removedParameter = removedParameterIterator.next();
            for (Iterator<UMLParameter> addedParameterIterator =
                 addedParameters.iterator(); addedParameterIterator.hasNext(); ) {
                UMLParameter addedParameter = addedParameterIterator.next();
                if (removedParameter.getType().equalsQualified(addedParameter.getType()) &&
                    !existsAnotherAddedParameterWithTheSameType(addedParameter)) {
                    UMLParameterDiff parameterDiff =
                        new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation,
                            mappings);
                    parameterDiffList.add(parameterDiff);
                    addedParameterIterator.remove();
                    removedParameterIterator.remove();
                    break;
                }
            }
        }
        //third round match parameters with different type and name
        List<UMLParameter> removedParametersWithoutReturnType = removedOperation.getParametersWithoutReturnType();
        List<UMLParameter> addedParametersWithoutReturnType = addedOperation.getParametersWithoutReturnType();
        if (matchedParameterCount == removedParametersWithoutReturnType.size() - 1 &&
            matchedParameterCount == addedParametersWithoutReturnType.size() - 1) {
            for (Iterator<UMLParameter> removedParameterIterator =
                 removedParameters.iterator(); removedParameterIterator.hasNext(); ) {
                UMLParameter removedParameter = removedParameterIterator.next();
                int indexOfRemovedParameter = removedParametersWithoutReturnType.indexOf(removedParameter);
                for (Iterator<UMLParameter> addedParameterIterator =
                     addedParameters.iterator(); addedParameterIterator.hasNext(); ) {
                    UMLParameter addedParameter = addedParameterIterator.next();
                    int indexOfAddedParameter = addedParametersWithoutReturnType.indexOf(addedParameter);
                    if (indexOfRemovedParameter == indexOfAddedParameter) {
                        UMLParameterDiff parameterDiff =
                            new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation,
                                mappings);
                        parameterDiffList.add(parameterDiff);
                        addedParameterIterator.remove();
                        removedParameterIterator.remove();
                        break;
                    }
                }
            }
        }
    }

    public UMLOperationDiff(UMLOperation removedOperation,
                            UMLOperation addedOperation,
                            Set<AbstractCodeMapping> mappings) {
        this(removedOperation, addedOperation);
        this.mappings = mappings;
    }

    private boolean existsAnotherAddedParameterWithTheSameType(UMLParameter parameter) {
        if (removedOperation.hasTwoParametersWithTheSameType() && addedOperation.hasTwoParametersWithTheSameType()) {
            return false;
        }
        for (UMLParameter addedParameter : addedParameters) {
            if (!addedParameter.getName().equals(parameter.getName()) &&
                addedParameter.getType().equalsQualified(parameter.getType())) {
                return true;
            }
        }
        return false;
    }

    private List<SimpleEntry<UMLParameter, UMLParameter>> updateAddedRemovedParameters(UMLOperation removedOperation,
                                                                                       UMLOperation addedOperation) {
        List<SimpleEntry<UMLParameter, UMLParameter>> matchedParameters = new ArrayList<>();
        for (UMLParameter parameter1 : removedOperation.getParameters()) {
            if (!parameter1.getKind().equals("return")) {
                boolean found = false;
                for (UMLParameter parameter2 : addedOperation.getParameters()) {
                    if (parameter1.equalsIncludingName(parameter2)) {
                        matchedParameters.add(new SimpleEntry<>(parameter1, parameter2));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    this.removedParameters.add(parameter1);
                }
            }
        }
        for (UMLParameter parameter1 : addedOperation.getParameters()) {
            if (!parameter1.getKind().equals("return")) {
                boolean found = false;
                for (UMLParameter parameter2 : removedOperation.getParameters()) {
                    if (parameter1.equalsIncludingName(parameter2)) {
                        matchedParameters.add(new SimpleEntry<>(parameter2, parameter1));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    this.addedParameters.add(parameter1);
                }
            }
        }
        return matchedParameters;
    }

    public List<UMLParameterDiff> getParameterDiffList() {
        return parameterDiffList;
    }

    public UMLOperation getRemovedOperation() {
        return removedOperation;
    }

    public UMLOperation getAddedOperation() {
        return addedOperation;
    }

    public List<UMLParameter> getAddedParameters() {
        return addedParameters;
    }

    public List<UMLParameter> getRemovedParameters() {
        return removedParameters;
    }

    public boolean isOperationRenamed() {
        return operationRenamed;
    }

    public boolean isEmpty() {
        return addedParameters.isEmpty() && removedParameters.isEmpty() && parameterDiffList.isEmpty() &&
            !visibilityChanged && !abstractionChanged && !returnTypeChanged && !operationRenamed &&
            annotationListDiff.isEmpty();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty()) {
            sb.append("\t").append(removedOperation).append("\n");
        }
        if (operationRenamed) {
            sb.append("\t").append("renamed from ").append(removedOperation.getName()).append(" to ").append(
                addedOperation.getName()).append("\n");
        }
        if (visibilityChanged) {
            sb.append("\t").append("visibility changed from ").append(removedOperation.getVisibility()).append(
                " to ").append(addedOperation.getVisibility()).append(
                "\n");
        }
        if (abstractionChanged) {
            sb.append("\t").append("abstraction changed from ").append(
                removedOperation.isAbstract() ? "abstract" : "concrete").append(" to ").append(
                addedOperation.isAbstract() ? "abstract" : "concrete").append("\n");
        }
        if (returnTypeChanged || qualifiedReturnTypeChanged) {
            sb.append("\t").append("return type changed from ").append(removedOperation.getReturnParameter()).append(
                " to ").append(addedOperation.getReturnParameter()).append(
                "\n");
        }
        for (UMLParameter umlParameter : removedParameters) {
            sb.append("\t").append("parameter ").append(umlParameter).append(" removed").append("\n");
        }
        for (UMLParameter umlParameter : addedParameters) {
            sb.append("\t").append("parameter ").append(umlParameter).append(" added").append("\n");
        }
        for (UMLParameterDiff parameterDiff : parameterDiffList) {
            sb.append(parameterDiff);
        }
        for (UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
            sb.append("\t").append("annotation ").append(annotation).append(" removed").append("\n");
        }
        for (UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
            sb.append("\t").append("annotation ").append(annotation).append(" added").append("\n");
        }
        for (UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
            sb.append("\t").append("annotation ").append(annotationDiff.getRemovedAnnotation()).append(
                " modified to ").append(annotationDiff.getAddedAnnotation()).append(
                "\n");
        }
        return sb.toString();
    }

    public Set<Refactoring> getRefactorings() {
        Set<Refactoring> refactorings = new LinkedHashSet<>();
        for (UMLParameterDiff parameterDiff : getParameterDiffList()) {
            refactorings.addAll(parameterDiff.getRefactorings());
        }
        if (removedParameters.isEmpty()) {
            for (UMLParameter umlParameter : addedParameters) {
                AddParameterRefactoring
                    refactoring = new AddParameterRefactoring(umlParameter, removedOperation, addedOperation);
                refactorings.add(refactoring);
            }
        }
        if (addedParameters.isEmpty()) {
            for (UMLParameter umlParameter : removedParameters) {
                RemoveParameterRefactoring
                    refactoring = new RemoveParameterRefactoring(umlParameter, removedOperation, addedOperation);
                refactorings.add(refactoring);
            }
        }
        if (parametersReordered) {
            ReorderParameterRefactoring refactoring = new ReorderParameterRefactoring(removedOperation, addedOperation);
            refactorings.add(refactoring);
        }
        for (UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
            AddMethodAnnotationRefactoring
                refactoring = new AddMethodAnnotationRefactoring(annotation, removedOperation, addedOperation);
            refactorings.add(refactoring);
        }
        for (UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveMethodAnnotationRefactoring
                refactoring = new RemoveMethodAnnotationRefactoring(annotation, removedOperation, addedOperation);
            refactorings.add(refactoring);
        }
        for (UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
            ModifyMethodAnnotationRefactoring refactoring =
                new ModifyMethodAnnotationRefactoring(annotationDiff.getRemovedAnnotation(),
                    annotationDiff.getAddedAnnotation(), removedOperation,
                    addedOperation);
            refactorings.add(refactoring);
        }
        return refactorings;
    }
}
