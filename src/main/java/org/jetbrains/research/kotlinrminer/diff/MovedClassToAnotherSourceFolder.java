package org.jetbrains.research.kotlinrminer.diff;

import org.jetbrains.research.kotlinrminer.uml.UMLClass;
import org.jetbrains.research.kotlinrminer.util.PrefixSuffixUtils;

public class MovedClassToAnotherSourceFolder {
    private final UMLClass originalClass;
    private final UMLClass movedClass;
    private final String originalPath;
    private final String movedPath;

    public MovedClassToAnotherSourceFolder(UMLClass originalClass, UMLClass movedClass,
                                           String originalPath, String movedPath) {
        this.originalClass = originalClass;
        this.movedClass = movedClass;
        this.originalPath = originalPath;
        this.movedPath = movedPath;
    }

    public String getOriginalClassName() {
        return originalClass.getQualifiedName();
    }

    public String getMovedClassName() {
        return movedClass.getQualifiedName();
    }

    public UMLClass getOriginalClass() {
        return originalClass;
    }

    public UMLClass getMovedClass() {
        return movedClass;
    }

    public RenamePattern getRenamePattern() {
        int separatorPos = PrefixSuffixUtils.separatorPosOfCommonSuffix('/', originalPath, movedPath);
        if (separatorPos == -1) {
            return new RenamePattern(originalPath, movedPath);
        }
        String original = originalPath.substring(0, originalPath.length() - separatorPos);
        String moved = movedPath.substring(0, movedPath.length() - separatorPos);
        return new RenamePattern(original, moved);
    }
}
