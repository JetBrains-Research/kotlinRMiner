package org.jetbrains.research.kotlinrminer.uml;

import java.io.Serializable;
import java.util.List;

import org.jetbrains.research.kotlinrminer.decomposition.LocationInfo;
import org.jetbrains.research.kotlinrminer.decomposition.LocationInfoProvider;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclarationProvider;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

public class UMLAttribute
    implements Comparable<UMLAttribute>, Serializable, LocationInfoProvider, VariableDeclarationProvider {
    private final LocationInfo locationInfo;
    private final String name;
    private UMLType type;
    private String visibility;
    private String className;
    private boolean isFinal;
    private boolean isStatic;
    private VariableDeclaration variableDeclaration;
    private UMLJavadoc javadoc;

    public UMLAttribute(String name, UMLType type, LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
        this.name = name;
        this.type = type;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public UMLType getType() {
        return type;
    }

    public void setType(UMLType type) {
        this.type = type;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public String getNonQualifiedClassName() {
        return className.contains(".") ? className.substring(className.lastIndexOf(".") + 1) : className;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public VariableDeclaration getVariableDeclaration() {
        return variableDeclaration;
    }

    public void setVariableDeclaration(VariableDeclaration variableDeclaration) {
        this.variableDeclaration = variableDeclaration;
    }

    public UMLJavadoc getJavadoc() {
        return javadoc;
    }

    public void setJavadoc(UMLJavadoc javadoc) {
        this.javadoc = javadoc;
    }

    public List<UMLAnnotation> getAnnotations() {
        return variableDeclaration.getAnnotations();
    }

    public boolean equalsIgnoringChangedType(UMLAttribute attribute) {
        if (this.isStatic != attribute.isStatic) {
            return false;
        }
        if (this.isFinal != attribute.isFinal) {
            return false;
        }
        if (this.name.equals(attribute.name) && this.type.equals(attribute.type) && this.type.equalsQualified(
            attribute.type)) {
            return true;
        }
        if (!this.type.equals(attribute.type)) {
            return this.name.equals(attribute.name);
        }
        return false;
    }

    public boolean equalsIgnoringChangedVisibility(UMLAttribute attribute) {
        return this.name.equals(attribute.name) && this.type.equals(attribute.type);
    }

    public CodeRange codeRange() {
        LocationInfo info = getLocationInfo();
        return info.codeRange();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof UMLAttribute) {
            UMLAttribute umlAttribute = (UMLAttribute) o;
            return this.name.equals(umlAttribute.name) &&
                this.visibility.equals(umlAttribute.visibility) &&
                this.type.equals(umlAttribute.type);
        }
        return false;
    }

    public boolean equalsQualified(UMLAttribute umlAttribute) {
        return this.name.equals(umlAttribute.name) &&
            this.visibility.equals(umlAttribute.visibility) &&
            this.type.equalsQualified(umlAttribute.type);
    }

    public String toString() {
        return visibility +
            " " +
            name +
            " : " +
            type;
    }

    public String toQualifiedString() {
        return visibility +
            " " +
            name +
            " : " +
            type.toQualifiedString();
    }

    public int compareTo(UMLAttribute attribute) {
        return this.toString().compareTo(attribute.toString());
    }

}