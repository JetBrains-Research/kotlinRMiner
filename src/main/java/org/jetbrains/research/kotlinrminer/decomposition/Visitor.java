package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.kotlin.psi.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

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
    private final List<String> arguments = new ArrayList<>();
    private final List<LambdaExpressionObject> lambdas = new ArrayList<>();
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final DefaultMutableTreeNode current = root;
    //TODO: implement adding of created objects to the map
    private final Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<>();

    public Visitor(KtFile file, String filePath) {
        this.ktFile = file;
        this.filePath = filePath;
    }

    @Override
    public Object visitKtElement(@NotNull KtElement element, Object data) {
        return super.visitKtElement(element, data);
    }

    @Override
    public Object visitDeclaration(@NotNull KtDeclaration dcl, Object data) {
        if (dcl instanceof KtVariableDeclaration) {
            VariableDeclaration variableDeclaration = new VariableDeclaration(dcl.getContainingKtFile(), filePath, dcl);
            variableDeclarations.add(variableDeclaration);
            variables.add(dcl.getName());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getVariableDeclarations().add(variableDeclaration);
            }
        }
        return super.visitDeclaration(dcl, data);
    }

    @Override
    public Object visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor, Object data) {
        return super.visitSecondaryConstructor(constructor, data);
    }

    @Override
    public Object visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor, Object data) {
        return super.visitPrimaryConstructor(constructor, data);
    }

    @Override
    public Object visitProperty(@NotNull KtProperty property, Object data) {
        variableDeclarations.add(new VariableDeclaration(ktFile, filePath, property));
        return super.visitProperty(property, data);
    }

    @Override
    public Object visitModifierList(@NotNull KtModifierList list, Object data) {
        return super.visitModifierList(list, data);
    }

    @Override
    public Object visitAnnotation(@NotNull KtAnnotation annotation, Object data) {
        return super.visitAnnotation(annotation, data);
    }

    @Override
    public Object visitParameter(@NotNull KtParameter parameter, Object data) {
        variableDeclarations.add(new VariableDeclaration(ktFile, filePath, parameter));
        return super.visitParameter(parameter, data);
    }

    @Override
    public Object visitLambdaExpression(@NotNull KtLambdaExpression expression, Object data) {
        LambdaExpressionObject lambda =
            new LambdaExpressionObject(expression.getContainingKtFile(), filePath, expression);
        lambdas.add(lambda);
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getLambdas().add(expression);
        }
        return super.visitLambdaExpression(expression, data);
    }

    @Override
    public Object visitCallExpression(@NotNull KtCallExpression expression, Object data) {
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
        return super.visitCallExpression(expression, data);
    }

    @Override
    public Object visitUserType(@NotNull KtUserType type, Object data) {
        types.add(type.getName());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getTypes().add(type.toString());
        }
        return super.visitUserType(type, data);
    }

    @Override
    public Object visitDynamicType(@NotNull KtDynamicType type, Object data) {
        types.add(type.getName());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getTypes().add(type.toString());
        }
        return super.visitDynamicType(type, data);
    }

    @Override
    public Object visitFunctionType(@NotNull KtFunctionType type, Object data) {
        types.add(type.getName());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getTypes().add(type.toString());
        }
        return super.visitFunctionType(type, data);
    }

    @Override
    public Object visitSelfType(@NotNull KtSelfType type, Object data) {
        types.add(type.getName());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getTypes().add(type.toString());
        }
        return super.visitSelfType(type, data);
    }

    @Override
    public Object visitNullableType(@NotNull KtNullableType nullableType, Object data) {
        types.add(nullableType.getName());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getTypes().add(nullableType.toString());
        }
        return super.visitNullableType(nullableType, data);
    }

    @Override
    public Object visitPrefixExpression(@NotNull KtPrefixExpression expression, Object data) {
        prefixExpressions.add(expression.getText());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getPrefixExpressions().add(expression.getText());
        }
        return super.visitPrefixExpression(expression, data);
    }

    @Override
    public Object visitPostfixExpression(@NotNull KtPostfixExpression expression, Object data) {
        postfixExpressions.add(expression.getText());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getPostfixExpressions().add(expression.getText());
        }
        return super.visitPostfixExpression(expression, data);
    }

    @Override
    public Object visitConstantExpression(@NotNull KtConstantExpression expression, Object data) {
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
        return super.visitConstantExpression(expression, data);
    }

    @Override
    public Object visitTypeReference(@NotNull KtTypeReference typeReference, Object data) {
        typeLiterals.add(typeReference.getText());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getTypeLiterals().add(typeReference.getText());
        }
        return super.visitTypeReference(typeReference, data);
    }

    @Override
    public Object visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression, Object data) {
        arrayAccesses.add(expression.getText());
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getArrayAccesses().add(expression.getText());
        }
        return super.visitArrayAccessExpression(expression, data);
    }

    @Override
    public Object visitThisExpression(@NotNull KtThisExpression expression, Object data) {
        if (!(expression.getParent() instanceof KtPropertyAccessor)) {
            variables.add(expression.getText());
            if (current.getUserObject() != null) {
                AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
                anonymous.getVariables().add(expression.getText());
            }
        }
        return super.visitThisExpression(expression, data);
    }

    public static String processMethodInvocation(KtCallExpression node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getCalleeExpression().getText());
        sb.append("(");
        List<KtValueArgument> arguments = node.getValueArguments();
        if (arguments.size() > 0) {
            for (int i = 0; i < arguments.size() - 1; i++)
                sb.append(arguments.get(i).getText()).append(", ");
            sb.append(arguments.get(arguments.size() - 1).getText());
        }
        sb.append(")");
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
}
