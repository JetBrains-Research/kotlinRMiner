package org.jetbrains.research.kotlinrminer.core.diff;

import org.jetbrains.research.kotlinrminer.core.Refactoring;
import org.jetbrains.research.kotlinrminer.core.decomposition.UMLOperationBodyMapper;
import org.jetbrains.research.kotlinrminer.core.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.core.decomposition.VariableReferenceExtractor;
import org.jetbrains.research.kotlinrminer.core.diff.refactoring.CandidateAttributeRefactoring;
import org.jetbrains.research.kotlinrminer.core.diff.refactoring.ChangeAttributeTypeRefactoring;
import org.jetbrains.research.kotlinrminer.core.diff.refactoring.RenameAttributeRefactoring;
import org.jetbrains.research.kotlinrminer.core.uml.UMLAnnotation;
import org.jetbrains.research.kotlinrminer.core.uml.UMLAttribute;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UMLAttributeDiff {
    private final UMLAttribute removedAttribute;
    private final UMLAttribute addedAttribute;
    private boolean visibilityChanged;
    private boolean typeChanged;
    private boolean qualifiedTypeChanged;
    private boolean renamed;
    private boolean staticChanged;
    private boolean finalChanged;
    private final List<UMLOperationBodyMapper> operationBodyMapperList;
    private final UMLAnnotationListDiff annotationListDiff;

    public UMLAttributeDiff(UMLAttribute removedAttribute,
                            UMLAttribute addedAttribute,
                            List<UMLOperationBodyMapper> operationBodyMapperList) {
        this.removedAttribute = removedAttribute;
        this.addedAttribute = addedAttribute;
        this.operationBodyMapperList = operationBodyMapperList;
        this.visibilityChanged = false;
        this.typeChanged = false;
        this.renamed = false;
        this.staticChanged = false;
        this.finalChanged = false;
        if (!removedAttribute.getName().equals(addedAttribute.getName()))
            renamed = true;
        if (!removedAttribute.getVisibility().equals(addedAttribute.getVisibility()))
            visibilityChanged = true;
        if (!removedAttribute.getType().equals(addedAttribute.getType()))
            typeChanged = true;
        else if (!removedAttribute.getType().equalsQualified(addedAttribute.getType()))
            qualifiedTypeChanged = true;
        if (removedAttribute.isStatic() != addedAttribute.isStatic())
            staticChanged = true;
        if (removedAttribute.isFinal() != addedAttribute.isFinal())
            finalChanged = true;
        this.annotationListDiff =
            new UMLAnnotationListDiff(removedAttribute.getAnnotations(), addedAttribute.getAnnotations());
    }

    public UMLAttribute getRemovedAttribute() {
        return removedAttribute;
    }

    public UMLAttribute getAddedAttribute() {
        return addedAttribute;
    }

    public boolean isRenamed() {
        return renamed;
    }

    public boolean isVisibilityChanged() {
        return visibilityChanged;
    }

    public boolean isTypeChanged() {
        return typeChanged;
    }

    public boolean isQualifiedTypeChanged() {
        return qualifiedTypeChanged;
    }

    public boolean isEmpty() {
        return !visibilityChanged && !typeChanged && !renamed && !qualifiedTypeChanged && annotationListDiff.isEmpty();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty())
            sb.append("\t").append(removedAttribute).append("\n");
        if (renamed)
            sb.append("\t").append(
                "renamed from " + removedAttribute.getName() + " to " + addedAttribute.getName()).append("\n");
        if (visibilityChanged)
            sb.append("\t").append(
                "visibility changed from " + removedAttribute.getVisibility() + " to " + addedAttribute.getVisibility()).append(
                "\n");
        if (typeChanged || qualifiedTypeChanged)
            sb.append("\t").append(
                "type changed from " + removedAttribute.getType() + " to " + addedAttribute.getType()).append("\n");
        if (staticChanged)
            sb.append("\t").append(
                "modifier changed from " + (removedAttribute.isStatic() ? "static" : "non-static") + " to " +
                    (addedAttribute.isStatic() ? "static" : "non-static")).append("\n");
        if (finalChanged)
            sb.append("\t").append(
                "modifier changed from " + (removedAttribute.isFinal() ? "final" : "non-final") + " to " +
                    (addedAttribute.isFinal() ? "final" : "non-final")).append("\n");
        for (UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
            sb.append("\t").append("annotation " + annotation + " removed").append("\n");
        }
        for (UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
            sb.append("\t").append("annotation " + annotation + " added").append("\n");
        }
        for (UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
            sb.append("\t").append(
                "annotation " + annotationDiff.getRemovedAnnotation() + " modified to " + annotationDiff.getAddedAnnotation()).append(
                "\n");
        }
        return sb.toString();
    }

    private Set<Refactoring> getAnnotationRefactorings() {
        Set<Refactoring> refactorings = new LinkedHashSet<>();
        for (UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
/*            AddAttributeAnnotationRefactoring refactoring =
                new AddAttributeAnnotationRefactoring(annotation, removedAttribute, addedAttribute);
            refactorings.add(refactoring);
        }
        for (UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveAttributeAnnotationRefactoring refactoring =
                new RemoveAttributeAnnotationRefactoring(annotation, removedAttribute, addedAttribute);
            refactorings.add(refactoring);
        }
        for (UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
            ModifyAttributeAnnotationRefactoring refactoring =
                new ModifyAttributeAnnotationRefactoring(annotationDiff.getRemovedAnnotation(),
                                                         annotationDiff.getAddedAnnotation(), removedAttribute,
                                                         addedAttribute);
            refactorings.add(refactoring);*/
        }
        return refactorings;
    }

    public Set<Refactoring> getRefactorings() {
        Set<Refactoring> refactorings = new LinkedHashSet<>();
        if (changeTypeCondition()) {
            ChangeAttributeTypeRefactoring ref = new ChangeAttributeTypeRefactoring(removedAttribute, addedAttribute,
                                                                                    VariableReferenceExtractor.findReferences(
                                                                                        removedAttribute.getVariableDeclaration(),
                                                                                        addedAttribute.getVariableDeclaration(),
                                                                                        operationBodyMapperList));
            refactorings.add(ref);
        }
        refactorings.addAll(getAnnotationRefactorings());
        return refactorings;
    }

    public Set<Refactoring> getRefactorings(Set<CandidateAttributeRefactoring> set) {
        Set<Refactoring> refactorings = new LinkedHashSet<>();
        RenameAttributeRefactoring rename = null;
        if (isRenamed()) {
            rename = new RenameAttributeRefactoring(removedAttribute, addedAttribute, set);
            refactorings.add(rename);
        }
        if (changeTypeCondition()) {
            ChangeAttributeTypeRefactoring ref = new ChangeAttributeTypeRefactoring(removedAttribute, addedAttribute,
                                                                                    VariableReferenceExtractor.findReferences(
                                                                                        removedAttribute.getVariableDeclaration(),
                                                                                        addedAttribute.getVariableDeclaration(),
                                                                                        operationBodyMapperList));
            refactorings.add(ref);
            if (rename != null) {
                ref.addRelatedRefactoring(rename);
            }
        }
        refactorings.addAll(getAnnotationRefactorings());
        return refactorings;
    }

    private boolean changeTypeCondition() {
        return (isTypeChanged() || isQualifiedTypeChanged()) && !enumConstantsDeclaredInTheSameEnumDeclarationType();
    }

    private boolean enumConstantsDeclaredInTheSameEnumDeclarationType() {
        VariableDeclaration removedVariableDeclaration = removedAttribute.getVariableDeclaration();
        VariableDeclaration addedVariableDeclaration = addedAttribute.getVariableDeclaration();
        return removedVariableDeclaration.isEnumConstant() && addedVariableDeclaration.isEnumConstant() &&
            removedVariableDeclaration.getType().equals(addedVariableDeclaration.getType());
    }
}
