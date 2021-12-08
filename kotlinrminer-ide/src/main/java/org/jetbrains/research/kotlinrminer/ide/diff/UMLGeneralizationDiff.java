package org.jetbrains.research.kotlinrminer.ide.diff;

import org.jetbrains.research.kotlinrminer.ide.uml.UMLGeneralization;

public class UMLGeneralizationDiff implements Comparable<UMLGeneralizationDiff> {
    private final UMLGeneralization removedGeneralization;
    private final UMLGeneralization addedGeneralization;
    private boolean parentChanged;
    private boolean childChanged;

    public UMLGeneralizationDiff(UMLGeneralization removedGeneralization, UMLGeneralization addedGeneralization) {
        this.removedGeneralization = removedGeneralization;
        this.addedGeneralization = addedGeneralization;
        this.parentChanged = false;
        this.childChanged = false;
        if (!removedGeneralization.getParent().equals(addedGeneralization.getParent()))
            parentChanged = true;
        if (!removedGeneralization.getChild().equals(addedGeneralization.getChild()))
            childChanged = true;
    }

    public UMLGeneralization getRemovedGeneralization() {
        return removedGeneralization;
    }

    public UMLGeneralization getAddedGeneralization() {
        return addedGeneralization;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (parentChanged || childChanged)
            sb.append("generalization ").append(removedGeneralization).append(":").append("\n");
        if (childChanged)
            sb.append("\t").append("child changed from ").append(removedGeneralization.getChild())
                .append(" to ").append(addedGeneralization.getChild()).append("\n");
        if (parentChanged)
            sb.append("\t").append("parent changed from ").append(removedGeneralization.getParent())
                .append(" to ").append(addedGeneralization.getParent()).append("\n");
        return sb.toString();
    }

    public int compareTo(UMLGeneralizationDiff generalizationDiff) {
        int compare = this.removedGeneralization.compareTo(generalizationDiff.removedGeneralization);
        if (compare == 0)
            return this.addedGeneralization.compareTo(generalizationDiff.addedGeneralization);
        else
            return compare;
    }
}
