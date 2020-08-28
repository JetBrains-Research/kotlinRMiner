package org.jetbrains.research.kotlinrminer.uml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.research.kotlinrminer.LocationInfo;
import org.jetbrains.research.kotlinrminer.decomposition.LocationInfoProvider;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

/**
 * Represents a companion object in Kotlin
 */
public class UMLCompanionObject implements Comparable<UMLCompanionObject>, Serializable, LocationInfoProvider {
    private final List<UMLOperation> methods;
    private LocationInfo locationInfo;
    private String name;
    private String className;
    private UMLJavadoc javadoc;

    public UMLCompanionObject() {
        this.methods = new ArrayList<>();
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

    public void setName(String name) {
        this.name = name;
    }

    public void addMethod(UMLOperation method) {
        methods.add(method);
    }

    @Override
    public int compareTo(UMLCompanionObject o) {
        return this.name.compareTo(o.toString());
    }

    @Override
    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    @Override
    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof UMLCompanionObject) {
            UMLCompanionObject umlClass = (UMLCompanionObject) o;
            return this.name.equals(umlClass.name) && this.className.equals(umlClass.getClassName());
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    public void setJavadoc(UMLJavadoc javadoc) {
        this.javadoc = javadoc;
    }
}
