package org.jetbrains.research.kotlinrminer.cli.decomposition;

import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.research.kotlinrminer.common.decomposition.CodeElementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OperationBody {

    private final CompositeStatementObject compositeStatement;
    private final boolean isEmpty;

    public OperationBody(KtFile cu, String filePath, KtExpression methodBody, CodeElementType codeElementType) {
        this.compositeStatement = new CompositeStatementObject(cu, filePath, methodBody, 0, codeElementType);
        boolean empty = true;
        for (PsiElement child = methodBody.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof KtExpression) {
                empty = false;
                processStatement(cu, filePath, compositeStatement, (KtExpression) child);
            }
        }
        this.isEmpty = empty;
    }

    public OperationBody(KtFile cu, String filePath, KtBlockExpression methodBody) {
        this(cu, filePath, methodBody, CodeElementType.BLOCK);
    }

    public boolean isEmpty() {
        return isEmpty;
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

    private void processStatement(KtFile ktFile,
                                  String filePath,
                                  CompositeStatementObject parent,
                                  KtExpression statement) {
        if (statement instanceof KtBlockExpression) {
            KtBlockExpression block = (KtBlockExpression) statement;
            List<KtExpression> blockStatements = block.getStatements();
            CompositeStatementObject child =
                new CompositeStatementObject(ktFile, filePath, block, parent.getDepth() + 1, CodeElementType.BLOCK);
            parent.addStatement(child);
            for (KtExpression blockStatement : blockStatements) {
                processStatement(ktFile, filePath, child, blockStatement);
            }
        } else if (statement instanceof KtIfExpression) {
            KtIfExpression ifStatement = (KtIfExpression) statement;
            CompositeStatementObject child =
                new CompositeStatementObject(ktFile, filePath, ifStatement, parent.getDepth() + 1,
                                             CodeElementType.IF_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(ktFile, filePath, ifStatement.getCondition(),
                                                                           CodeElementType.IF_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            processStatement(ktFile, filePath, child, ifStatement.getThen());
            if (ifStatement.getElse() != null) {
                processStatement(ktFile, filePath, child, ifStatement.getElse());
            }
        } else if (statement instanceof KtForExpression) {
            KtForExpression forStatement = (KtForExpression) statement;
            CompositeStatementObject child = new CompositeStatementObject(ktFile, filePath,
                                                                          forStatement, parent.getDepth() + 1,
                                                                          CodeElementType.ENHANCED_FOR_STATEMENT);

            parent.addStatement(child);
            VariableDeclaration variableDeclaration =
                new VariableDeclaration(ktFile, filePath, forStatement.getLoopParameter());
            child.addVariableDeclaration(variableDeclaration);
            AbstractExpression abstractEx = new AbstractExpression(ktFile, filePath, forStatement.getLoopParameter(),
                                                                   CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME);
            child.addExpression(abstractEx);
            KtDestructuringDeclaration ktDeclaration = forStatement.getDestructuringDeclaration();
            if (ktDeclaration != null && ktDeclaration.getInitializer() != null) {
                KtExpression initializer = ktDeclaration.getInitializer();
                AbstractExpression abstractExpression = new AbstractExpression(ktFile, filePath, initializer,
                                                                               CodeElementType.FOR_STATEMENT_INITIALIZER);
                child.addExpression(abstractExpression);
            }
            KtExpression rangeExpr = forStatement.getLoopRange();
            AbstractExpression range = new AbstractExpression(ktFile, filePath, rangeExpr,
                                                              CodeElementType.ENHANCED_FOR_STATEMENT_RANGE);
            child.addExpression(range);
            if (ktDeclaration != null) {
                AbstractExpression abstractExpr = new AbstractExpression(ktFile, filePath, ktDeclaration,
                                                                         CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION);
                child.addExpression(abstractExpr);
            }
            processStatement(ktFile, filePath, child, forStatement.getBody());
        } else if (statement instanceof KtWhileExpression) {
            KtWhileExpression whileStatement = (KtWhileExpression) statement;
            CompositeStatementObject child =
                new CompositeStatementObject(ktFile, filePath, whileStatement, parent.getDepth() + 1,
                                             CodeElementType.WHILE_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression =
                new AbstractExpression(ktFile, filePath, whileStatement.getCondition(),
                                       CodeElementType.WHILE_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            processStatement(ktFile, filePath, child, whileStatement.getBody());
        } else if (statement instanceof KtDoWhileExpression) {
            KtDoWhileExpression doStatement = (KtDoWhileExpression) statement;
            CompositeStatementObject child =
                new CompositeStatementObject(ktFile, filePath, doStatement, parent.getDepth() + 1,
                                             CodeElementType.DO_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(ktFile, filePath, doStatement.getCondition(),
                                                                           CodeElementType.DO_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            processStatement(ktFile, filePath, child, doStatement.getBody());
        } else if (statement instanceof KtLabeledExpression) {
            KtLabeledExpression labeledStatement = (KtLabeledExpression) statement;
            KtSimpleNameExpression label = labeledStatement.getTargetLabel();
            CompositeStatementObject child =
                new CompositeStatementObject(ktFile, filePath, labeledStatement, parent.getDepth() + 1,
                                             CodeElementType.LABELED_STATEMENT.setName(label.getName()));
            parent.addStatement(child);
            processStatement(ktFile, filePath, child, labeledStatement.getBaseExpression());
        } else if (statement instanceof KtReturnExpression) {
            KtReturnExpression returnStatement = (KtReturnExpression) statement;
            StatementObject child = new StatementObject(ktFile, filePath, returnStatement, parent.getDepth() + 1,
                                                        CodeElementType.RETURN_STATEMENT);
            parent.addStatement(child);
        } else if (statement instanceof KtThrowExpression) {
            KtThrowExpression throwStatement = (KtThrowExpression) statement;
            StatementObject child = new StatementObject(ktFile, filePath, throwStatement, parent.getDepth() + 1,
                                                        CodeElementType.THROW_STATEMENT);
            parent.addStatement(child);
        } else if (statement instanceof KtTryExpression) {
            KtTryExpression tryStatement = (KtTryExpression) statement;
            TryStatementObject child = new TryStatementObject(ktFile, filePath, tryStatement, parent.getDepth() + 1);
            parent.addStatement(child);
            List<KtCatchClause> catchClauses = tryStatement.getCatchClauses();
            for (KtCatchClause catchClause : catchClauses) {
                KtExpression catchClauseBody = catchClause.getCatchBody();
                CompositeStatementObject catchClauseStatementObject =
                    new CompositeStatementObject(ktFile, filePath, catchClauseBody, parent.getDepth() + 1,
                                                 CodeElementType.CATCH_CLAUSE);
                child.addCatchClause(catchClauseStatementObject);
                parent.addStatement(catchClauseStatementObject);

                KtParameter variableDeclaration = catchClause.getCatchParameter();
                VariableDeclaration vd = new VariableDeclaration(ktFile, filePath, variableDeclaration);
                catchClauseStatementObject.addVariableDeclaration(vd);
                AbstractExpression variableDeclarationName =
                    new AbstractExpression(ktFile, filePath, variableDeclaration,
                                           CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME);
                catchClauseStatementObject.addExpression(variableDeclarationName);

                if (catchClauseBody instanceof KtBlockExpression) {
                    KtBlockExpression block = (KtBlockExpression) catchClauseBody;
                    List<KtExpression> blockStatements = block.getStatements();
                    for (KtExpression expression : blockStatements) {
                        processStatement(ktFile, filePath, catchClauseStatementObject, expression);
                    }
                }
            }
            KtFinallySection finallyBlock = tryStatement.getFinallyBlock();
            if (finallyBlock != null) {
                CompositeStatementObject finallyClauseStatementObject =
                    new CompositeStatementObject(ktFile, filePath, finallyBlock, parent.getDepth() + 1,
                                                 CodeElementType.FINALLY_BLOCK);
                child.setFinallyClause(finallyClauseStatementObject);
                parent.addStatement(finallyClauseStatementObject);
                List<KtExpression> blockStatements = finallyBlock.getFinalExpression().getStatements();
                for (KtExpression blockStatement : blockStatements) {
                    processStatement(ktFile, filePath, finallyClauseStatementObject, blockStatement);
                }
            }
        } else if (statement instanceof KtConstructorCalleeExpression) {
            KtConstructorCalleeExpression constructorInvocation = (KtConstructorCalleeExpression) statement;
            StatementObject child = new StatementObject(ktFile, filePath, constructorInvocation, parent.getDepth() + 1,
                                                        CodeElementType.CONSTRUCTOR_INVOCATION);
            parent.addStatement(child);
        } else if (statement instanceof KtSuperExpression) {
            KtSuperExpression superConstructorInvocation = (KtSuperExpression) statement;
            StatementObject child =
                new StatementObject(ktFile, filePath, superConstructorInvocation, parent.getDepth() + 1,
                                    CodeElementType.SUPER_CONSTRUCTOR_INVOCATION);
            parent.addStatement(child);
        } else if (statement instanceof KtBreakExpression) {
            KtBreakExpression breakStatement = (KtBreakExpression) statement;
            StatementObject child = new StatementObject(ktFile, filePath, breakStatement, parent.getDepth() + 1,
                                                        CodeElementType.BREAK_STATEMENT);
            parent.addStatement(child);
        } else if (statement instanceof KtContinueExpression) {
            KtContinueExpression continueStatement = (KtContinueExpression) statement;
            StatementObject child = new StatementObject(ktFile, filePath, continueStatement, parent.getDepth() + 1,
                                                        CodeElementType.CONTINUE_STATEMENT);
            parent.addStatement(child);
        } else if (statement instanceof KtWhenExpression) {
            KtWhenExpression whenExpression = (KtWhenExpression) statement;
            CompositeStatementObject child =
                new CompositeStatementObject(ktFile, filePath, whenExpression, parent.getDepth() + 1,
                                             CodeElementType.WHEN_EXPRESSION);
            parent.addStatement(child);
            if (whenExpression.getSubjectExpression() != null) {
                AbstractExpression abstractExpression =
                    new AbstractExpression(ktFile, filePath, whenExpression.getSubjectExpression(),
                                           CodeElementType.WHEN_EXPRESSION);
                child.addExpression(abstractExpression);
                processStatement(ktFile, filePath, child, whenExpression.getSubjectExpression());
            }

            if (whenExpression.getElseExpression() != null) {
                processStatement(ktFile, filePath, child, whenExpression.getElseExpression());
            }

            List<KtWhenEntry> entries = whenExpression.getEntries();
            for (KtWhenEntry entry : entries) {
                if (entry.getConditions().length > 0 && entry.getConditions()[0] != null && entry.getConditions()[0] instanceof KtWhenConditionWithExpression) {
                    AbstractExpression abstractExpression = new AbstractExpression(ktFile, filePath,
                                                                                   ((KtWhenConditionWithExpression) entry.getConditions()[0]).getExpression(),
                                                                                   CodeElementType.WHEN_EXPRESSION_CONDITION);
                    child.addExpression(abstractExpression);
                }
                processStatement(ktFile, filePath, child, entry.getExpression());
            }
        } else if (statement instanceof KtProperty) {
            KtProperty variableDeclarationStatement = (KtProperty) statement;
            StatementObject child = new StatementObject(ktFile, filePath, variableDeclarationStatement,
                                                        parent.getDepth() + 1,
                                                        CodeElementType.VARIABLE_DECLARATION_STATEMENT);
            parent.addStatement(child);
        } else {
            StatementObject child = new StatementObject(ktFile, filePath, statement, parent.getDepth() + 1,
                                                        CodeElementType.EXPRESSION_STATEMENT);
            parent.addStatement(child);
        }
    }

    public Map<String, Set<String>> aliasedAttributes() {
        return compositeStatement.aliasedAttributes();
    }

    public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
        return compositeStatement.loopWithVariables(currentElementName, collectionName);
    }

    public List<String> stringRepresentation() {
        return compositeStatement.stringRepresentation();
    }

    public List<OperationInvocation> getAllOperationInvocations() {
        List<OperationInvocation> invocations = new ArrayList<>();
        Map<String, List<OperationInvocation>> invocationMap = compositeStatement.getAllMethodInvocations();
        for (String key : invocationMap.keySet()) {
            invocations.addAll(invocationMap.get(key));
        }
        return invocations;
    }

    public List<LambdaExpressionObject> getAllLambdas() {
        return new ArrayList<>(compositeStatement.getAllLambdas());
    }
}
