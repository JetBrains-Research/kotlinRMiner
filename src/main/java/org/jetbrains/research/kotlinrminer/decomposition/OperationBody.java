package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.kotlin.psi.*;
import org.jetbrains.research.kotlinrminer.LocationInfo;
import org.jetbrains.research.kotlinrminer.LocationInfo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OperationBody {

    private CompositeStatementObject compositeStatement;

    public OperationBody(KtFile cu, String filePath, KtBlockExpression methodBody) {
        this.compositeStatement = new CompositeStatementObject(cu, filePath, methodBody, 0, CodeElementType.BLOCK);
        List<KtExpression> statements = methodBody.getStatements();
        for (KtExpression statement : statements) {
            processStatement(cu, filePath, compositeStatement, statement);
        }
    }

    public int statementCount() {
        return compositeStatement.statementCount();
    }

    public CompositeStatementObject getCompositeStatement() {
        return compositeStatement;
    }

    public List<String> getAllVariables() {
        return new ArrayList<>(compositeStatement.getAllVariables());
    }

    public List<VariableDeclaration> getAllVariableDeclarations() {
        return new ArrayList<>(compositeStatement.getAllVariableDeclarations());
    }

    public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
        return new ArrayList<>(compositeStatement.getVariableDeclarationsInScope(location));
    }

    public VariableDeclaration getVariableDeclaration(String variableName) {
        return compositeStatement.getVariableDeclaration(variableName);
    }

    private void processStatement(KtFile cu, String filePath, CompositeStatementObject parent, KtExpression statement) {
        if (statement instanceof KtBlockExpression) {
            KtBlockExpression block = (KtBlockExpression) statement;
            List<KtExpression> blockStatements = block.getStatements();
            CompositeStatementObject child = new CompositeStatementObject(cu, filePath, block, parent.getDepth() + 1, CodeElementType.BLOCK);
            parent.addStatement(child);
            for (KtExpression blockStatement : blockStatements) {
                processStatement(cu, filePath, child, blockStatement);
            }
        } else if (statement instanceof KtIfExpression) {
            KtIfExpression ifStatement = (KtIfExpression) statement;
            CompositeStatementObject child = new CompositeStatementObject(cu, filePath, ifStatement, parent.getDepth() + 1, CodeElementType.IF_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, ifStatement.getCondition(), CodeElementType.IF_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            processStatement(cu, filePath, child, ifStatement.getThen());
            if (ifStatement.getElse() != null) {
                processStatement(cu, filePath, child, ifStatement.getElse());
            }
        } else if (statement instanceof KtForExpression) {
            KtForExpression forStatement = (KtForExpression) statement;
            CompositeStatementObject child = new CompositeStatementObject(cu, filePath, forStatement, parent.getDepth() + 1, CodeElementType.FOR_STATEMENT);
            parent.addStatement(child);
            KtExpression ktDeclaration = forStatement.getDestructuringDeclaration();
            if (ktDeclaration != null) {
                KtExpression initializer = forStatement.getDestructuringDeclaration().getInitializer();
                AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, initializer, CodeElementType.FOR_STATEMENT_INITIALIZER);
                child.addExpression(abstractExpression);
            }
            processStatement(cu, filePath, child, forStatement.getBody());
        } else if (statement instanceof KtWhileExpression) {
            KtWhileExpression whileStatement = (KtWhileExpression) statement;
            CompositeStatementObject child = new CompositeStatementObject(cu, filePath, whileStatement, parent.getDepth() + 1, CodeElementType.WHILE_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            processStatement(cu, filePath, child, whileStatement.getBody());
        } else if (statement instanceof KtDoWhileExpression) {
            KtDoWhileExpression doStatement = (KtDoWhileExpression) statement;
            CompositeStatementObject child = new CompositeStatementObject(cu, filePath, doStatement, parent.getDepth() + 1, CodeElementType.DO_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, doStatement.getCondition(), CodeElementType.DO_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            processStatement(cu, filePath, child, doStatement.getBody());
        } else if (statement instanceof KtLabeledExpression) {
            KtLabeledExpression labeledStatement = (KtLabeledExpression) statement;
            KtSimpleNameExpression label = labeledStatement.getTargetLabel();
            CompositeStatementObject child = new CompositeStatementObject(cu, filePath, labeledStatement, parent.getDepth() + 1, CodeElementType.LABELED_STATEMENT.setName(label.getName()));
            parent.addStatement(child);
            processStatement(cu, filePath, child, labeledStatement.getBaseExpression());
        } else if (statement instanceof KtReturnExpression) {
            KtReturnExpression returnStatement = (KtReturnExpression) statement;
            StatementObject child = new StatementObject(cu, filePath, returnStatement, parent.getDepth() + 1, CodeElementType.RETURN_STATEMENT);
            parent.addStatement(child);
        } else if (statement instanceof KtThrowExpression) {
            KtThrowExpression throwStatement = (KtThrowExpression) statement;
            StatementObject child = new StatementObject(cu, filePath, throwStatement, parent.getDepth() + 1, CodeElementType.THROW_STATEMENT);
            parent.addStatement(child);
        } else if (statement instanceof KtTryExpression) {
            KtTryExpression tryStatement = (KtTryExpression) statement;
            //   TryStatementObject child = new TryStatementObject(cu, filePath, tryStatement, parent.getDepth() + 1);
            // parent.addStatement(child);
/*          TODO: process resources
            List<Expression> resources = tryStatement.resources();
            for (Expression resource : resources) {
                AbstractExpression expression = new AbstractExpression(cu, filePath, resource, CodeElementType.TRY_STATEMENT_RESOURCE);
                child.addExpression(expression);
            }*/
/*            List<KtExpression> tryStatements = tryStatement.getTryBlock().getStatements();
            for (KtExpression blockStatement : tryStatements) {
                processStatement(cu, filePath, child, blockStatement);
            }*/
            List<KtCatchClause> catchClauses = tryStatement.getCatchClauses();
            for (KtCatchClause catchClause : catchClauses) {
                KtExpression catchClauseBody = catchClause.getCatchBody();
                CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(cu, filePath, catchClauseBody, parent.getDepth() + 1, CodeElementType.CATCH_CLAUSE);
                //TODO:  child.addCatchClause(catchClauseStatementObject);
                parent.addStatement(catchClauseStatementObject);
/*              TODO: process catch clauses
                SingleVariableDeclaration variableDeclaration = catchClause.getException();
                VariableDeclaration vd = new VariableDeclaration(cu, filePath, variableDeclaration);
                catchClauseStatementObject.addVariableDeclaration(vd);
                AbstractExpression variableDeclarationName = new AbstractExpression(cu, filePath, variableDeclaration.getName(), CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME);
                catchClauseStatementObject.addExpression(variableDeclarationName);
                if (variableDeclaration.getInitializer() != null) {
                    AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER);
                    catchClauseStatementObject.addExpression(variableDeclarationInitializer);
                }*/
/*                List<Statement> blockStatements = catchClauseBody.statements();
                for (Statement blockStatement : blockStatements) {
                    processStatement(cu, filePath, catchClauseStatementObject, blockStatement);
                }*/
            }
            KtFinallySection finallyBlock = tryStatement.getFinallyBlock();
            if (finallyBlock != null) {
                CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(cu, filePath, finallyBlock, parent.getDepth() + 1, CodeElementType.FINALLY_BLOCK);
                //TODO: child.setFinallyClause(finallyClauseStatementObject);
                parent.addStatement(finallyClauseStatementObject);
                List<KtExpression> blockStatements = finallyBlock.getFinalExpression().getStatements();
                for (KtExpression blockStatement : blockStatements) {
                    processStatement(cu, filePath, finallyClauseStatementObject, blockStatement);
                }
            }
        } else if (statement instanceof KtConstructorCalleeExpression) {
            KtConstructorCalleeExpression constructorInvocation = (KtConstructorCalleeExpression) statement;
            StatementObject child = new StatementObject(cu, filePath, constructorInvocation, parent.getDepth() + 1, CodeElementType.CONSTRUCTOR_INVOCATION);
            parent.addStatement(child);
        } else if (statement instanceof KtSuperExpression) {
            KtSuperExpression superConstructorInvocation = (KtSuperExpression) statement;
            StatementObject child = new StatementObject(cu, filePath, superConstructorInvocation, parent.getDepth() + 1, CodeElementType.SUPER_CONSTRUCTOR_INVOCATION);
            parent.addStatement(child);
        } else if (statement instanceof KtBreakExpression) {
            KtBreakExpression breakStatement = (KtBreakExpression) statement;
            StatementObject child = new StatementObject(cu, filePath, breakStatement, parent.getDepth() + 1, CodeElementType.BREAK_STATEMENT);
            parent.addStatement(child);
        } else if (statement instanceof KtContinueExpression) {
            KtContinueExpression continueStatement = (KtContinueExpression) statement;
            StatementObject child = new StatementObject(cu, filePath, continueStatement, parent.getDepth() + 1, CodeElementType.CONTINUE_STATEMENT);
            parent.addStatement(child);
        }
    }

    public Map<String, Set<String>> aliasedAttributes() {
        return compositeStatement.aliasedAttributes();
    }

    public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
        return compositeStatement.loopWithVariables(currentElementName, collectionName);
    }
}
