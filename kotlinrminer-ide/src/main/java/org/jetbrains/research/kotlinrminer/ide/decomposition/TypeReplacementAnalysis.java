package org.jetbrains.research.kotlinrminer.ide.decomposition;

import org.jetbrains.research.kotlinrminer.common.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.ide.diff.refactoring.ChangeVariableTypeRefactoring;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TypeReplacementAnalysis {
    private final Set<AbstractCodeMapping> mappings;
    private final Set<ChangeVariableTypeRefactoring> changedTypes = new LinkedHashSet<>();

    public TypeReplacementAnalysis(Set<AbstractCodeMapping> mappings) {
        this.mappings = mappings;
        findTypeChanges();
    }

    public Set<ChangeVariableTypeRefactoring> getChangedTypes() {
        return changedTypes;
    }

    private void findTypeChanges() {
        for (AbstractCodeMapping mapping : mappings) {
            AbstractCodeFragment fragment1 = mapping.getFragment1();
            AbstractCodeFragment fragment2 = mapping.getFragment2();
            for (Replacement replacement : mapping.getReplacements()) {
                if (replacement.getType().equals(Replacement.ReplacementType.TYPE)) {
                    List<VariableDeclaration> declarations1 = fragment1.getVariableDeclarations();
                    List<VariableDeclaration> declarations2 = fragment2.getVariableDeclarations();
                    for (VariableDeclaration declaration1 : declarations1) {
                        for (VariableDeclaration declaration2 : declarations2) {
                            if (declaration1.getVariableName().equals(declaration2.getVariableName()) &&
                                (!declaration1.getType().equals(
                                    declaration2.getType()) || !declaration1.getType().equalsQualified(
                                    declaration2.getType())) &&
                                !containsVariableDeclarationWithSameNameAndType(declaration1, declarations2)) {
                                ChangeVariableTypeRefactoring ref =
                                    new ChangeVariableTypeRefactoring(declaration1, declaration2,
                                                                      mapping.getOperation1(),
                                                                      mapping.getOperation2(),
                                                                      VariableReferenceExtractor.findReferences(
                                                                          declaration1, declaration2,
                                                                          mappings));
                                changedTypes.add(ref);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean containsVariableDeclarationWithSameNameAndType(VariableDeclaration declaration,
                                                                   List<VariableDeclaration> declarations) {
        for (VariableDeclaration d : declarations) {
            if (d.getVariableName().equals(declaration.getVariableName()) && d.getType().equals(
                declaration.getType()) && d.getType().equalsQualified(declaration.getType())) {
                return true;
            }
        }
        return false;
    }
}
