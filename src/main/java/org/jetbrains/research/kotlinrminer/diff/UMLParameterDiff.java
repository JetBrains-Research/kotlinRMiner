package org.jetbrains.research.kotlinrminer.diff;

import org.jetbrains.research.kotlinrminer.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.uml.UMLParameter;

import java.util.Set;

public class UMLParameterDiff {
    private final UMLParameter removedParameter;
    private final UMLParameter addedParameter;
    private boolean typeChanged;
    private boolean qualifiedTypeChanged;
    private boolean nameChanged;

    public UMLParameterDiff(UMLParameter removedParameter, UMLParameter addedParameter,
                            UMLOperation removedOperation, UMLOperation addedOperation,
                            Set<AbstractCodeMapping> mappings) {
        this.removedParameter = removedParameter;
        this.addedParameter = addedParameter;
        this.typeChanged = false;
        this.nameChanged = false;
        if (!removedParameter.getType().equals(addedParameter.getType()))
            typeChanged = true;
        else if (!removedParameter.getType().equalsQualified(addedParameter.getType()))
            qualifiedTypeChanged = true;
        if (!removedParameter.getName().equals(addedParameter.getName()))
            nameChanged = true;
    }

    public UMLParameter getRemovedParameter() {
        return removedParameter;
    }

    public UMLParameter getAddedParameter() {
        return addedParameter;
    }

    public boolean isTypeChanged() {
        return typeChanged;
    }

    public boolean isQualifiedTypeChanged() {
        return qualifiedTypeChanged;
    }

    public boolean isNameChanged() {
        return nameChanged;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (typeChanged || nameChanged || qualifiedTypeChanged)
            sb.append("\t\t").append("parameter ").append(removedParameter).append(":").append("\n");
        if (typeChanged || qualifiedTypeChanged)
            sb.append("\t\t").append("type changed from ").append(removedParameter.getType()).append(" to ").append(
                    addedParameter.getType()).append("\n");
        if (nameChanged)
            sb.append("\t\t").append("name changed from ").append(removedParameter.getName()).append(" to ").append(
                    addedParameter.getName()).append("\n");
        return sb.toString();
    }
}
