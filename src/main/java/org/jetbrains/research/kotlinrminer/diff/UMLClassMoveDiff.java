package org.jetbrains.research.kotlinrminer.diff;

import org.jetbrains.research.kotlinrminer.uml.UMLClass;

public class UMLClassMoveDiff extends UMLClassBaseDiff {

    public UMLClassMoveDiff(UMLClass originalClass, UMLClass movedClass, UMLModelDiff modelDiff) {
        super(originalClass, movedClass, modelDiff);
    }

    public UMLClass getMovedClass() {
        return nextClass;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ");
        sb.append(originalClass.getName());
        sb.append(" was moved to ");
        sb.append(nextClass.getName());
        sb.append("\n");
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof UMLClassMoveDiff) {
            UMLClassMoveDiff classMoveDiff = (UMLClassMoveDiff) o;
            return this.originalClass.equals(classMoveDiff.originalClass) && this.nextClass.equals(classMoveDiff.nextClass);
        }
        return false;
    }
}
