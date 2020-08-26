package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.research.kotlinrminer.uml.UMLType;

import java.util.regex.Pattern;

public class LeafType extends UMLType {
    private final String classType;
    private final String nonQualifiedClassType;
    private volatile int hashCode = 0;
    private static final Pattern CAMEL_CASE_SPLIT_PATTERN =
            Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

    public LeafType(String type) {
        this.classType = type;
        this.nonQualifiedClassType = simpleNameOf(type);
    }

    @Override
    public String getClassType() {
        return classType;
    }

    private static String simpleNameOf(String name) {
        int numberOfDots = 0;
        int indexOfFirstUpperCaseCharacterFollowedByDot = -1;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '.') {
                numberOfDots++;
                if (Character.isUpperCase(name.charAt(i + 1)) &&
                        indexOfFirstUpperCaseCharacterFollowedByDot == -1) {
                    indexOfFirstUpperCaseCharacterFollowedByDot = i + 1;
                }
            }
        }
        if (numberOfDots == 0 || Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        if (numberOfDots > 1 && indexOfFirstUpperCaseCharacterFollowedByDot != -1) {
            return name.substring(indexOfFirstUpperCaseCharacterFollowedByDot);
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof LeafType) {
            LeafType typeObject = (LeafType) o;

            return equalClassType(typeObject) && equalTypeArgumentsAndArrayDimension(typeObject);
        }
        return false;
    }

    @Override
    public boolean equalsQualified(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.classType.equals(((LeafType) type).classType) && equalTypeArgumentsAndArrayDimension(type);
        }
        return false;
    }

    @Override
    public boolean equalsWithSubType(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return firstOrLastCamelCaseTokenMatch(this.nonQualifiedClassType, ((LeafType) type).nonQualifiedClassType)
                    && equalTypeArgumentsAndArrayDimensionForSubType(type);
        }
        return false;
    }

    private static boolean firstOrLastCamelCaseTokenMatch(String classType1, String classType2) {
        String[] tokens1 = CAMEL_CASE_SPLIT_PATTERN.split(classType1);
        String[] tokens2 = CAMEL_CASE_SPLIT_PATTERN.split(classType2);
        if (tokens1.length > 0 && tokens2.length > 0) {
            return tokens1[0].equals(tokens2[0]) || tokens1[tokens1.length - 1].equals(tokens2[tokens2.length - 1]);
        }
        return false;
    }

    @Override
    public boolean equalClassType(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.nonQualifiedClassType.equals(((LeafType) type).nonQualifiedClassType);
        }
        return false;
    }

    @Override
    public boolean compatibleTypes(UMLType type) {
        if (this.getClass() == type.getClass()) {
            LeafType leafType = (LeafType) type;
            return this.getClassType().equals(leafType.getClassType()) ||
                    this.getClassType().equals("Object") ||
                    leafType.getClassType().equals("Object") ||
                    this.getClassType().startsWith(leafType.getClassType()) ||
                    leafType.getClassType().startsWith(this.getClassType()) ||
                    this.getClassType().endsWith(leafType.getClassType()) ||
                    leafType.getClassType().endsWith(this.getClassType()) ||
                    this.containsTypeArgument(leafType.getClassType()) ||
                    leafType.containsTypeArgument(this.getClassType()) ||
                    this.commonTokenInClassType(leafType);
        }
        return false;
    }

    private boolean commonTokenInClassType(LeafType type) {
        String[] tokens1 = CAMEL_CASE_SPLIT_PATTERN.split(this.nonQualifiedClassType);
        String[] tokens2 = CAMEL_CASE_SPLIT_PATTERN.split(type.nonQualifiedClassType);
        for (String token1 : tokens1) {
            for (String token2 : tokens2) {
                if ((token1.equals(token2) && token1.length() > 1) || token1.equals(token2 + "s")
                        || token2.equals(token1 + "s")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + classType.hashCode();
            if (isParameterized())
                result = 37 * result + typeArgumentsToString().hashCode();
            result = 37 * result + getArrayDimension();
            hashCode = result;
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return nonQualifiedClassType +
                typeArgumentsToString();
    }

    @Override
    public String toQualifiedString() {
        return classType +
                typeArgumentsToString();
    }
}
