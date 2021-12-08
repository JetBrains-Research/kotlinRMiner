package org.jetbrains.research.kotlinrminer.cli.diff;

import org.jetbrains.research.kotlinrminer.cli.uml.UMLClass;

public class UMLClassRenameDiff extends UMLClassBaseDiff {
    public UMLClassRenameDiff(UMLClass originalClass, UMLClass renamedClass, UMLModelDiff modelDiff) {
        super(originalClass, renamedClass, modelDiff);
    }

    public UMLClass getRenamedClass() {
        return nextClass;
    }

    public boolean samePackage() {
        return originalClass.getPackageName().equals(nextClass.getPackageName());
    }

    public String toString() {
        return "class " +
            originalClass.getQualifiedName() +
            " was renamed to " +
            nextClass.getQualifiedName() +
            "\n";
    }
}
