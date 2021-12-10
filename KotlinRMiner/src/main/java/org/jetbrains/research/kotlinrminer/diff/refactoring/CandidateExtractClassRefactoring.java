package org.jetbrains.research.kotlinrminer.diff.refactoring;

import org.jetbrains.research.kotlinrminer.diff.UMLClassBaseDiff;
import org.jetbrains.research.kotlinrminer.uml.UMLType;

public class CandidateExtractClassRefactoring implements Comparable<CandidateExtractClassRefactoring> {
    private final UMLClassBaseDiff classDiff;
    private final ExtractClassRefactoring refactoring;

    public CandidateExtractClassRefactoring(UMLClassBaseDiff classDiff, ExtractClassRefactoring refactoring) {
        this.classDiff = classDiff;
        this.refactoring = refactoring;
    }

    public boolean innerClassExtract() {
        return refactoring.getExtractedClass().getQualifiedName().startsWith(refactoring.getOriginalClass().getQualifiedName() + ".");
    }

    public boolean subclassExtract() {
        UMLType thisSuperType = this.refactoring.getExtractedClass().getSuperclass();
        return thisSuperType != null && this.classDiff.getNextClassName().endsWith("." + thisSuperType.getClassType());
    }

    public UMLClassBaseDiff getClassDiff() {
        return classDiff;
    }

    public ExtractClassRefactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public int compareTo(CandidateExtractClassRefactoring o) {
        double sourceFolderDistance1 =
            this.refactoring.getExtractedClass().normalizedSourceFolderDistance(this.classDiff.getNextClass());
        double sourceFolderDistance2 =
            o.refactoring.getExtractedClass().normalizedSourceFolderDistance(o.classDiff.getNextClass());
        if (sourceFolderDistance1 != sourceFolderDistance2) {
            return Double.compare(sourceFolderDistance1, sourceFolderDistance2);
        } else {
            if (this.innerClassExtract()) {
                return -1;
            }
            if (o.innerClassExtract()) {
                return 1;
            }
            if (this.subclassExtract()) {
                return -1;
            }
            if (o.subclassExtract()) {
                return 1;
            }
            return this.classDiff.compareTo(o.classDiff);
        }
    }
}
