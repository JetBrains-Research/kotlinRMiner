package org.jetbrains.research.kotlinrminer.ide.diff.refactoring;

import java.util.Set;

import org.jetbrains.research.kotlinrminer.ide.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.ide.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLAttribute;

public class CandidateMergeVariableRefactoring {
    private final Set<String> mergedVariables;
    private final String newVariable;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> variableReferences;
    private Set<UMLAttribute> mergedAttributes;
    private UMLAttribute newAttribute;

    public CandidateMergeVariableRefactoring(Set<String> mergedVariables,
                                             String newVariable,
                                             UMLOperation operationBefore,
                                             UMLOperation operationAfter,
                                             Set<AbstractCodeMapping> variableReferences) {
        this.mergedVariables = mergedVariables;
        this.newVariable = newVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.variableReferences = variableReferences;
    }

    public Set<String> getMergedVariables() {
        return mergedVariables;
    }

    public String getNewVariable() {
        return newVariable;
    }

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    public Set<AbstractCodeMapping> getVariableReferences() {
        return variableReferences;
    }

    public Set<UMLAttribute> getMergedAttributes() {
        return mergedAttributes;
    }

    public void setMergedAttributes(Set<UMLAttribute> mergedAttributes) {
        this.mergedAttributes = mergedAttributes;
    }

    public UMLAttribute getNewAttribute() {
        return newAttribute;
    }

    public void setNewAttribute(UMLAttribute newAttribute) {
        this.newAttribute = newAttribute;
    }

    public String toString() {
        return "Merge Attribute" + "\t" +
            mergedVariables +
            " to " +
            newVariable +
            " in method " +
            operationAfter +
            " in class " + operationAfter.getClassName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mergedVariables == null) ? 0 : mergedVariables.hashCode());
        result = prime * result + ((newVariable == null) ? 0 : newVariable.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CandidateMergeVariableRefactoring other = (CandidateMergeVariableRefactoring) obj;
        if (mergedVariables == null) {
            if (other.mergedVariables != null) {
                return false;
            }
        } else if (!mergedVariables.equals(other.mergedVariables)) {
            return false;
        }
        if (newVariable == null) {
            if (other.newVariable != null) {
                return false;
            }
        } else if (!newVariable.equals(other.newVariable)) {
            return false;
        }
        if (operationAfter == null) {
            if (other.operationAfter != null) {
                return false;
            }
        } else if (!operationAfter.equals(other.operationAfter)) {
            return false;
        }
        if (operationBefore == null) {
            return other.operationBefore == null;
        } else {
            return operationBefore.equals(other.operationBefore);
        }
    }
}

