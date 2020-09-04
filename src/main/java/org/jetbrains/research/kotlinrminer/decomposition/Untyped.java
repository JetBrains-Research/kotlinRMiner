package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.research.kotlinrminer.uml.UMLType;

/**
 * It's used when a property/variable/method's return valued type isn't defined.
 */
public class Untyped extends UMLType {
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "Untyped";
    }

    @Override
    public String toQualifiedString() {
        return "Untyped";
    }

    @Override
    public String getClassType() {
        return "Untyped";
    }
}
