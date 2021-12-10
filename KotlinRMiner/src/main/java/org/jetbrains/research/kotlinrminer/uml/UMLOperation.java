package org.jetbrains.research.kotlinrminer.uml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.research.kotlinrminer.decomposition.*;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.diff.StringDistance;

public class UMLOperation implements Comparable<UMLOperation>, Serializable, LocationInfoProvider {
    private final LocationInfo locationInfo;
    private final String name;
    private final List<UMLParameter> parameters;
    private final List<UMLTypeParameter> typeParameters;
    private final List<UMLAnnotation> annotations;
    private String visibility;
    private boolean isAbstract;
    private String className;
    private boolean isConstructor;
    private boolean isFinal;
    private boolean isStatic;
    private boolean emptyBody;
    private OperationBody operationBody;
    private UMLJavadoc javadoc;

    public UMLOperation(String name, LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
        this.name = name;
        this.parameters = new ArrayList<>();
        this.typeParameters = new ArrayList<>();
        this.annotations = new ArrayList<>();
    }

    private static boolean equivalentNames(UMLOperation operation1, UMLOperation operation2) {
        boolean equalReturn = operation1.equalReturnParameter(
            operation2) && operation1.getParametersWithoutReturnType().size() > 0 &&
            operation2.getParametersWithoutReturnType().size() > 0;
        if (operation1.name.startsWith(operation2.name) && !operation2.name.equals("get") && !operation2.name.equals(
            "set") && !operation2.name.equals("print")) {
            String suffix1 = operation1.name.substring(operation2.name.length());
            String className2 = operation2.className.contains(".") ?
                operation2.className.substring(operation2.className.lastIndexOf(".") + 1
                ) : operation2.className;
            return operation2.name.length() > operation1.name.length() - operation2.name.length() || equalReturn ||
                className2.contains(
                    suffix1);
        }
        return false;
    }

    public List<UMLTypeParameter> getTypeParameters() {
        return typeParameters;
    }

    public void addTypeParameter(UMLTypeParameter typeParameter) {
        typeParameters.add(typeParameter);
    }

