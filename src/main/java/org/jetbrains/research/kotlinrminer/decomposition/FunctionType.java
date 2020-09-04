package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.research.kotlinrminer.LocationInfo;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLType;

import java.util.List;

/**
 * Wrapper for function types in Kotlin.
 * The examples of function types:
 * (Int) -> String
 * (Int, Int) -> String
 * () -> Unit
 */
public class FunctionType extends UMLType {
    private final UMLType receiver;
    private final UMLType returnType;
    private final List<UMLType> parametersList;

    public FunctionType(UMLType receiver, UMLType returnType, List<UMLType> parametersList) {
        this.receiver = receiver;
        this.returnType = returnType;
        this.parametersList = parametersList;
    }

    @Override
    public LocationInfo getLocationInfo() {
        return null;
    }

    @Override
    public CodeRange codeRange() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof FunctionType) {
            FunctionType other = (FunctionType) o;
            return this.returnType.equals(other.returnType);
        } else return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((receiver == null) ? 0 : receiver.hashCode());
        result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "(" + receiver.toString() + ")" + "->" + returnType.toString();
    }

    @Override
    public String toQualifiedString() {
        return "(" + receiver.toString() + ")" + "->" + returnType.toString();
    }

    @Override
    public String getClassType() {
        return null;
    }

    public UMLType getReturnType() {
        return returnType;
    }

    public UMLType getReceiver() {
        return receiver;
    }

    public List<UMLType> getParametersList() {
        return parametersList;
    }
}
