package org.jetbrains.research.kotlinrminer.uml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.kotlinrminer.LocationInfo;
import org.jetbrains.research.kotlinrminer.decomposition.LocationInfoProvider;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

/**
 * Represents an object in Kotlin
 */
public class UMLObject implements Comparable<UMLObject>, Serializable, LocationInfoProvider {
    private final List<UMLOperation> methods;
    private final List<UMLAttribute> properties;
    private LocationInfo locationInfo;
    private String name;

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

    public void setLocationInfo(LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    @Override
    public CodeRange codeRange() {
        return locationInfo.codeRange();
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

    public void addProperty(UMLAttribute attribute) {
        properties.add(attribute);
    }
}
