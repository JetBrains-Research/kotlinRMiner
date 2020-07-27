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
    private List<LambdaExpressionObject> lambdas;

    public StatementObject(KtFile cu, String filePath, KtExpression statement, int depth, LocationInfo.CodeElementType codeElementType) {
        super();
        /*		TODO: to adapt Visitor
         */
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
