package org.jetbrains.research.kotlinrminer.ide.diff;

import org.jetbrains.research.kotlinrminer.ide.uml.UMLClass;

public class UMLClassMoveDiff extends UMLClassBaseDiff {

    public UMLClassMoveDiff(UMLClass originalClass, UMLClass movedClass, UMLModelDiff modelDiff) {
        super(originalClass, movedClass, modelDiff);
    }

    public UMLClass getMovedClass() {
        return nextClass;
    }

    public String toString() {
        return "class " +
            originalClass.getQualifiedName() +
            " was moved to " +
            nextClass.getQualifiedName() +
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
