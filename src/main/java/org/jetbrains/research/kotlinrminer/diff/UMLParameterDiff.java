package org.jetbrains.research.kotlinrminer.diff;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.decomposition.AbstractCodeMapping;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.decomposition.VariableReferenceExtractor;
import org.jetbrains.research.kotlinrminer.decomposition.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.uml.UMLParameter;

public class UMLParameterDiff {

  private final UMLParameter removedParameter;
  private final UMLParameter addedParameter;
  private boolean typeChanged;
  private boolean qualifiedTypeChanged;
  private boolean nameChanged;
  private UMLOperation removedOperation;
  private UMLOperation addedOperation;
  private Set<AbstractCodeMapping> mappings;

  public UMLParameterDiff(UMLParameter removedParameter, UMLParameter addedParameter,
                          UMLOperation removedOperation, UMLOperation addedOperation,
                          Set<AbstractCodeMapping> mappings) {
    this.mappings = mappings;
    this.removedParameter = removedParameter;
    this.addedParameter = addedParameter;
    this.typeChanged = false;
    this.nameChanged = false;
    this.removedOperation = removedOperation;
    this.addedOperation = addedOperation;
      if (!removedParameter.getType().equals(addedParameter.getType())) {
          typeChanged = true;
      } else if (!removedParameter.getType().equalsQualified(addedParameter.getType())) {
          qualifiedTypeChanged = true;
      }
      if (!removedParameter.getName().equals(addedParameter.getName())) {
          nameChanged = true;
      }
  }

  public UMLParameter getRemovedParameter() {
    return removedParameter;
  }

  public UMLParameter getAddedParameter() {
    return addedParameter;
  }

  public boolean isTypeChanged() {
    return typeChanged;
  }

  public boolean isQualifiedTypeChanged() {
    return qualifiedTypeChanged;
  }

  public boolean isNameChanged() {
    return nameChanged;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
      if (typeChanged || nameChanged || qualifiedTypeChanged) {
          sb.append("\t\t").append("parameter ").append(removedParameter).append(":").append("\n");
      }
      if (typeChanged || qualifiedTypeChanged) {
          sb.append("\t\t").append("type changed from ").append(removedParameter.getType())
              .append(" to ").append(
              addedParameter.getType()).append("\n");
      }
      if (nameChanged) {
          sb.append("\t\t").append("name changed from ").append(removedParameter.getName())
              .append(" to ").append(
              addedParameter.getName()).append("\n");
      }
    return sb.toString();
  }

  public Set<Refactoring> getRefactorings() {
    Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
    VariableDeclaration originalVariable = getRemovedParameter().getVariableDeclaration();
    VariableDeclaration newVariable = getAddedParameter().getVariableDeclaration();
    Set<AbstractCodeMapping> references = VariableReferenceExtractor
        .findReferences(originalVariable, newVariable, mappings);
    RenameVariableRefactoring renameRefactoring = null;
    if (isNameChanged() && !inconsistentReplacement(originalVariable, newVariable)) {
      renameRefactoring =
          new RenameVariableRefactoring(originalVariable, newVariable, removedOperation,
              addedOperation, references);
      refactorings.add(renameRefactoring);
    }
    if ((isTypeChanged() || isQualifiedTypeChanged()) &&
        !inconsistentReplacement(originalVariable, newVariable)) {
      ChangeVariableTypeRefactoring refactoring =
          new ChangeVariableTypeRefactoring(originalVariable, newVariable, removedOperation,
              addedOperation, references);
      if (renameRefactoring != null) {
        refactoring.addRelatedRefactoring(renameRefactoring);
      }
      refactorings.add(refactoring);
    }
    return refactorings;
  }

  private boolean inconsistentReplacement(VariableDeclaration originalVariable,
                                          VariableDeclaration newVariable) {
    if (removedOperation.isStatic() || addedOperation.isStatic()) {
      for (AbstractCodeMapping mapping : mappings) {
        for (Replacement replacement : mapping.getReplacements()) {
          if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
            if (replacement.getBefore().equals(originalVariable.getVariableName()) &&
                !replacement.getAfter().equals(newVariable.getVariableName())) {
              return true;
            } else if (!replacement.getBefore().equals(originalVariable.getVariableName()) &&
                replacement.getAfter().equals(newVariable.getVariableName())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

}
