package org.jetbrains.research.kotlinrminer.cli.uml;

import org.jetbrains.research.kotlinrminer.cli.decomposition.LocationInfo;
import org.jetbrains.research.kotlinrminer.cli.decomposition.LocationInfoProvider;
import org.jetbrains.research.kotlinrminer.cli.diff.CodeRange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Used for representation of package-level functions.
 */
public class UMLFile implements Comparable<UMLFile>, Serializable, LocationInfoProvider {
    private final List<UMLOperation> operations;
    private LocationInfo locationInfo;
    private final String fileName;

    public UMLFile(String fileName) {
        this.fileName = fileName;
        this.operations = new ArrayList<>();
    }

    @Override
    public int compareTo(UMLFile o) {
        return this.toString().compareTo(o.toString());
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
    public String toString() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof UMLFile) {
            UMLFile umlFile = (UMLFile) o;
            return this.fileName.equals(umlFile.fileName);
        }
        return false;
    }

    public void addMethod(UMLOperation method) {
        operations.add(method);
    }

    public List<UMLOperation> getOperations() {
        return this.operations;
    }

    public String getFileName() {
        return fileName;
    }
}