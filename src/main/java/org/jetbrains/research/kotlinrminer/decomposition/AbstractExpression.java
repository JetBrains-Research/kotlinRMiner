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
    private List<VariableDeclaration> variableDeclarations;

    public AbstractExpression(KtFile cu, String filePath, KtExpression expression, LocationInfo.CodeElementType codeElementType) {
        this.locationInfo = new LocationInfo(cu, filePath, expression, codeElementType);
        //TODO: to adapt Visitor
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

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }
}