    public List<UMLAnnotation> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(UMLAnnotation annotation) {
        annotations.add(annotation);
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public String getName() {
        return name;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public void setConstructor(boolean isConstructor) {
        this.isConstructor = isConstructor;
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

    public boolean hasEmptyBody() {
        return emptyBody;
    }

    public void setEmptyBody(boolean emptyBody) {
        this.emptyBody = emptyBody;
    }

    public OperationBody getBody() {
        return operationBody;
    }

    public void setBody(OperationBody body) {
        this.operationBody = body;
    }

    public boolean hasTestAnnotation() {
        for (UMLAnnotation annotation : annotations) {
            if (annotation.getTypeName().equals("Test")) {
                return true;
            }
        }
        return false;
    }

    public boolean testAnnotationCheck(UMLOperation operation) {
        if (this.hasTestAnnotation() && !operation.hasTestAnnotation())
            return false;
        return this.hasTestAnnotation() || !operation.hasTestAnnotation();
    }

    public UMLJavadoc getJavadoc() {
        return javadoc;
    }

    public void setJavadoc(UMLJavadoc javadoc) {
        this.javadoc = javadoc;
    }

    public List<String> getAllVariables() {
        if (operationBody != null) {
            return operationBody.getAllVariables();
        }
        return new ArrayList<>();
    }

    public List<VariableDeclaration> getAllVariableDeclarations() {
        if (operationBody != null) {
            return operationBody.getAllVariableDeclarations();
        }
        return new ArrayList<>();
    }

    public VariableDeclaration getVariableDeclaration(String variableName) {
        if (operationBody != null) {
            return operationBody.getVariableDeclaration(variableName);
        }
        return null;
    }

    public Map<String, UMLType> variableTypeMap() {
        Map<String, UMLType> variableTypeMap = new LinkedHashMap<>();
        for (UMLParameter parameter : parameters) {
            if (!parameter.getKind().equals("return")) {
                variableTypeMap.put(parameter.getName(), parameter.getType());
            }
        }
        for (VariableDeclaration declaration : getAllVariableDeclarations()) {
            variableTypeMap.put(declaration.getVariableName(), declaration.getType());
        }
        return variableTypeMap;
    }

    public int statementCount() {
        if (operationBody != null) {
            return operationBody.statementCount();
        }
        return 0;
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

    public void addParameter(UMLParameter parameter) {
        this.parameters.add(parameter);
    }

    public List<UMLParameter> getParameters() {
        return parameters;
    }

    public UMLParameter getReturnParameter() {
        for (UMLParameter parameter : parameters) {
            if (parameter.getKind().equals("return")) {
                return parameter;
            }
        }
        return null;
    }

    public List<VariableDeclaration> getParameterDeclarationList() {
        List<VariableDeclaration> parameterDeclarationList = new ArrayList<>();
        for (UMLParameter parameter : parameters) {
            if (!parameter.getKind().equals("return"))
                parameterDeclarationList.add(parameter.getVariableDeclaration());
        }
        return parameterDeclarationList;
    }

    public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
        List<VariableDeclaration> variableDeclarations = new ArrayList<>();
        for (VariableDeclaration parameterDeclaration : getParameterDeclarationList()) {
            if (parameterDeclaration.getScope().subsumes(location)) {
                variableDeclarations.add(parameterDeclaration);
            }
        }
        if (operationBody != null) {
            variableDeclarations.addAll(operationBody.getVariableDeclarationsInScope(location));
        }
        return variableDeclarations;
    }

    public boolean equalReturnParameter(UMLOperation operation) {
        UMLParameter thisReturnParameter = this.getReturnParameter();
        UMLParameter otherReturnParameter = operation.getReturnParameter();
        if (thisReturnParameter != null && otherReturnParameter != null) {
            return thisReturnParameter.equals(otherReturnParameter);
        } else {
            return thisReturnParameter == null && otherReturnParameter == null;
        }
    }

    public boolean equalQualifiedReturnParameter(UMLOperation operation) {
        UMLParameter thisReturnParameter = this.getReturnParameter();
        UMLParameter otherReturnParameter = operation.getReturnParameter();
        if (thisReturnParameter != null && otherReturnParameter != null) {
            return thisReturnParameter.equalsQualified(otherReturnParameter);
        } else {
            return thisReturnParameter == null && otherReturnParameter == null;
        }
    }

    public boolean equalSignature(UMLOperation operation) {
        boolean equalParameterTypes = this.getParameterTypeList().equals(operation.getParameterTypeList());
        boolean compatibleParameterTypes = false;
        if (!equalParameterTypes) {
            List<UMLType> thisParameterTypeList = this.getParameterTypeList();
            List<UMLType> otherParameterTypeList = operation.getParameterTypeList();
            if (thisParameterTypeList.size() != 0 && thisParameterTypeList.size() == otherParameterTypeList.size()) {
                int compatibleTypes = 0;
                int equalTypes = 0;
                for (int i = 0; i < thisParameterTypeList.size(); i++) {
                    UMLType thisParameterType = thisParameterTypeList.get(i);
                    UMLType otherParameterType = otherParameterTypeList.get(i);
                    if (thisParameterType != null && otherParameterType != null
                        && thisParameterType.getClassType() != null && otherParameterType.getClassType() != null) {
                        if ((thisParameterType.getClassType().endsWith("." + otherParameterType.getClassType()) ||
                            otherParameterType.getClassType().endsWith("." + thisParameterType.getClassType())) &&
                            thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) {
                            compatibleTypes++;
                        } else if (thisParameterType.equals(otherParameterType)) {
                            equalTypes++;
                        }
                    }
                    if (equalTypes + compatibleTypes == thisParameterTypeList.size()) {
                        compatibleParameterTypes = true;
                    }
                }
            }
        }
        return this.name.equals(operation.name) && equalTypeParameters(
            operation) && (equalParameterTypes || compatibleParameterTypes) && equalReturnParameter(operation);
    }

    public boolean equalSignatureIgnoringOperationName(UMLOperation operation) {
        return this.visibility.equals(operation.visibility) &&
            this.isAbstract == operation.isAbstract &&
            this.isFinal == operation.isFinal &&
            this.isStatic == operation.isStatic &&
            this.parameters.equals(operation.parameters) &&
            equalTypeParameters(operation);
    }

    public boolean equalSignatureIgnoringChangedTypes(UMLOperation operation) {
        if (!(this.isConstructor && operation.isConstructor || equivalentName(operation))) {
            return false;
        }
        if (this.isAbstract != operation.isAbstract) {
            return false;
        }
        if (this.parameters.size() != operation.parameters.size()) {
            return false;
        }
        if (!equalTypeParameters(operation)) {
            return false;
        }
        int i = 0;
        for (UMLParameter thisParameter : this.parameters) {
            UMLParameter otherParameter = operation.parameters.get(i);
            if (!thisParameter.equals(otherParameter) && !thisParameter.equalsExcludingType(otherParameter)) {
                return false;
            }
            i++;
        }
        return true;
    }

    public OperationInvocation isDelegate() {
        if (getBody() != null) {
            List<AbstractStatement> statements = getBody().getCompositeStatement().getStatements();
            if (statements.size() == 1 && statements.get(0) instanceof StatementObject) {
                StatementObject statement = (StatementObject) statements.get(0);
                Map<String, List<OperationInvocation>> operationInvocationMap = statement.getMethodInvocationMap();
                for (String key : operationInvocationMap.keySet()) {
                    List<OperationInvocation> operationInvocations = operationInvocationMap.get(key);
                    for (OperationInvocation operationInvocation : operationInvocations) {
                        if (operationInvocation.matchesOperation(this, this.variableTypeMap(),
                                                                 null) || operationInvocation.getMethodName().equals(
                            this.getName())) {
                            return operationInvocation;
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean isGetter() {
        if (getBody() != null) {
            List<AbstractStatement> statements = getBody().getCompositeStatement().getStatements();
            List<UMLParameter> parameters = getParametersWithoutReturnType();
            if (statements.size() == 1 && statements.get(0) instanceof StatementObject) {
                StatementObject statement = (StatementObject) statements.get(0);
                if (statement.getString().startsWith("return ")) {
                    for (String variable : statement.getVariables()) {
                        if (statement.getString().equals("return " + variable + "\n") && parameters.size() == 0) {
                            return true;
                        } else if (statement.getString().equals(
                            "return " + variable + ".keySet()" + "\n") && parameters.size() == 0) {
                            return true;
                        } else if (statement.getString().equals(
                            "return " + variable + ".values()" + "\n") && parameters.size() == 0) {
                            return true;
                        }
                    }
                    UMLParameter returnParameter = getReturnParameter();
                    if ((name.startsWith("is") || name.startsWith("has")) && parameters.size() == 0 &&
                        returnParameter != null && returnParameter.getType().getClassType().equals("Boolean")) {
                        return true;
                    }
                    return statement.getString().equals("return Null\n");
                }
            }
        }
        return false;
    }

    public boolean equalSignatureWithIdenticalNameIgnoringChangedTypes(UMLOperation operation) {
        if (!(this.isConstructor && operation.isConstructor || this.name.equals(operation.name))) {
            return false;
        }
        if (this.isAbstract != operation.isAbstract) {
            return false;
        }
        if (this.parameters.size() != operation.parameters.size()) {
            return false;
        }
        if (!equalTypeParameters(operation)) {
            return false;
        }
        int i = 0;
        for (UMLParameter thisParameter : this.parameters) {
            UMLParameter otherParameter = operation.parameters.get(i);
            if (!thisParameter.equals(otherParameter) && !thisParameter.equalsExcludingType(otherParameter)) {
                return false;
            }
            i++;
        }
        return true;
    }

    private boolean equivalentName(UMLOperation operation) {
        return this.name.equals(operation.name) || equivalentNames(this, operation) || equivalentNames(operation, this);
    }

    public double normalizedNameDistance(UMLOperation operation) {
        String s1 = getName().toLowerCase();
        String s2 = operation.getName().toLowerCase();
        int distance = StringDistance.editDistance(s1, s2);
        return (double) distance / (double) Math.max(s1.length(), s2.length());
    }

    public List<String> stringRepresentation() {
        if (operationBody != null) {
            return operationBody.stringRepresentation();
        }
        return new ArrayList<>();
    }

    public List<UMLParameter> getParametersWithoutReturnType() {
        List<UMLParameter> params = new ArrayList<>();
        for (UMLParameter parameter : parameters) {
            if (!parameter.getKind().equals("return")) {
                params.add(parameter);
            }
        }
        return params;
    }

    public List<UMLType> commonParameterTypes(UMLOperation operation) {
        List<UMLType> commonParameterTypes = new ArrayList<>();
        List<UMLType> thisParameterTypeList = getParameterTypeList();
        List<UMLType> otherParameterTypeList = operation.getParameterTypeList();
        int min = Math.min(thisParameterTypeList.size(), otherParameterTypeList.size());
        for (int i = 0; i < min; i++) {
            UMLType thisParameterType = thisParameterTypeList.get(i);
            UMLType otherParameterType = otherParameterTypeList.get(i);
            if (thisParameterType.equals(otherParameterType)) {
                commonParameterTypes.add(thisParameterType);
            }
        }
        return commonParameterTypes;
    }

    public List<UMLType> getParameterTypeList() {
        List<UMLType> parameterTypeList = new ArrayList<>();
        for (UMLParameter parameter : parameters) {
            if (!parameter.getKind().equals("return")) {
                parameterTypeList.add(parameter.getType());
            }
        }
        return parameterTypeList;
    }

    public List<String> getParameterNameList() {
        List<String> parameterNameList = new ArrayList<>();
        for (UMLParameter parameter : parameters) {
            if (!parameter.getKind().equals("return")) {
                parameterNameList.add(parameter.getName());
            }
        }
        return parameterNameList;
    }

    public int getNumberOfNonVarargsParameters() {
        int counter = 0;
        for (UMLParameter parameter : parameters) {
            if (!parameter.getKind().equals("return") && !parameter.isVarargs()) {
                counter++;
            }
        }
        return counter;
    }

    public boolean hasVarargsParameter() {
        for (UMLParameter parameter : parameters) {
            if (!parameter.getKind().equals("return") && parameter.isVarargs()) {
                return true;
            }
        }
        return false;
    }

    public boolean overridesObject() {
        return /*isEquals() ||*/ isHashCode() || isToString() || isClone() || isCompareTo();
    }

    public boolean equalsIgnoringVisibility(UMLOperation operation) {
        boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
        boolean otherEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
        return this.className.equals(operation.className) &&
            this.name.equals(operation.name) &&
            this.isAbstract == operation.isAbstract &&
            thisEmptyBody == otherEmptyBody &&
            equalReturnParameter(operation) &&
            this.getParameterTypeList().equals(operation.getParameterTypeList()) &&
            equalTypeParameters(operation);
    }

    public boolean equalsIgnoringNameCase(UMLOperation operation) {
        boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
        boolean otherEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
        return this.className.equals(operation.className) &&
            this.name.equalsIgnoreCase(operation.name) &&
            this.visibility.equals(operation.visibility) &&
            this.isAbstract == operation.isAbstract &&
            thisEmptyBody == otherEmptyBody &&
            equalReturnParameter(operation) &&
            this.getParameterTypeList().equals(operation.getParameterTypeList()) &&
            equalTypeParameters(operation);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof UMLOperation) {
            UMLOperation operation = (UMLOperation) o;
            boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
            boolean otherEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
            return this.className.equals(operation.className) &&
                this.name.equals(operation.name) &&
                this.visibility.equals(operation.visibility) &&
                this.isAbstract == operation.isAbstract &&
                thisEmptyBody == otherEmptyBody &&
                this.getParameterTypeList().equals(operation.getParameterTypeList()) &&
                equalTypeParameters(operation);
        }
        return false;
    }

    public boolean equalsQualified(UMLOperation operation) {
        if (this.className.equals(operation.className) &&
            this.name.equals(operation.name) &&
            this.visibility.equals(operation.visibility) &&
            this.isAbstract == operation.isAbstract &&
            equalTypeParameters(operation)) {
            UMLParameter thisReturnParameter = this.getReturnParameter();
            UMLParameter otherReturnParameter = operation.getReturnParameter();
            if (thisReturnParameter != null && otherReturnParameter != null) {
                if (!thisReturnParameter.getType().equalsQualified(otherReturnParameter.getType())) {
                    return false;
                }
            }
            List<UMLType> thisParameterTypeList = this.getParameterTypeList();
            List<UMLType> otherParameterTypeList = operation.getParameterTypeList();
            if (thisParameterTypeList.size() != otherParameterTypeList.size()) {
                return false;
            }
            for (int i = 0; i < thisParameterTypeList.size(); i++) {
                UMLType thisParameterType = thisParameterTypeList.get(i);
                UMLType otherParameterType = otherParameterTypeList.get(i);
                if (!thisParameterType.equalsQualified(otherParameterType)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + (isAbstract ? 1231 : 1237);
        result = prime * result + (thisEmptyBody ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
        result = prime * result + typeParameters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(visibility);
        sb.append(" ");
        if (isAbstract) {
            sb.append("abstract");
            sb.append(" ");
        }
        sb.append(name);
        UMLParameter returnParameter = getReturnParameter();
        List<UMLParameter> parameters = new ArrayList<>(this.parameters);
        parameters.remove(returnParameter);
        sb.append("(");
        for (int i = 0; i < parameters.size(); i++) {
            UMLParameter parameter = parameters.get(i);
            if (parameter.getKind().equals("in")) {
                sb.append(parameter);
                if (i < parameters.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append(")");
        if (returnParameter != null) {
            sb.append(" : ");
            sb.append(returnParameter.toString());
        }
        return sb.toString();
    }

    public String toQualifiedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(visibility);
        sb.append(" ");
        if (isAbstract) {
            sb.append("abstract");
            sb.append(" ");
        }
        sb.append(name);
        UMLParameter returnParameter = getReturnParameter();
        List<UMLParameter> parameters = new ArrayList<>(this.parameters);
        parameters.remove(returnParameter);
        sb.append("(");
        for (int i = 0; i < parameters.size(); i++) {
            UMLParameter parameter = parameters.get(i);
            if (parameter.getKind().equals("in")) {
                sb.append(parameter.toQualifiedString());
                if (i < parameters.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append(")");
        if (returnParameter != null) {
            sb.append(" : ");
            sb.append(returnParameter.toQualifiedString());
        }
        return sb.toString();
    }

    @Override
    public int compareTo(UMLOperation operation) {
        return this.toString().compareTo(operation.toString());
    }

    public boolean equalParameters(UMLOperation operation) {
        return this.equalReturnParameter(operation) && this.getParameters().equals(operation.getParameters());
    }

    public boolean equalParameterTypes(UMLOperation operation) {
        return this.equalReturnParameter(operation) && this.getParameterTypeList().equals(
            operation.getParameterTypeList()) && equalTypeParameters(operation);
    }

    private boolean equalTypeParameters(UMLOperation operation) {
        return this.typeParameters.equals(operation.typeParameters);
    }

    public boolean equalParameterNames(UMLOperation operation) {
        return this.equalReturnParameter(operation) && this.getParameterNameList().equals(
            operation.getParameterNameList());
    }

    public boolean overloadedParameters(UMLOperation operation) {
        return this.equalReturnParameter(operation) &&
            (this.getParameters().containsAll(operation.getParameters()) || operation.getParameters().containsAll(
                this.getParameters()));
    }

    public boolean overloadedParameterTypes(UMLOperation operation) {
        return this.equalReturnParameter(operation) &&
            (this.getParameterTypeList().containsAll(
                operation.getParameterTypeList()) || operation.getParameterTypeList().containsAll(
                this.getParameterTypeList()));
    }

    public boolean replacedParameterTypes(UMLOperation operation) {
        List<UMLType> thisParameterTypes = this.getParameterTypeList();
        List<UMLType> otherParameterTypes = operation.getParameterTypeList();

        if (thisParameterTypes.size() == otherParameterTypes.size() && thisParameterTypes.size() > 0) {
            int commonParameterTypes = 0;
            int differentParameterTypes = 0;
            for (int i = 0; i < thisParameterTypes.size(); i++) {
                UMLType thisParameterType = thisParameterTypes.get(i);
                UMLType otherParameterType = otherParameterTypes.get(i);
                if (thisParameterType.equals(otherParameterType) ||
                    (thisParameterType.getClassType().equals(
                        otherParameterType.getClassType()) &&
                        thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
                    thisParameterType.equalsWithSubType(otherParameterType)) {
                    commonParameterTypes++;
                } else {
                    differentParameterTypes++;
                }
            }
            return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
        } else if (thisParameterTypes.size() > otherParameterTypes.size() && thisParameterTypes.size() > 0) {
            int commonParameterTypes = 0;
            int differentParameterTypes = 0;
            for (int i = 0; i < otherParameterTypes.size(); i++) {
                UMLType thisParameterType = thisParameterTypes.get(i);
                UMLType otherParameterType = otherParameterTypes.get(i);
                if (thisParameterType.equals(otherParameterType) ||
                    (thisParameterType.getClassType().equals(
                        otherParameterType.getClassType()) &&
                        thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
                    thisParameterType.equalsWithSubType(otherParameterType)) {
                    commonParameterTypes++;
                } else {
                    differentParameterTypes++;
                }
            }
            return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
        } else if (otherParameterTypes.size() > thisParameterTypes.size() && thisParameterTypes.size() > 0) {
            int commonParameterTypes = 0;
            int differentParameterTypes = 0;
            for (int i = 0; i < thisParameterTypes.size(); i++) {
                UMLType thisParameterType = thisParameterTypes.get(i);
                UMLType otherParameterType = otherParameterTypes.get(i);
                if (thisParameterType.equals(otherParameterType) ||
                    (thisParameterType.getClassType().equals(
                        otherParameterType.getClassType()) &&
                        thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
                    thisParameterType.equalsWithSubType(otherParameterType)) {
                    commonParameterTypes++;
                } else {
                    differentParameterTypes++;
                }
            }
            return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
        }
        return false;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    private boolean isHashCode() {
        List<UMLType> parameterTypeList = getParameterTypeList();
        return getName().equals("hashCode") && getReturnParameter().getType().getClassType().equals(
            "Int") && parameterTypeList.size() == 0;
    }

    private boolean isToString() {
        List<UMLType> parameterTypeList = getParameterTypeList();
        return getName().equals("toString") && getReturnParameter().getType().getClassType().equals(
            "String") && parameterTypeList.size() == 0;
    }

    private boolean isClone() {
        List<UMLType> parameterTypeList = getParameterTypeList();
        return getName().equals("clone") && getReturnParameter().getType().getClassType().equals(
            "Object") && parameterTypeList.size() == 0;
    }

    private boolean isCompareTo() {
        List<UMLType> parameterTypeList = getParameterTypeList();
        return getName().equals("compareTo") && getReturnParameter().getType().getClassType().equals(
            "Int") && parameterTypeList.size() == 1;
    }

    public boolean compatibleSignature(UMLOperation removedOperation) {
        return equalParameterTypes(removedOperation) || overloadedParameterTypes(
            removedOperation) || replacedParameterTypes(removedOperation) || equalParameterNames(removedOperation);
    }

    public boolean hasTwoParametersWithTheSameType() {
        List<UMLType> parameterTypes = getParameterTypeList();
        if (parameterTypes.size() == 2) {
            return parameterTypes.get(0).equals(parameterTypes.get(1));
        }
        return false;
    }

    public Map<String, Set<String>> aliasedAttributes() {
        if (operationBody != null && isConstructor) {
            List<String> parameterNames = getParameterNameList();
            Map<String, Set<String>> map = operationBody.aliasedAttributes();
            Set<String> keysToBeRemoved = new LinkedHashSet<>();
            for (String key : map.keySet()) {
                if (!parameterNames.contains(key)) {
                    keysToBeRemoved.add(key);
                }
            }
            for (String key : keysToBeRemoved) {
                map.remove(key);
            }
            return map;
        }
        return new LinkedHashMap<>();
    }

    public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
        if (operationBody != null) {
            return operationBody.loopWithVariables(currentElementName, collectionName);
        }
        return null;
    }

    public List<OperationInvocation> getAllOperationInvocations() {
        if (operationBody != null) {
            return operationBody.getAllOperationInvocations();
        }
        return new ArrayList<>();
    }

    public List<LambdaExpressionObject> getAllLambdas() {
        if (operationBody != null)
            return operationBody.getAllLambdas();
        return new ArrayList<>();
    }

}