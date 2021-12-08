package org.jetbrains.research.kotlinrminer.cli.decomposition;

import org.jetbrains.research.kotlinrminer.cli.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.cli.uml.UMLType;

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
        return this.toQualifiedString();
    }

    @Override
    public String toQualifiedString() {
        StringBuilder s = new StringBuilder("(");
        for (int i = 0; i < parametersList.size(); i++) {
            UMLType parameter = parametersList.get(i);
            s.append(parameter.toQualifiedString());
            if (i < parametersList.size() - 1) {
                s.append(",");
            }
        }
        s.append(")").append(" -> ").append(returnType.toString());
        return s.toString();
    }

    @Override
    public String getClassType() {
        return returnType.getClassType();
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
