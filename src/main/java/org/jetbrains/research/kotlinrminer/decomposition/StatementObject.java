package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.LocationInfo;

import java.util.ArrayList;
import java.util.List;

public class StatementObject extends AbstractStatement {
    private String statement;
    private LocationInfo locationInfo;
    private List<String> variables;
    private List<String> types;
    private List<VariableDeclaration> variableDeclarations;
    //TODO private Map<String, List<OperationInvocation>> methodInvocationMap;
    //TODO private List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
    private List<String> stringLiterals;
    private List<String> numberLiterals;
    private List<String> nullLiterals;
    private List<String> booleanLiterals;
    private List<String> typeLiterals;
    //TODO private Map<String, List<ObjectCreation>> creationMap;
    private List<String> arrayAccesses;
    private List<String> prefixExpressions;
    private List<String> postfixExpressions;
    private List<String> arguments;
    //TODO private List<LambdaExpressionObject> lambdas;
    private List<LambdaExpressionObject> lambdas;

    public StatementObject(KtFile cu, String filePath, KtExpression statement, int depth, LocationInfo.CodeElementType codeElementType) {
        super();
        this.locationInfo = new LocationInfo(cu, filePath, statement, codeElementType);
        Visitor visitor = new Visitor(cu, filePath);
        statement.accept(visitor);
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
        this.arguments = visitor.getArguments();
        //this.lambdas = visitor.getLambdas();
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
