package org.jetbrains.research.kotlinrminer.ide.decomposition;

import org.jetbrains.kotlin.psi.*;
import org.jetbrains.research.kotlinrminer.common.decomposition.CodeElementType;
import org.jetbrains.research.kotlinrminer.ide.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLAnnotation;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLType;

import java.util.ArrayList;
import java.util.List;

public class VariableDeclaration implements LocationInfoProvider, VariableDeclarationProvider {
    private String variableName;
    private AbstractExpression initializer;
    private UMLType type;
    private boolean varargsParameter;
    private LocationInfo locationInfo;
    private boolean isParameter;
    private boolean isAttribute;
    private boolean isEnumConstant;
    private VariableScope scope;
    private final List<UMLAnnotation> annotations;

    public VariableDeclaration(KtFile ktFile, String filePath, KtElement fragment) {
        this.annotations = new ArrayList<>();
        if (fragment instanceof KtParameter) {
            KtParameter parameter = (KtParameter) fragment;
            KtModifierList extendedModifiers = parameter.getModifierList();
            if (extendedModifiers != null) {
                List<KtAnnotation> annotations = extendedModifiers.getAnnotations();
                annotations.forEach(
                    ktAnnotation -> this.annotations.add(new UMLAnnotation(ktFile, filePath, ktAnnotation)));
            }
            //TODO check for the code element type
            this.locationInfo =
                new LocationInfo(ktFile, filePath, fragment, CodeElementType.SINGLE_VARIABLE_DECLARATION);
            this.variableName = String.valueOf(fragment.getName());
            this.type = UMLType.extractTypeObject(ktFile, filePath, parameter.getTypeReference(), 0);
            int startOffset = fragment.getStartOffsetInParent();
            int endOffset = startOffset + fragment.getTextLength();
            this.scope = new VariableScope(ktFile, filePath, startOffset, endOffset);
        } else if (fragment instanceof KtProperty) {
            KtProperty property = (KtProperty) fragment;
            KtModifierList extendedModifiers = property.getModifierList();
            if (extendedModifiers != null) {
                List<KtAnnotation> annotations = extendedModifiers.getAnnotations();
                annotations.forEach(
                    ktAnnotation -> this.annotations.add(new UMLAnnotation(ktFile, filePath, ktAnnotation)));
            }
            //TODO check for the code element type
            this.locationInfo = new LocationInfo(ktFile, filePath, fragment, CodeElementType.FIELD_DECLARATION);
            this.variableName = String.valueOf(((KtProperty) fragment).getNameIdentifier().getText());
            this.initializer = property.getInitializer() != null ?
                new AbstractExpression(ktFile, filePath,
                                       property.getInitializer(),
                                       CodeElementType.VARIABLE_DECLARATION_INITIALIZER) : null;
            this.type = UMLType.extractTypeObject(ktFile, filePath, property.getTypeReference(), 0);
            int startOffset = fragment.getStartOffsetInParent();
            int endOffset = startOffset + fragment.getTextLength();
            this.scope = new VariableScope(ktFile, filePath, startOffset, endOffset);
        } else if (fragment instanceof KtVariableDeclaration) {
            KtVariableDeclaration variableDeclaration = (KtVariableDeclaration) fragment;
            variableDeclaration.getAnnotations().forEach(ktAnnotation
                                                             -> this.annotations.add(
                new UMLAnnotation(ktFile, filePath, ktAnnotation)));
            //TODO check for the code element type
            this.locationInfo =
                new LocationInfo(ktFile, filePath, fragment, CodeElementType.VARIABLE_DECLARATION_EXPRESSION);
            this.variableName = fragment.getName();
            this.initializer = variableDeclaration.getInitializer() != null ?
                new AbstractExpression(ktFile, filePath,
                                       variableDeclaration.getInitializer(),
                                       CodeElementType.VARIABLE_DECLARATION_INITIALIZER) : null;
            this.type = UMLType.extractTypeObject(ktFile, filePath, variableDeclaration.getTypeReference(), 0);
            int startOffset = fragment.getStartOffsetInParent();
            int endOffset = startOffset + fragment.getTextLength();
            this.scope = new VariableScope(ktFile, filePath, startOffset, endOffset);
        }
    }

    public VariableDeclaration(KtFile cu, String filePath, KtParameter fragment, boolean varargs) {
        this(cu, filePath, fragment);
        this.varargsParameter = varargs;
    }

    public String getVariableName() {
        return variableName;
    }

    public AbstractExpression getInitializer() {
        return initializer;
    }

    public UMLType getType() {
        return type;
    }

    public VariableScope getScope() {
        return scope;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public void setParameter(boolean isParameter) {
        this.isParameter = isParameter;
    }

    public boolean isAttribute() {
        return isAttribute;
    }

    public boolean isEnumConstant() {
        return isEnumConstant;
    }

    public void setAttribute(boolean isAttribute) {
        this.isAttribute = isAttribute;
    }

    public boolean isVarargsParameter() {
        return varargsParameter;
    }

    public List<UMLAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
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
        VariableDeclaration other = (VariableDeclaration) obj;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        if (variableName == null) {
            return other.variableName == null;
        } else return variableName.equals(other.variableName);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(" : ").append(type);
        if (varargsParameter) {
            sb.append("...");
        }
        return sb.toString();
    }

    public String toQualifiedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(" : ").append(type.toQualifiedString());
        if (varargsParameter) {
            sb.append("...");
        }
        return sb.toString();
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    public boolean equalVariableDeclarationType(VariableDeclaration other) {
        return this.locationInfo.getCodeElementType().equals(other.locationInfo.getCodeElementType());
    }

    public VariableDeclaration getVariableDeclaration() {
        return this;
    }
}
