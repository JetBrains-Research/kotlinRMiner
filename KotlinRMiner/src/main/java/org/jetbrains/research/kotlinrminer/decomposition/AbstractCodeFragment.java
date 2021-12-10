package org.jetbrains.research.kotlinrminer.decomposition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCodeFragment implements LocationInfoProvider {
    private int depth;
    private int index;
    private String codeFragmentAfterReplacingParametersWithArguments;

    public String getArgumentizedString() {
        return codeFragmentAfterReplacingParametersWithArguments != null ?
            codeFragmentAfterReplacingParametersWithArguments : getString();
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public abstract CompositeStatementObject getParent();

    public abstract String getString();

    public abstract List<String> getVariables();

    public abstract List<String> getTypes();

    public abstract List<VariableDeclaration> getVariableDeclarations();

    public abstract Map<String, List<OperationInvocation>> getMethodInvocationMap();

    public abstract List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations();

    public abstract List<String> getStringLiterals();

    public abstract List<String> getNumberLiterals();

    public abstract List<String> getNullLiterals();

    public abstract List<String> getBooleanLiterals();

    public abstract List<String> getTypeLiterals();

    public abstract Map<String, List<ObjectCreation>> getCreationMap();

    public abstract List<String> getArrayAccesses();

    public abstract List<String> getPrefixExpressions();

    public abstract List<String> getPostfixExpressions();

    public abstract List<String> getInfixOperators();

    public abstract List<String> getInfixExpressions();

    public abstract List<String> getArguments();

    public abstract List<LambdaExpressionObject> getLambdas();

    public abstract VariableDeclaration searchVariableDeclaration(String variableName);

    public abstract VariableDeclaration getVariableDeclaration(String variableName);

    public void replaceParametersWithArguments(Map<String, String> parameterToArgumentMap) {
        String afterReplacements = getString();
        for (String parameter : parameterToArgumentMap.keySet()) {
            String argument = parameterToArgumentMap.get(parameter);
            if (!parameter.equals(argument)) {
                StringBuffer sb = new StringBuffer();
                Pattern p = Pattern.compile(Pattern.quote(parameter));
                Matcher m = p.matcher(afterReplacements);
                while (m.find()) {
                    //check if the matched string is an argument
                    //previous character should be "(" or "," or " " or there is no previous character
                    int start = m.start();
                    boolean isArgument = false;
                    boolean isInsideStringLiteral = false;
                    if (start >= 1) {
                        String previousChar = afterReplacements.substring(start - 1, start);
                        if (previousChar.equals("(") || previousChar.equals(",") || previousChar.equals(
                            " ") || previousChar.equals("=")) {
                            isArgument = true;
                        }
                        String beforeMatch = afterReplacements.substring(0, start);
                        String afterMatch =
                            afterReplacements.substring(start + parameter.length());

                        if (quoteBefore(beforeMatch) && quoteAfter(afterMatch)) {
                            isInsideStringLiteral = true;
                        }
                    } else if (start == 0 && !afterReplacements.startsWith("return ")) {
                        isArgument = true;
                    }
                    if (isArgument && !isInsideStringLiteral) {
                        m.appendReplacement(sb, Matcher.quoteReplacement(argument));
                    }
                }
                m.appendTail(sb);
                afterReplacements = sb.toString();
            }
        }
        this.codeFragmentAfterReplacingParametersWithArguments = afterReplacements;
    }


    public ObjectCreation creationCoveringEntireFragment() {
        Map<String, List<ObjectCreation>> creationMap = getCreationMap();
        String statement = getString();
        for (String objectCreation : creationMap.keySet()) {
            List<ObjectCreation> creations = creationMap.get(objectCreation);
            for (ObjectCreation creation : creations) {
                if ((objectCreation + "\n").equals(statement) || objectCreation.equals(statement)) {
                    creation.coverage = AbstractCall.StatementCoverageType.ONLY_CALL;
                    return creation;
                } else if (("return " + objectCreation + "\n").equals(statement)) {
                    creation.coverage = AbstractCall.StatementCoverageType.RETURN_CALL;
                    return creation;
                } else if (("throw " + objectCreation + "\n").equals(statement)) {
                    creation.coverage = AbstractCall.StatementCoverageType.THROW_CALL;
                    return creation;
                } else if (expressionIsTheInitializerOfVariableDeclaration(objectCreation)) {
                    creation.coverage = AbstractCall.StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL;
                    return creation;
                }
            }
        }
        return null;
    }

    public OperationInvocation assignmentInvocationCoveringEntireStatement() {
        Map<String, List<OperationInvocation>> methodInvocationMap = getMethodInvocationMap();
        for (String methodInvocation : methodInvocationMap.keySet()) {
            List<OperationInvocation> invocations = methodInvocationMap.get(methodInvocation);
            for (OperationInvocation invocation : invocations) {
                if (expressionIsTheRightHandSideOfAssignment(methodInvocation)) {
                    return invocation;
                }
            }
        }
        return null;
    }

    private static boolean quoteBefore(String beforeMatch) {
        if (beforeMatch.contains("\"")) {
            if (beforeMatch.contains("+")) {
                int indexOfQuote = beforeMatch.lastIndexOf("\"");
                int indexOfPlus = beforeMatch.lastIndexOf("+");
                return indexOfPlus <= indexOfQuote;
            } else {
                return true;
            }
        }
        return false;
    }

    private static boolean quoteAfter(String afterMatch) {
        if (afterMatch.contains("\"")) {
            if (afterMatch.contains("+")) {
                int indexOfQuote = afterMatch.indexOf("\"");
                int indexOfPlus = afterMatch.indexOf("+");
                return indexOfPlus >= indexOfQuote;
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean equalFragment(AbstractCodeFragment other) {
        if (this.getString().equals(other.getString())) {
            return true;
        } else if (this.getString().contains(other.getString())) {
            return true;
        } else if (other.getString().contains(this.getString())) {
            return true;
        } else if (this.codeFragmentAfterReplacingParametersWithArguments != null) {
            return this.codeFragmentAfterReplacingParametersWithArguments.equals(other.getString());
        } else if (other.codeFragmentAfterReplacingParametersWithArguments != null) {
            return other.codeFragmentAfterReplacingParametersWithArguments.equals(this.getString());
        }
        return false;
    }

    public void resetArgumentization() {
        this.codeFragmentAfterReplacingParametersWithArguments = getString();
    }

    private boolean isCastExpressionCoveringEntireFragment(String expression) {
        String statement = getString();
        int index = statement.indexOf(expression);
        if (index != -1) {
            String prefix = statement.substring(0, index);
            if (prefix.contains("(") && prefix.contains(")")) {
                String casting = prefix.substring(prefix.indexOf("("), prefix.indexOf(")") + 1);
                return ("return " + casting + expression).equals(statement);
            }
        }
        return false;
    }

    protected boolean containsInitializerOfVariableDeclaration(Set<String> expressions) {
        List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
        if (variableDeclarations.size() == 1 && variableDeclarations.get(0).getInitializer() != null) {
            String initializer = variableDeclarations.get(0).getInitializer().toString();
            return expressions.contains(initializer);
        }
        return false;
    }

    private boolean expressionIsTheInitializerOfVariableDeclaration(String expression) {
        List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
        if (variableDeclarations.size() == 1 && variableDeclarations.get(0).getInitializer() != null) {
            String initializer = variableDeclarations.get(0).getInitializer().toString();
            if (initializer.equals(expression))
                return true;
            if (initializer.startsWith("(")) {
                //ignore casting
                String initializerWithoutCasting =
                    initializer.substring(initializer.indexOf(")") + 1);

                return initializerWithoutCasting.equals(expression);
            }
        }
        return false;
    }

    private boolean expressionIsTheRightHandSideOfAssignment(String expression) {
        String statement = getString();
        if (statement.contains("=")) {
            List<String> variables = getVariables();
            if (variables.size() > 0) {
                String s = variables.get(0) + "=" + expression + "\n";
                return statement.equals(s);
            }
        }
        return false;
    }

    public boolean throwsNewException() {
        return getString().startsWith("throw ");
    }

    public boolean countableStatement() {
        String statement = getString();
        //covers the cases of lambda expressions having an expression as their body
        if (this instanceof AbstractExpression) {
            return true;
        }
        //covers the cases of methods with only one statement in their body
        if (this instanceof AbstractStatement && this.getParent() != null &&
            this.getParent().statementCount() == 1 && this.getParent().getParent() == null) {
            return true;
        }
        return !statement.equals("{") && !statement.startsWith("catch(") && !statement.startsWith(
            "case ") && !statement.startsWith("else ") &&
            !statement.startsWith("return True") && !statement.startsWith(
            "return False") && !statement.startsWith("return this") && !statement.startsWith(
            "return null") && !statement.startsWith("return");
    }

    public OperationInvocation invocationCoveringEntireFragment() {
        Map<String, List<OperationInvocation>> methodInvocationMap = getMethodInvocationMap();
        String statement = getString();
        if (methodInvocationMap != null) {
            for (String methodInvocation : methodInvocationMap.keySet()) {
                List<OperationInvocation> invocations = methodInvocationMap.get(methodInvocation);
                for (OperationInvocation invocation : invocations) {
                    if ((methodInvocation + "\n").equals(statement) || methodInvocation.equals(statement)) {
                        invocation.coverage = AbstractCall.StatementCoverageType.ONLY_CALL;
                        return invocation;
                    } else if (("return " + methodInvocation + "\n").equals(statement)) {
                        invocation.coverage = AbstractCall.StatementCoverageType.RETURN_CALL;
                        return invocation;
                    } else if (isCastExpressionCoveringEntireFragment(methodInvocation)) {
                        invocation.coverage = AbstractCall.StatementCoverageType.CAST_CALL;
                        return invocation;
                    } else if (expressionIsTheInitializerOfVariableDeclaration(methodInvocation)) {
                        invocation.coverage = AbstractCall.StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL;
                        return invocation;
                    } else if (invocation.getLocationInfo().getCodeElementType().equals(
                        CodeElementType.SUPER_CONSTRUCTOR_INVOCATION) ||
                        invocation.getLocationInfo().getCodeElementType().equals(
                            CodeElementType.CONSTRUCTOR_INVOCATION)) {
                        invocation.coverage = AbstractCall.StatementCoverageType.ONLY_CALL;
                        return invocation;
                    }
                }
            }
        }
        return null;
    }
}
