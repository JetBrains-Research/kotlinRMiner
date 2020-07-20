package org.jetbrains.research.kotlinrminer.diff;

import org.jetbrains.research.kotlinrminer.uml.UMLClass;

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
        StringBuilder sb = new StringBuilder();
        sb.append("class ");
        sb.append(originalClass.getName());
        sb.append(" was renamed to ");
        sb.append(nextClass.getName());
        sb.append("\n");
        return sb.toString();
    }
}
