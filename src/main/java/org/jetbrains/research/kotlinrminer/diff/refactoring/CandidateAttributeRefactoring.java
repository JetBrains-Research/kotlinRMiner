package org.jetbrains.research.kotlinrminer.diff.refactoring;

import java.util.Set;

import org.jetbrains.research.kotlinrminer.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.uml.UMLAttribute;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

public class CandidateAttributeRefactoring {
    private final String originalVariableName;
    private final String renamedVariableName;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> attributeReferences;
    private VariableDeclaration originalVariableDeclaration;
    private VariableDeclaration renamedVariableDeclaration;
    private UMLAttribute originalAttribute;
    private UMLAttribute renamedAttribute;

    public CandidateAttributeRefactoring(
            String originalVariableName,
            String renamedVariableName,
            UMLOperation operationBefore,
            UMLOperation operationAfter,
            Set<AbstractCodeMapping> attributeReferences) {
        this.originalVariableName = originalVariableName;
        this.renamedVariableName = renamedVariableName;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.attributeReferences = attributeReferences;
    }

    public String getOriginalVariableName() {
        return originalVariableName;
    }

    public String getRenamedVariableName() {
        return renamedVariableName;
    }

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    public Set<AbstractCodeMapping> getAttributeReferences() {
        return attributeReferences;
    }

    public int getOccurrences() {
        return attributeReferences.size();
    }

    public VariableDeclaration getOriginalVariableDeclaration() {
        return originalVariableDeclaration;
    }

    public void setOriginalVariableDeclaration(VariableDeclaration originalVariableDeclaration) {
        this.originalVariableDeclaration = originalVariableDeclaration;
    }

    public VariableDeclaration getRenamedVariableDeclaration() {
        return renamedVariableDeclaration;
    }

    public void setRenamedVariableDeclaration(VariableDeclaration renamedVariableDeclaration) {
        this.renamedVariableDeclaration = renamedVariableDeclaration;
    }

    public UMLAttribute getOriginalAttribute() {
        return originalAttribute;
    }

    public void setOriginalAttribute(UMLAttribute originalAttribute) {
        this.originalAttribute = originalAttribute;
    }

    public UMLAttribute getRenamedAttribute() {
        return renamedAttribute;
    }

    public void setRenamedAttribute(UMLAttribute renamedAttribute) {
        this.renamedAttribute = renamedAttribute;
    }

    public String toString() {
        return "Rename Attribute" + "\t" +
                originalVariableName +
                " to " +
                renamedVariableName +
                " in method " +
                operationAfter +
                " in class " + operationAfter.getClassName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
        result = prime * result + ((originalVariableName == null) ? 0 : originalVariableName.hashCode());
        result = prime * result + ((renamedVariableName == null) ? 0 : renamedVariableName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CandidateAttributeRefactoring other = (CandidateAttributeRefactoring) obj;
        if (operationAfter == null) {
            if (other.operationAfter != null)
                return false;
        } else if (!operationAfter.equals(other.operationAfter))
            return false;
        if (operationBefore == null) {
            if (other.operationBefore != null)
                return false;
        } else if (!operationBefore.equals(other.operationBefore))
            return false;
        if (originalVariableName == null) {
            if (other.originalVariableName != null)
                return false;
        } else if (!originalVariableName.equals(other.originalVariableName))
            return false;
        if (renamedVariableName == null) {
            return other.renamedVariableName == null;
        } else return renamedVariableName.equals(other.renamedVariableName);
    }

}
