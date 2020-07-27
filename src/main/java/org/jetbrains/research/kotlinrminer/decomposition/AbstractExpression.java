package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.LocationInfo;

import java.util.List;

public class AbstractExpression extends AbstractCodeFragment {
    private String expression;
    private LocationInfo locationInfo;
    private CompositeStatementObject owner;
    private List<String> variables;
    private List<String> types;
    private List<String> stringLiterals;
    private List<String> numberLiterals;
    private List<String> nullLiterals;
    private List<String> booleanLiterals;
    private List<String> typeLiterals;
    private List<VariableDeclaration> variableDeclarations;
    private List<String> arrayAccesses;
    private List<String> prefixExpressions;
    private List<String> postfixExpressions;
    private List<LambdaExpressionObject> lambdas;

    public AbstractExpression(KtFile cu, String filePath, KtExpression expression, LocationInfo.CodeElementType codeElementType) {
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
        this.prefixExpressions = visitor.getPrefixExpressions();
        this.postfixExpressions = visitor.getPostfixExpressions();
        this.lambdas = visitor.getLambdas();
        this.expression = expression.getText();
        this.owner = null;
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
        return getExpression().toString();
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
