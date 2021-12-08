package org.jetbrains.research.kotlinrminer.ide.uml;

import java.util.ArrayList;
import java.util.List;

public class UMLJavadoc {
    private final List<UMLTagElement> tags;

    public UMLJavadoc() {
        this.tags = new ArrayList<>();
    }

    public void addTag(UMLTagElement tag) {
        tags.add(tag);
    }

    public List<UMLTagElement> getTags() {
        return tags;
    }

    public boolean contains(String s) {
        for (UMLTagElement tag : tags) {
            if (tag.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsIgnoreCase(String s) {
        for (UMLTagElement tag : tags) {
            if (tag.containsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
}