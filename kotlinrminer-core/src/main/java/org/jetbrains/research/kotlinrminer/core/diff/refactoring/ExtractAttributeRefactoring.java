package org.jetbrains.research.kotlinrminer.core.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.core.Refactoring;
import org.jetbrains.research.kotlinrminer.core.RefactoringType;
import org.jetbrains.research.kotlinrminer.core.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.core.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.core.uml.UMLAttribute;
import org.jetbrains.research.kotlinrminer.core.uml.UMLClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtractAttributeRefactoring implements Refactoring {
    private final UMLAttribute attributeDeclaration;
    private final UMLClass originalClass;
    private final UMLClass nextClass;
    private final Set<AbstractCodeMapping> references;

    public ExtractAttributeRefactoring(UMLAttribute variableDeclaration, UMLClass originalClass, UMLClass nextClass) {
        this.attributeDeclaration = variableDeclaration;
        this.originalClass = originalClass;
        this.nextClass = nextClass;
        this.references = new LinkedHashSet<>();
    }

    public void addReference(AbstractCodeMapping mapping) {
        references.add(mapping);
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.EXTRACT_ATTRIBUTE;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public UMLAttribute getVariableDeclaration() {
        return attributeDeclaration;
    }

    public Set<AbstractCodeMapping> getReferences() {
        return references;
    }

    public String toString() {
        return getName() + "\t" +
            attributeDeclaration +
            " in class " +
            attributeDeclaration.getClassName();
    }

    /**
     * @return the code range of the extracted variable declaration in the <b>child</b> commit
     */
    public CodeRange getExtractedVariableDeclarationCodeRange() {
        return attributeDeclaration.codeRange();
    }

    public UMLClass getOriginalClass() {
        return originalClass;
    }

    public UMLClass getNextClass() {
        return nextClass;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributeDeclaration == null) ? 0 : attributeDeclaration.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExtractAttributeRefactoring other = (ExtractAttributeRefactoring) obj;
        if (attributeDeclaration == null) {
            return other.attributeDeclaration == null;
        } else return attributeDeclaration.equals(other.attributeDeclaration);
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(
            new ImmutablePair<>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getQualifiedName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getNextClass().getLocationInfo().getFilePath(), getNextClass().getQualifiedName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (AbstractCodeMapping mapping : references) {
            ranges.add(mapping.getFragment1().codeRange().setDescription(
                "statement with the initializer of the extracted attribute"));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(attributeDeclaration.codeRange()
            .setDescription("extracted attribute declaration")
            .setCodeElement(attributeDeclaration.toString()));
        for (AbstractCodeMapping mapping : references) {
            ranges.add(mapping.getFragment2().codeRange().setDescription(
                "statement with the name of the extracted attribute"));
        }
        return ranges;
    }
}
