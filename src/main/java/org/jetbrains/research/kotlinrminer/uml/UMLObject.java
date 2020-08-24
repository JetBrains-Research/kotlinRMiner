package org.jetbrains.research.kotlinrminer.uml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.kotlinrminer.LocationInfo;
import org.jetbrains.research.kotlinrminer.decomposition.LocationInfoProvider;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an object in Kotlin
 */
public class UMLObject implements Comparable<UMLObject>, Serializable, LocationInfoProvider {
    private LocationInfo locationInfo;
    private String name;
    private List<UMLOperation> methods;
    private List<UMLAttribute> properties;

    public UMLObject() {
        this.methods = new ArrayList<>();
        this.properties = new ArrayList<>();
    }

    @Override
    public int compareTo(@NotNull UMLObject o) {
        return this.name.compareTo(o.getName());
    }

    @Override
    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    @Override
    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLocationInfo(LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    public void addMethod(UMLOperation method) {
        methods.add(method);
    }

    public void addProperty(UMLAttribute attribute) {
        properties.add(attribute);
    }
}
