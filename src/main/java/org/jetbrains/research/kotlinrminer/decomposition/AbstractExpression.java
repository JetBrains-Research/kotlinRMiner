package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.LocationInfo;

import java.util.List;
import java.util.Map;

public class AbstractExpression extends AbstractCodeFragment {
    private final String expression;
    private final LocationInfo locationInfo;
    private CompositeStatementObject owner;
    private final List<String> variables;
    private final List<String> types;
    private final List<String> stringLiterals;
    private final List<String> numberLiterals;
    private final List<String> nullLiterals;
    private final List<String> booleanLiterals;
    private final List<String> typeLiterals;
    private final List<VariableDeclaration> variableDeclarations;
    private final List<String> arrayAccesses;
    private final List<String> arguments;
    private final List<String> prefixExpressions;
    private final List<String> postfixExpressions;
    private final List<LambdaExpressionObject> lambdas;
    private final Map<String, List<ObjectCreation>> creationMap;

    public AbstractExpression(KtFile cu,
                              String filePath,
                              KtExpression expression,
                              LocationInfo.CodeElementType codeElementType) {
        this.locationInfo = new LocationInfo(cu, filePath, expression, codeElementType);
        Visitor visitor = new Visitor(cu, filePath);
        expression.accept(visitor);
        this.variables = visitor.getVariables();
        this.types = visitor.getTypes();
        this.variableDeclarations = visitor.getVariableDeclarations();
        this.stringLiterals = visitor.getStringLiterals();
        this.numberLiterals = visitor.getNumberLiterals();
        this.nullLiterals = visitor.getNullLiterals();
        this.booleanLiterals = visitor.getBooleanLiterals();
        this.typeLiterals = visitor.getTypeLiterals();
        this.arrayAccesses = visitor.getArrayAccesses();
        this.arguments = visitor.getArguments();
        this.prefixExpressions = visitor.getPrefixExpressions();
        this.postfixExpressions = visitor.getPostfixExpressions();
        this.lambdas = visitor.getLambdas();
        this.expression = expression.getText();
        this.owner = null;
        this.creationMap = visitor.getCreationMap();
    }

    public void setOwner(CompositeStatementObject owner) {
        this.owner = owner;
    }

    public CompositeStatementObject getOwner() {
        return this.owner;
    }

    @Override
    public CompositeStatementObject getParent() {
        return getOwner();
    }

    public String getExpression() {
        return expression;
    }

    public String getString() {
        return toString();
    }

    public String toString() {
        return getExpression();
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
        //TODO: Implement collecting of methods' invocations
        return null;
    }

    @Override
    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        //TODO: Implement collecting of anonymous class declarations
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
        return creationMap;
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

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public VariableDeclaration searchVariableDeclaration(String variableName) {
        VariableDeclaration variableDeclaration = this.getVariableDeclaration(variableName);
        if (variableDeclaration != null) {
            return variableDeclaration;
        } else if (owner != null) {
            return owner.searchVariableDeclaration(variableName);
        }
        return null;
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

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }
}
