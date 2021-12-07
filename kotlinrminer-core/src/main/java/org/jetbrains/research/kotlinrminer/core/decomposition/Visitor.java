package org.jetbrains.research.kotlinrminer.core.decomposition;

import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.*;

public class Visitor extends KtVisitor {
    private final String filePath;
    private final KtFile ktFile;
    private final List<String> variables = new ArrayList<>();
    private final List<String> types = new ArrayList<>();
    private final Map<String, List<OperationInvocation>> methodInvocationMap = new LinkedHashMap<>();
    private final List<VariableDeclaration> variableDeclarations = new ArrayList<>();
    private final List<String> stringLiterals = new ArrayList<>();
    private final List<String> numberLiterals = new ArrayList<>();
    private final List<String> nullLiterals = new ArrayList<>();
    private final List<String> booleanLiterals = new ArrayList<>();
    private final List<String> typeLiterals = new ArrayList<>();
    private final List<String> arrayAccesses = new ArrayList<>();
    private final List<String> prefixExpressions = new ArrayList<>();
    private final List<String> postfixExpressions = new ArrayList<>();
    private final List<String> infixOperators = new ArrayList<>();
    private final List<String> arguments = new ArrayList<>();
    private final List<LambdaExpressionObject> lambdas = new ArrayList<>();
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final DefaultMutableTreeNode current = root;
    //TODO: implement adding of created objects to the map
    //TODO: implement processing of objects
    private final Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<>();

    public Visitor(KtFile file, String filePath) {
        this.ktFile = file;
        this.filePath = filePath;
    }

    @Override
    public Object visitExpression(@NotNull KtExpression expression, Object data) {
        if (expression instanceof KtBinaryExpression) {
            this.processBinaryExpression((KtBinaryExpression) expression, data);
        } else if (expression instanceof KtReturnExpression) {
            this.processReturnExpression((KtReturnExpression) expression, data);
        } else if (expression instanceof KtDotQualifiedExpression) {
            this.processDotQualifiedExpression((KtDotQualifiedExpression) expression, data);
        } else if (expression instanceof KtCallExpression) {
            this.processCallExpression((KtCallExpression) expression);
        } else if (expression instanceof KtPrefixExpression) {
            this.processPrefixExpression((KtPrefixExpression) expression, data);
        } else if (expression instanceof KtPostfixExpression) {
            this.processPostfixExpression((KtPostfixExpression) expression);
        } else if (expression instanceof KtThisExpression) {
            this.processThisExpression((KtThisExpression) expression);
        } else if (expression instanceof KtConstantExpression) {
            this.processConstantExpression((KtConstantExpression) expression);
        } else if (expression instanceof KtNameReferenceExpression) {
            this.processReferenceExpression((KtNameReferenceExpression) expression);
        } else if (expression instanceof KtParenthesizedExpression) {
            this.visitExpression(((KtParenthesizedExpression) expression).getExpression(),
                data);
        } else if (expression instanceof KtStringTemplateExpression) {
            stringLiterals.add(expression.getText());
        } else if (expression instanceof KtArrayAccessExpression) {
            arrayAccesses.add(expression.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getArrayAccesses().add(expression.getText());
            }
        } else if (expression instanceof KtProperty) {
            processPropertyExpression((KtProperty) expression, data);
        } else if (expression instanceof KtVariableDeclaration) {
            VariableDeclaration variableDeclaration = new VariableDeclaration(ktFile, filePath, expression);
            variableDeclarations.add(variableDeclaration);
            visitElement(((KtProperty) expression).getIdentifyingElement());
        } else if (expression instanceof KtSafeQualifiedExpression) {
            processSafeQualifiedExpression((KtSafeQualifiedExpression) expression, data);
        } else if (expression instanceof KtLambdaExpression) {
            processLambdaExpression((KtLambdaExpression) expression);
        }
        return super.visitExpression(expression, data);
    }

