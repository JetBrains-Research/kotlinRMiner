package org.jetbrains.research.kotlinrminer.cli.decomposition;

import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtTryExpression;
import org.jetbrains.research.kotlinrminer.common.decomposition.CodeElementType;

import java.util.ArrayList;
import java.util.List;

public class TryStatementObject extends CompositeStatementObject {
    private final List<CompositeStatementObject> catchClauses;
    private CompositeStatementObject finallyClause;

    public TryStatementObject(KtFile cu, String filePath, KtTryExpression statement, int depth) {
        super(cu, filePath, statement, depth, CodeElementType.TRY_STATEMENT);
        this.catchClauses = new ArrayList<>();
    }

    public void addCatchClause(CompositeStatementObject catchClause) {
        catchClauses.add(catchClause);
    }

    public List<CompositeStatementObject> getCatchClauses() {
        return catchClauses;
    }

    public void setFinallyClause(CompositeStatementObject finallyClause) {
        this.finallyClause = finallyClause;
    }

    public CompositeStatementObject getFinallyClause() {
        return finallyClause;
    }

    @Override
    public List<VariableDeclaration> getVariableDeclarations() {
        List<VariableDeclaration> variableDeclarations = new ArrayList<>(super.getVariableDeclarations());
        for (CompositeStatementObject catchClause : catchClauses) {
            variableDeclarations.addAll(catchClause.getVariableDeclarations());
        }
        return variableDeclarations;
    }
}
