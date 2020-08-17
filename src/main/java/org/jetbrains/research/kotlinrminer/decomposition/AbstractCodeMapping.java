package org.jetbrains.research.kotlinrminer.decomposition;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

public abstract class AbstractCodeMapping {

  private AbstractCodeFragment fragment1;
  private AbstractCodeFragment fragment2;
  private UMLOperation operation1;
  private UMLOperation operation2;
  private Set<Replacement> replacements;
  private boolean identicalWithExtractedVariable;
  private boolean identicalWithInlinedVariable;

  public AbstractCodeMapping(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2,
                             UMLOperation operation1, UMLOperation operation2) {
    this.fragment1 = fragment1;
    this.fragment2 = fragment2;
    this.operation1 = operation1;
    this.operation2 = operation2;
    this.replacements = new LinkedHashSet<Replacement>();
  }

  public AbstractCodeFragment getFragment1() {
    return fragment1;
  }

  public AbstractCodeFragment getFragment2() {
    return fragment2;
  }

  public UMLOperation getOperation1() {
    return operation1;
  }

  public UMLOperation getOperation2() {
    return operation2;
  }

  public boolean isIdenticalWithExtractedVariable() {
    return identicalWithExtractedVariable;
  }

  public boolean isIdenticalWithInlinedVariable() {
    return identicalWithInlinedVariable;
  }

  public boolean isExact() {
    return (fragment1.getArgumentizedString().equals(fragment2.getArgumentizedString()) ||
        fragment1.getString().equals(fragment2.getString()) ||
        containsIdenticalOrCompositeReplacement()) && !isKeyword();
  }


  private boolean isKeyword() {
    return fragment1.getString().startsWith("return") ||
        fragment1.getString().startsWith("break") ||
        fragment1.getString().startsWith("continue");
  }


  private boolean containsIdenticalOrCompositeReplacement() {
    for (Replacement r : replacements) {
      if (r.getType().equals(
          Replacement.ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS) &&
          r.getBefore().equals(r.getAfter())) {
        return true;
      } else if (r.getType().equals(Replacement.ReplacementType.COMPOSITE)) {
        return true;
      }
    }
    return false;
  }

  public void addReplacement(Replacement replacement) {
    this.replacements.add(replacement);
  }

  public void addReplacements(Set<Replacement> replacements) {
    this.replacements.addAll(replacements);
  }

  public Set<Replacement> getReplacements() {
    return replacements;
  }

  public boolean containsReplacement(Replacement.ReplacementType type) {
    for (Replacement replacement : replacements) {
      if (replacement.getType().equals(type)) {
        return true;
      }
    }
    return false;
  }

  public Set<Replacement.ReplacementType> getReplacementTypes() {
    Set<Replacement.ReplacementType> types = new LinkedHashSet<Replacement.ReplacementType>();
    for (Replacement replacement : replacements) {
      types.add(replacement.getType());
    }
    return types;
  }

  public String toString() {
    return fragment1.toString() + fragment2.toString();
  }

  public Set<Replacement> commonReplacements(AbstractCodeMapping other) {
    Set<Replacement> intersection = new LinkedHashSet<Replacement>(this.replacements);
    intersection.retainAll(other.replacements);
    return intersection;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fragment1 == null) ? 0 : fragment1.hashCode());
    result = prime * result + ((fragment2 == null) ? 0 : fragment2.hashCode());
    result = prime * result + ((operation1 == null) ? 0 : operation1.hashCode());
    result = prime * result + ((operation2 == null) ? 0 : operation2.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AbstractCodeMapping other = (AbstractCodeMapping) obj;
    if (fragment1 == null) {
      if (other.fragment1 != null) {
        return false;
      }
    } else if (!fragment1.equals(other.fragment1)) {
      return false;
    }
    if (fragment2 == null) {
      if (other.fragment2 != null) {
        return false;
      }
    } else if (!fragment2.equals(other.fragment2)) {
      return false;
    }
    if (operation1 == null) {
      if (other.operation1 != null) {
        return false;
      }
    } else if (!operation1.equals(other.operation1)) {
      return false;
    }
    if (operation2 == null) {
      if (other.operation2 != null) {
        return false;
      }
    } else if (!operation2.equals(other.operation2)) {
      return false;
    }
    return true;
  }
}
