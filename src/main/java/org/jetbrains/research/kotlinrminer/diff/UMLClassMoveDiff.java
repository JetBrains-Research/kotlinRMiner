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
        return "class " +
                originalClass.getName() +
                " was moved to " +
                nextClass.getName() +
                "\n";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof UMLClassMoveDiff) {
            UMLClassMoveDiff classMoveDiff = (UMLClassMoveDiff) o;
            return this.originalClass.equals(classMoveDiff.originalClass) && this.nextClass.equals(
                    classMoveDiff.nextClass);
        }
        return false;
    }
}
