package org.jetbrains.research.kotlinrminer.diff.refactoring;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

public class InlineVariableRefactoring implements Refactoring {
    private final VariableDeclaration variableDeclaration;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> references;

    public InlineVariableRefactoring(VariableDeclaration variableDeclaration,
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
        return RefactoringType.INLINE_VARIABLE;
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
        return getName() + "\t" +
                variableDeclaration +
                " in method " +
                operationBefore +
                " from class " +
                operationBefore.getClassName();
    }

    /**
     * @return the code range of the inlined variable declaration in the <b>parent</b> commit
     */
    public CodeRange getInlinedVariableDeclarationCodeRange() {
        return variableDeclaration.codeRange();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
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
        InlineVariableRefactoring other = (InlineVariableRefactoring) obj;
        if (operationBefore == null) {
            if (other.operationBefore != null)
                return false;
        } else if (!operationBefore.equals(other.operationBefore))
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
        ranges.add(variableDeclaration.codeRange()
                           .setDescription("inlined variable declaration")
                           .setCodeElement(variableDeclaration.toString()));
        for (AbstractCodeMapping mapping : references) {
            ranges.add(mapping.getFragment1().codeRange().setDescription(
                    "statement with the name of the inlined variable"));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (AbstractCodeMapping mapping : references) {
            ranges.add(mapping.getFragment2().codeRange().setDescription(
                    "statement with the initializer of the inlined variable"));
        }
        return ranges;
    }
}

