package org.jetbrains.research.kotlinrminer.ide.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.ide.Refactoring;
import org.jetbrains.research.kotlinrminer.common.RefactoringType;
import org.jetbrains.research.kotlinrminer.ide.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.ide.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.ide.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLOperation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtractVariableRefactoring implements Refactoring {
    private final VariableDeclaration variableDeclaration;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> references;

    public ExtractVariableRefactoring(VariableDeclaration variableDeclaration,
                                      UMLOperation operationBefore,
                                      UMLOperation operationAfter) {
        this.variableDeclaration = variableDeclaration;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.references = new LinkedHashSet<>();
    }

    public void addReference(AbstractCodeMapping mapping) {
        references.add(mapping);
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.EXTRACT_VARIABLE;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public VariableDeclaration getVariableDeclaration() {
        return variableDeclaration;
    }

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    public Set<AbstractCodeMapping> getReferences() {
        return references;
    }

    public String toString() {
        return getName() + " " +
            variableDeclaration +
            " in method " +
            operationAfter +
            " from class " +
            operationAfter.getClassName();
    }

    /**
     * @return the code range of the extracted variable declaration in the <b>child</b> commit
     */
    public CodeRange getExtractedVariableDeclarationCodeRange() {
        return variableDeclaration.codeRange();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((variableDeclaration == null) ? 0 : variableDeclaration.hashCode());
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
        ExtractVariableRefactoring other = (ExtractVariableRefactoring) obj;
        if (operationAfter == null) {
            if (other.operationAfter != null)
                return false;
        } else if (!operationAfter.equals(other.operationAfter))
            return false;
        if (variableDeclaration == null) {
            return other.variableDeclaration == null;
        } else return variableDeclaration.equals(other.variableDeclaration);
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(),
            getOperationBefore().getClassName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(
            new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(),
                getOperationAfter().getClassName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (AbstractCodeMapping mapping : references) {
            ranges.add(mapping.getFragment1().codeRange().setDescription(
                "statement with the initializer of the extracted variable"));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(variableDeclaration.codeRange()
            .setDescription("extracted variable declaration")
            .setCodeElement(variableDeclaration.toString()));
        for (AbstractCodeMapping mapping : references) {
            ranges.add(mapping.getFragment2().codeRange().setDescription(
                "statement with the name of the extracted variable"));
        }
        return ranges;
    }
}

