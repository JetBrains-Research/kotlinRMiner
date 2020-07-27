package org.jetbrains.research.kotlinrminer.uml;

import org.jetbrains.kotlin.psi.*;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.LocationInfo;
import org.jetbrains.research.kotlinrminer.decomposition.CompositeType;
import org.jetbrains.research.kotlinrminer.decomposition.LeafType;
import org.jetbrains.research.kotlinrminer.decomposition.LocationInfoProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class UMLType implements Serializable, LocationInfoProvider {
    private LocationInfo locationInfo;
    private int arrayDimension;
    private List<UMLType> typeArguments = new ArrayList<>();
    protected List<UMLAnnotation> annotations = new ArrayList<>();

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public int getArrayDimension() {
        return this.arrayDimension;
    }

    public List<UMLAnnotation> getAnnotations() {
        return annotations;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    public void setVarargs() {
        arrayDimension++;
    }

    protected String typeArgumentsToString() {
        StringBuilder sb = new StringBuilder();
        if (typeArguments.isEmpty()) {
            sb.append("");
        } else {
            sb.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                sb.append(typeArguments.get(i).toQualifiedString());
                if (i < typeArguments.size() - 1)
                    sb.append(",");
            }
            sb.append(">");
        }
        return sb.toString();
    }

    protected String typeArgumentsAndArrayDimensionToString() {
        StringBuilder sb = new StringBuilder();
        if (isParameterized())
            sb.append(typeArgumentsToString());
        for (int i = 0; i < getArrayDimension(); i++)
            sb.append("[]");
        return sb.toString();
    }

    private boolean equalTypeArguments(UMLType type) {
        String thisTypeArguments = this.typeArgumentsToString();
        String otherTypeArguments = type.typeArgumentsToString();
        if ((thisTypeArguments.equals("<?>") && otherTypeArguments.startsWith("<? ")) ||
                (thisTypeArguments.startsWith("<? ") && otherTypeArguments.equals("<?>"))) {
            return true;
        }
        if ((thisTypeArguments.equals("<Object>") && otherTypeArguments.contains("<Object>")) ||
                (otherTypeArguments.equals("<Object>") && thisTypeArguments.contains("<Object>"))) {
            return true;
        }
        if (this.typeArguments.size() != type.typeArguments.size()) {
            return false;
        }
        for (int i = 0; i < this.typeArguments.size(); i++) {
            UMLType thisComponent = this.typeArguments.get(i);
            UMLType otherComponent = type.typeArguments.get(i);
            if (!thisComponent.equals(otherComponent)) {
                return false;
            }
        }
        return true;
    }

    protected boolean equalTypeArgumentsAndArrayDimension(UMLType typeObject) {
        if (!this.isParameterized() && !typeObject.isParameterized())
            return this.arrayDimension == typeObject.arrayDimension;
        else if (this.isParameterized() && typeObject.isParameterized())
            return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
        return false;
    }

    protected boolean equalTypeArgumentsAndArrayDimensionForSubType(UMLType typeObject) {
        if (!this.isParameterized() && !typeObject.isParameterized())
            return this.arrayDimension == typeObject.arrayDimension;
        else if (this.isParameterized() && typeObject.isParameterized())
            return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
        else if (this.isParameterized() && this.typeArgumentsToString().equals("<?>") && !typeObject.isParameterized())
            return this.arrayDimension == typeObject.arrayDimension;
        else if (!this.isParameterized() && typeObject.isParameterized() && typeObject.typeArgumentsToString().equals("<?>"))
            return this.arrayDimension == typeObject.arrayDimension;
        return false;
    }

    public boolean containsTypeArgument(String type) {
        for (UMLType typeArgument : typeArguments) {
            if (typeArgument.toString().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isParameterized() {
        return typeArguments.size() > 0;
    }

    public abstract boolean equals(Object o);

    public abstract int hashCode();

    public abstract String toString();

    public abstract String toQualifiedString();

    public abstract String getClassType();

    public boolean equalsQualified(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.equals(type);
        }
        return false;
    }

    public boolean equalsWithSubType(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.equals(type);
        }
        return false;
    }

    public boolean equalClassType(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.equals(type);
        }
        return false;
    }

    public boolean compatibleTypes(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.equals(type);
        }
        return false;
    }

    public static LeafType extractTypeObject(String qualifiedName) {
        int arrayDimension = 0;
        List<UMLType> typeArgumentDecomposition = new ArrayList<>();
        if (qualifiedName.endsWith("[]")) {
            while (qualifiedName.endsWith("[]")) {
                qualifiedName = qualifiedName.substring(0, qualifiedName.lastIndexOf("[]"));
                arrayDimension++;
            }
        }
        if (qualifiedName.contains("<") && qualifiedName.contains(">")) {
            String typeArguments = qualifiedName.substring(qualifiedName.indexOf("<") + 1, qualifiedName.lastIndexOf(">"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < typeArguments.length(); i++) {
                char charAt = typeArguments.charAt(i);
                if (charAt != ',') {
                    sb.append(charAt);
                } else {
                    if (sb.length() > 0 && equalOpeningClosingTags(sb.toString())) {
                        typeArgumentDecomposition.add(extractTypeObject(sb.toString()));
                        sb = new StringBuilder();
                    } else {
                        sb.append(charAt);
                    }
                }
            }
            if (sb.length() > 0) {
                typeArgumentDecomposition.add(extractTypeObject(sb.toString()));
            }
            qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("<"));
        }
        UMLType typeObject = new LeafType(qualifiedName);
        typeObject.arrayDimension = arrayDimension;
        typeObject.typeArguments = typeArgumentDecomposition;
        return (LeafType) typeObject;
    }

    private static boolean equalOpeningClosingTags(String typeArguments) {
        int openingTags = 0;
        int closingTags = 0;
        for (int i = 0; i < typeArguments.length(); i++) {
            if (typeArguments.charAt(i) == '>') {
                openingTags++;
            } else if (typeArguments.charAt(i) == '<') {
                closingTags++;
            }
        }
        return openingTags == closingTags;
    }

    public static UMLType extractTypeObject(KtFile ktFile, String filePath, KtElement type, int extraDimensions) {
        UMLType umlType = extractTypeObject(ktFile, filePath, type);
        umlType.locationInfo = new LocationInfo(ktFile, filePath, type, LocationInfo.CodeElementType.TYPE);
        umlType.arrayDimension += extraDimensions;
        return umlType;
    }

    private static UMLType extractTypeObject(KtFile ktFile, String filePath, KtElement type) {
        if (type instanceof KtUserType) {
            KtUserType userType = (KtUserType) type;
            KtUserType qualifier = userType.getQualifier();
            if (qualifier != null) {
                UMLType left = extractTypeObject(ktFile, filePath, qualifier);
                LeafType rightType = extractTypeObject(userType.getText());
                return new CompositeType(left, rightType);
            } else return extractTypeObject(type.getText());
        } else if (type instanceof KtTypeReference) {
            KtTypeReference typeReference = (KtTypeReference) type;
            KtTypeElement element = typeReference.getTypeElement();
            if (element instanceof KtFunctionType) {
                KtFunctionType functionType = (KtFunctionType) element;
                UMLType returnType = extractTypeObject(functionType.getReturnTypeReference().getText());
                UMLType receiver = extractTypeObject(functionType.getReceiverTypeReference().getText());
                return new UMLFunctionType(receiver, returnType);
            }
            UMLType result = extractTypeObject(element.getText());
            List<KtTypeReference> types = element.getTypeArgumentsAsTypes();
            for (KtTypeReference t : types) {
                result.typeArguments.add(extractTypeObject(ktFile, filePath, t));
            }

            final List<KtAnnotation> annotations = typeReference.getAnnotations();
            for (KtAnnotation annotation : annotations) {
                result.annotations.add(new UMLAnnotation(ktFile, filePath, annotation));
            }

            if (element instanceof KtUserType) {
                KtUserType userType = (KtUserType) element;
                if (userType.getQualifier() != null) {
                    UMLType left = extractTypeObject(ktFile, filePath, userType.getQualifier());
                    return new CompositeType(left, (LeafType) result);
                }
            }
            return result;
        } else if (type instanceof KtProperty) {
            KtProperty property = (KtProperty) type;
            return extractTypeObject(ktFile, filePath, property.getTypeReference());
        }
        return null;
    }

}
