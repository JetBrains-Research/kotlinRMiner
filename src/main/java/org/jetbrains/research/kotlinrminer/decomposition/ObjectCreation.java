package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.research.kotlinrminer.diff.StringDistance;
import org.jetbrains.research.kotlinrminer.uml.UMLType;

public class ObjectCreation extends AbstractCall {
    private UMLType type;
    private String anonymousClassDeclaration;
    private final boolean isArray = false;

    public UMLType getType() {
        return type;
    }

    public String getAnonymousClassDeclaration() {
        return anonymousClassDeclaration;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean identicalArrayInitializer(ObjectCreation other) {
        if (this.isArray && other.isArray) {
            if (this.anonymousClassDeclaration != null && other.anonymousClassDeclaration != null) {
                return this.anonymousClassDeclaration.equals(other.anonymousClassDeclaration);
            } else return this.anonymousClassDeclaration == null && other.anonymousClassDeclaration == null;
        }
        return false;
    }

    @Override
    public boolean identicalName(AbstractCall call) {
        return getType().equals(((ObjectCreation) call).getType());
    }

    @Override
    public String getName() {
        return getType().toString();
    }

    @Override
    public double normalizedNameDistance(AbstractCall call) {
        String s1 = getType().toString().toLowerCase();
        String s2 = ((ObjectCreation) call).getType().toString().toLowerCase();
        int distance = StringDistance.editDistance(s1, s2);
        return (double) distance / (double) Math.max(s1.length(), s2.length());
    }

    @Override
    public AbstractCall update(String oldExpression, String newExpression) {
        ObjectCreation newObjectCreation = new ObjectCreation();
        newObjectCreation.type = this.type;
        newObjectCreation.locationInfo = this.locationInfo;
        update(newObjectCreation, oldExpression, newExpression);
        return newObjectCreation;
    }
}