    private void processDotQualifiedExpression(KtDotQualifiedExpression expression, Object data) {
        this.visitExpression(expression.getReceiverExpression(), data);
        if (expression.getSelectorExpression() != null)
            this.visitExpression(expression.getSelectorExpression(), data);
    }

    private void processReferenceExpression(KtReferenceExpression expression) {
        if (expression instanceof KtNameReferenceExpression) {
            this.variables.add(expression.getText());
        }
    }

    private void processReturnExpression(KtReturnExpression expression, Object data) {
        if (expression.getReturnedExpression() != null)
            this.visitExpression(expression.getReturnedExpression(), data);
    }

    private void processBinaryExpression(KtBinaryExpression expression, Object data) {
        String operationToken = expression.getOperationToken().toString();
        if (!operationToken.equals("EQ"))
            this.infixOperators.add(operationToken);
        if (expression.getLeft() != null)
            this.visitExpression(expression.getLeft(), data);
        if (expression.getRight() != null)
            this.visitExpression(expression.getRight(), data);
    }

    private void processPropertyExpression(KtProperty ktProperty, Object data) {
        if (ktProperty.getInitializer() != null)
            this.visitExpression(ktProperty.getInitializer(), data);
        if (ktProperty.getDelegateExpression() != null)
            this.visitExpression(ktProperty.getDelegateExpression(), data);
        if (ktProperty.getNameIdentifier() != null) {
            VariableDeclaration variableDeclaration = new VariableDeclaration(ktFile, filePath, ktProperty);
            this.variableDeclarations.add(variableDeclaration);
            this.variables.add(ktProperty.getNameIdentifier().getText());
        }
    }

    private void processSafeQualifiedExpression(KtSafeQualifiedExpression safeQualifiedExpression, Object data) {
        if (safeQualifiedExpression.getSelectorExpression() != null)
            this.visitExpression(safeQualifiedExpression.getSelectorExpression(), data);
        this.visitExpression(safeQualifiedExpression.getReceiverExpression(), data);
    }

    private void processLambdaExpression(KtLambdaExpression expression) {
        LambdaExpressionObject lambda = new LambdaExpressionObject(ktFile, filePath, expression);
        lambdas.add(lambda);
/*        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            // anonymous.getLambdas().add(lambda);
        }*/
        if (expression.getBodyExpression() != null)
            this.visitExpression(expression.getBodyExpression(), null);
    }

    private void visitArgument(KtValueArgument argument) {
        processElementType(argument.getElementType(), argument);
        KtExpression argumentExpression = argument.getArgumentExpression();
        if (argumentExpression instanceof KtConstantExpression) {
            KtConstantExpression constantExpression = (KtConstantExpression) argumentExpression;
            processElementType(constantExpression.getElementType(), argument);
        } else if (argumentExpression instanceof KtStringTemplateExpression) {
            KtStringTemplateExpression stringTemplateExpression = (KtStringTemplateExpression) argumentExpression;
            processElementType(stringTemplateExpression.getElementType(), argument);
        } else if (argument instanceof KtLambdaArgument) {
            KtLambdaArgument lambdaArgument = (KtLambdaArgument) argument;
            if (lambdaArgument.getLambdaExpression() != null)
                this.visitExpression(lambdaArgument.getLambdaExpression(), null);
        } else if (argument.getArgumentExpression() instanceof KtObjectLiteralExpression) {
            //TODO: process object's creations
            KtObjectLiteralExpression objectLiteralExpression =
                (KtObjectLiteralExpression) argument.getArgumentExpression();
        } else if (argumentExpression instanceof KtCallExpression) {
            processCallExpression((KtCallExpression) argumentExpression);
        }
    }

