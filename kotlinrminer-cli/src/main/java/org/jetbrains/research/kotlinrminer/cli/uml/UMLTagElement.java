package org.jetbrains.research.kotlinrminer.cli.uml;

import java.util.ArrayList;
import java.util.List;

public class UMLTagElement {
    private final String tagName;
    private final List<String> fragments;

    public UMLTagElement(String tagName) {
        this.tagName = tagName;
        this.fragments = new ArrayList<>();
    }

    public void addFragment(String fragment) {
        fragments.add(fragment);
    }

    public String getTagName() {
        return tagName;
    }

    public List<String> getFragments() {
        return fragments;
    }

    public boolean contains(String s) {
        for (String fragment : fragments) {
            if (fragment.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsIgnoreCase(String s) {
        for (String fragment : fragments) {
            if (fragment.toLowerCase().contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
