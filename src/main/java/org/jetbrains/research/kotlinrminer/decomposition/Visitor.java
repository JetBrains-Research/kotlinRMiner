package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

public class Visitor extends KtVisitor {

    private String filePath;
    private KtFile ktFile;
    private List<String> variables = new ArrayList<String>();
    private List<String> types = new ArrayList<String>();
    private Map<String, List<KtCallExpression>> methodInvocationMap = new LinkedHashMap<String, List<KtCallExpression>>();
    private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
    private List<String> stringLiterals = new ArrayList<String>();
    private List<String> numberLiterals = new ArrayList<String>();
    private List<String> nullLiterals = new ArrayList<String>();
    private List<String> booleanLiterals = new ArrayList<String>();
    private List<String> typeLiterals = new ArrayList<String>();
    private List<KtLambdaExpression> lambdas = new ArrayList<KtLambdaExpression>();
    private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private DefaultMutableTreeNode current = root;

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
        return super.visitDeclaration(dcl, data);
    }

    @Override
    public Object visitClass(@NotNull KtClass klass, Object data) {
        return super.visitClass(klass, data);
    }

    @Override
    public Object visitObjectDeclaration(@NotNull KtObjectDeclaration declaration, Object data) {
        return super.visitObjectDeclaration(declaration, data);
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
    public Object visitNamedFunction(@NotNull KtNamedFunction function, Object data) {
        List<KtCallExpression> invocations = methodInvocationMap.getOrDefault(function.getName(), new ArrayList<>());
        invocations.addAll(PsiTreeUtil.findChildrenOfType(function, KtCallExpression.class));
        Collection<KtVariableDeclaration> variableDecl = PsiTreeUtil.findChildrenOfType(function, KtVariableDeclaration.class);
        variableDecl.forEach(v -> variableDeclarations.add(new VariableDeclaration(ktFile, filePath, v)));
        return super.visitNamedFunction(function, data);
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
        lambdas.add(expression);
        if (current.getUserObject() != null) {
            AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
            anonymous.getLambdas().add(expression);
        }
        return super.visitLambdaExpression(expression, data);
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
}