    private void processElementType(IStubElementType type, KtValueArgument argument) {
        if (type == INTEGER_CONSTANT || type == FLOAT_CONSTANT) {
            numberLiterals.add(argument.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous =
                    (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getNumberLiterals().add(argument.getText());
            }
        } else if (type == STRING_TEMPLATE) {
            stringLiterals.add(argument.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous =
                    (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getStringLiterals().add(argument.getText());
            }
        }
    }

    private void processCallExpression(KtCallExpression expression) {
        List<KtValueArgument> arguments = expression.getValueArguments();
        for (KtValueArgument argument : arguments) {
            processArgument(argument);
        }
        String methodInvocation = processMethodInvocation(expression);
        OperationInvocation invocation =
            new OperationInvocation(ktFile, filePath, expression);
        if (methodInvocationMap.containsKey(methodInvocation)) {
            methodInvocationMap.get(methodInvocation).add(invocation);
        } else {
            List<OperationInvocation> list = new ArrayList<>();
            list.add(invocation);
            methodInvocationMap.put(methodInvocation, list);
        }
    }

    private void processPrefixExpression(KtPrefixExpression expression, Object data) {
        prefixExpressions.add(expression.getText());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getPrefixExpressions().add(expression.getText());
        }
        if (expression.getBaseExpression() != null)
            this.visitExpression(expression.getBaseExpression(), data);
    }

    private void processPostfixExpression(KtPostfixExpression expression) {
        postfixExpressions.add(expression.getText());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getPostfixExpressions().add(expression.getText());
        }
    }

    private void processConstantExpression(KtConstantExpression expression) {
        IStubElementType elementType = expression.getElementType();
        if (elementType == BOOLEAN_CONSTANT) {
            booleanLiterals.add(expression.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getBooleanLiterals().add(expression.getText());
            }
        } else if (elementType == NULL) {
            nullLiterals.add(expression.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getNullLiterals().add(expression.getText());
            }
        } else if (elementType == INTEGER_CONSTANT || elementType == FLOAT_CONSTANT) {
            numberLiterals.add(expression.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getNumberLiterals().add(expression.getText());
            }
        } else if (elementType == STRING_TEMPLATE) {
            stringLiterals.add(expression.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getStringLiterals().add(expression.getText());
            }
        }
    }

    private void processThisExpression(KtThisExpression expression) {
        if (!(expression.getParent() instanceof KtPropertyAccessor)) {
            variables.add(expression.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getVariables().add(expression.getText());
            }
        }
    }

    private void processArgument(KtValueArgument argument) {
        this.arguments.add(argument.getText());
        visitArgument(argument);
        if (argument.getArgumentExpression() instanceof KtNameReferenceExpression) {
            variables.add(argument.getText());
        }
    }

    private static String processMethodInvocation(KtCallExpression node) {
        StringBuilder sb = new StringBuilder();
        if (node.getPrevSibling() != null && node.getPrevSibling().getParent() != null) {
            if (node.getPrevSibling().getParent() instanceof KtDotQualifiedExpression) {
                sb.append(node.getPrevSibling().getParent().getText());
            } else {
                sb.append(node.getCalleeExpression().getContext().getText());
            }
        } else {
            sb.append(node.getCalleeExpression().getContext().getText());
        }
        return sb.toString();
    }

    public List<String> getVariables() {
        return variables;
    }

    public List<String> getArrayAccesses() {
        return arrayAccesses;
    }

    public List<String> getPrefixExpressions() {
        return prefixExpressions;
    }

    public List<String> getPostfixExpressions() {
        return postfixExpressions;
    }

    public List<String> getStringLiterals() {
        return stringLiterals;
    }

    public List<String> getNumberLiterals() {
        return numberLiterals;
    }

    public List<String> getNullLiterals() {
        return nullLiterals;
    }

    public List<String> getBooleanLiterals() {
        return booleanLiterals;
    }

    public List<String> getTypeLiterals() {
        return typeLiterals;
    }

    public List<VariableDeclaration> getVariableDeclarations() {
        return variableDeclarations;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<LambdaExpressionObject> getLambdas() {
        return lambdas;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public Map<String, List<ObjectCreation>> getCreationMap() {
        return creationMap;
    }

    public Map<String, List<OperationInvocation>> getMethodInvocationMap() {
        return this.methodInvocationMap;
    }

    public List<String> getInfixOperators() {
        return infixOperators;
    }

}
