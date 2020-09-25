package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.LocationInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatementObject extends AbstractStatement {
    private final String statement;
    private final LocationInfo locationInfo;
    private final List<String> variables;
    private final List<String> types;
    private final List<VariableDeclaration> variableDeclarations;
    private Map<String, List<OperationInvocation>> methodInvocationMap;
    //TODO private List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
    private final List<String> stringLiterals;
    private final List<String> numberLiterals;
    private final List<String> nullLiterals;
    private final List<String> booleanLiterals;
    private final List<String> typeLiterals;
    //TODO private Map<String, List<ObjectCreation>> creationMap;
    private final List<String> arrayAccesses;
    private final List<String> prefixExpressions;
    private final List<String> postfixExpressions;
    private final List<String> arguments;
    private final List<LambdaExpressionObject> lambdas;

    public StatementObject(KtFile cu,
                           String filePath,
                           KtExpression statement,
                           int depth,
                           LocationInfo.CodeElementType codeElementType) {
        super();
        this.locationInfo = new LocationInfo(cu, filePath, statement, codeElementType);
        Visitor visitor = new Visitor(cu, filePath);
        statement.accept(visitor);
        this.variables = visitor.getVariables();
        this.types = visitor.getTypes();
        this.variableDeclarations = visitor.getVariableDeclarations();
        this.methodInvocationMap = visitor.getMethodInvocationMap();
        this.stringLiterals = visitor.getStringLiterals();
        this.numberLiterals = visitor.getNumberLiterals();
        this.nullLiterals = visitor.getNullLiterals();
        this.booleanLiterals = visitor.getBooleanLiterals();
        this.typeLiterals = visitor.getTypeLiterals();
        this.arrayAccesses = visitor.getArrayAccesses();
        this.prefixExpressions = visitor.getPrefixExpressions();
        this.postfixExpressions = visitor.getPostfixExpressions();
        this.arguments = visitor.getArguments();
        this.lambdas = visitor.getLambdas();
        setDepth(depth);
        this.statement = statement.getText();
    }

    public List<String> stringRepresentation() {
        List<String> stringRepresentation = new ArrayList<>();
        stringRepresentation.add(this.toString());
        return stringRepresentation;
    }

    @Override
    public List<StatementObject> getLeaves() {
        List<StatementObject> leaves = new ArrayList<>();
        leaves.add(this);
        return leaves;
    }

    public String toString() {
        return statement;
    }

    @Override
    public List<String> getVariables() {
        return variables;
    }

    @Override
    public List<String> getTypes() {
        return types;
    }

    @Override
    public List<VariableDeclaration> getVariableDeclarations() {
        return variableDeclarations;
    }

    @Override
    public Map<String, List<OperationInvocation>> getMethodInvocationMap() {
        return this.methodInvocationMap;
    }

    @Override
    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        // TODO: implement it
        return null;
    }

    @Override
    public List<String> getStringLiterals() {
        return stringLiterals;
    }

    @Override
    public List<String> getNumberLiterals() {
        return numberLiterals;
    }

    @Override
    public List<String> getNullLiterals() {
        return nullLiterals;
    }

    @Override
    public List<String> getBooleanLiterals() {
        return booleanLiterals;
    }

    @Override
    public List<String> getTypeLiterals() {
        return typeLiterals;
    }

    @Override
    public Map<String, List<ObjectCreation>> getCreationMap() {
        return new HashMap<>();
    }

    @Override
    public List<String> getArrayAccesses() {
        return arrayAccesses;
    }

    @Override
    public List<String> getPrefixExpressions() {
        return prefixExpressions;
    }

    @Override
    public List<String> getPostfixExpressions() {
        return postfixExpressions;
    }

    @Override
    public List<String> getArguments() {
        return arguments;
    }

    @Override
    public int statementCount() {
        return 1;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    public VariableDeclaration getVariableDeclaration(String variableName) {
        List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
        for (VariableDeclaration declaration : variableDeclarations) {
            if (declaration.getVariableName().equals(variableName)) {
                return declaration;
            }
        }
        return null;
    }

    @Override
    public List<LambdaExpressionObject> getLambdas() {
        return lambdas;
    }
}
