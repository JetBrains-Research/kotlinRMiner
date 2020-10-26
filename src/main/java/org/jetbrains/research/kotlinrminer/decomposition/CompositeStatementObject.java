package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

import java.util.*;

public class CompositeStatementObject extends AbstractStatement {
    private final List<AbstractStatement> statementList;
    private final List<AbstractExpression> expressionList;
    private final List<VariableDeclaration> variableDeclarations;

    private final LocationInfo locationInfo;

    public CompositeStatementObject(KtFile cu,
                                    String filePath,
                                    KtElement statement,
                                    int depth,
                                    CodeElementType codeElementType) {
        super();
        this.setDepth(depth);
        this.locationInfo = new LocationInfo(cu, filePath, statement, codeElementType);
        this.statementList = new ArrayList<>();
        this.expressionList = new ArrayList<>();
        this.variableDeclarations = new ArrayList<>();
    }

    public void addStatement(AbstractStatement statement) {
        statement.setIndex(statementList.size());
        statementList.add(statement);
        statement.setParent(this);
    }

    public List<AbstractStatement> getStatements() {
        return statementList;
    }

    public void addExpression(AbstractExpression expression) {
        //an expression has the same index and depth as the composite statement it belong to
        expression.setDepth(this.getDepth());
        expression.setIndex(this.getIndex());
        expressionList.add(expression);
        expression.setOwner(this);
    }

    public List<AbstractExpression> getExpressions() {
        return expressionList;
    }

    public void addVariableDeclaration(VariableDeclaration declaration) {
        this.variableDeclarations.add(declaration);
    }

    @Override
    public List<StatementObject> getLeaves() {
        List<StatementObject> leaves = new ArrayList<>();
        for (AbstractStatement statement : statementList) {
            leaves.addAll(statement.getLeaves());
        }
        return leaves;
    }

    public List<CompositeStatementObject> getInnerNodes() {
        List<CompositeStatementObject> innerNodes = new ArrayList<>();
        for (AbstractStatement statement : statementList) {
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) statement;
                innerNodes.addAll(composite.getInnerNodes());
            }
        }
        innerNodes.add(this);
        return innerNodes;
    }

    public boolean contains(AbstractCodeFragment fragment) {
        if (fragment instanceof StatementObject) {
            return getLeaves().contains(fragment);
        } else if (fragment instanceof CompositeStatementObject) {
            return getInnerNodes().contains(fragment);
        } else if (fragment instanceof AbstractExpression) {
            return getExpressions().contains(fragment);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(locationInfo.getCodeElementType().getName());
        if (expressionList.size() > 0) {
            sb.append("(");
            for (int i = 0; i < expressionList.size() - 1; i++) {
                sb.append(expressionList.get(i).toString()).append("; ");
            }
            sb.append(expressionList.get(expressionList.size() - 1).toString());
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public List<String> getVariables() {
        List<String> variables = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            variables.addAll(expression.getVariables());
        }
        return variables;
    }

    @Override
    public List<String> getTypes() {
        List<String> types = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            types.addAll(expression.getTypes());
        }
        return types;
    }

    @Override
    public List<VariableDeclaration> getVariableDeclarations() {
        //special handling for enhanced-for formal parameter
        List<VariableDeclaration> variableDeclarations = new ArrayList<>(this.variableDeclarations);
        for (AbstractExpression expression : expressionList) {
            variableDeclarations.addAll(expression.getVariableDeclarations());
        }
        return variableDeclarations;
    }

    public List<String> getAllVariables() {
        List<String> variables = new ArrayList<>(getVariables());
        for (AbstractStatement statement : statementList) {
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) statement;
                variables.addAll(composite.getAllVariables());
            } else if (statement instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) statement;
                variables.addAll(statementObject.getVariables());
            }
        }
        return variables;
    }


    public List<LambdaExpressionObject> getAllLambdas() {
        List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
        lambdas.addAll(getLambdas());
        for (AbstractStatement statement : statementList) {
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) statement;
                lambdas.addAll(composite.getAllLambdas());
            } else if (statement instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) statement;
                lambdas.addAll(statementObject.getLambdas());
            }
        }
        return lambdas;
    }

    public List<VariableDeclaration> getAllVariableDeclarations() {
        List<VariableDeclaration> variableDeclarations = new ArrayList<>(getVariableDeclarations());
        for (AbstractStatement statement : statementList) {
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) statement;
                variableDeclarations.addAll(composite.getAllVariableDeclarations());
            } else if (statement instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) statement;
                variableDeclarations.addAll(statementObject.getVariableDeclarations());
                for (LambdaExpressionObject lambda : statementObject.getLambdas()) {
                    if (lambda.getBody() != null) {
                        variableDeclarations.addAll(lambda.getBody().getAllVariableDeclarations());
                    }
                }
            }
        }
        return variableDeclarations;
    }

    public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
        List<VariableDeclaration> variableDeclarations = new ArrayList<>();
        for (VariableDeclaration variableDeclaration : getAllVariableDeclarations()) {
            if (variableDeclaration.getScope().subsumes(location)) {
                variableDeclarations.add(variableDeclaration);
            }
        }
        return variableDeclarations;
    }

    @Override
    public int statementCount() {
        int count = 0;
        if (!this.getString().equals("{"))
            count++;
        for (AbstractStatement statement : statementList) {
            count += statement.statementCount();
        }
        return count;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public VariableDeclaration getVariableDeclaration(String variableName) {
        List<VariableDeclaration> variableDeclarations = getAllVariableDeclarations();
        for (VariableDeclaration declaration : variableDeclarations) {
            if (declaration.getVariableName().equals(variableName)) {
                return declaration;
            }
        }
        return null;
    }

    public Map<String, Set<String>> aliasedAttributes() {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        for (StatementObject statement : getLeaves()) {
            String s = statement.getString();
            if (s.startsWith("this.") && s.endsWith(";\n")) {
                String firstLine = s.substring(0, s.indexOf("\n"));
                if (firstLine.contains("=")) {
                    String attribute = s.substring(5, s.indexOf("="));
                    String value = s.substring(s.indexOf("=") + 1, s.indexOf(";\n"));
                    if (map.containsKey(value)) {
                        map.get(value).add(attribute);
                    } else {
                        Set<String> set = new LinkedHashSet<>();
                        set.add(attribute);
                        map.put(value, set);
                    }
                }
            }
        }
        Set<String> keysToBeRemoved = new LinkedHashSet<>();
        for (String key : map.keySet()) {
            if (map.get(key).size() <= 1) {
                keysToBeRemoved.add(key);
            }
        }
        for (String key : keysToBeRemoved) {
            map.remove(key);
        }
        return map;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    public boolean isLoop() {
        return this.locationInfo.getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
            this.locationInfo.getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
            this.locationInfo.getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
            this.locationInfo.getCodeElementType().equals(CodeElementType.DO_STATEMENT);
    }

    public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
        for (CompositeStatementObject innerNode : getInnerNodes()) {
            if (innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
                boolean currentElementNameMatched = false;
                for (VariableDeclaration declaration : innerNode.getVariableDeclarations()) {
                    if (declaration.getVariableName().equals(currentElementName)) {
                        currentElementNameMatched = true;
                        break;
                    }
                }
                boolean collectionNameMatched = false;
                for (AbstractExpression expression : innerNode.getExpressions()) {
                    if (expression.getVariables().contains(collectionName)) {
                        collectionNameMatched = true;
                        break;
                    }
                }
                if (currentElementNameMatched && collectionNameMatched) {
                    return innerNode;
                }
            } else if (innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
                innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT)) {
                boolean collectionNameMatched = false;
                for (AbstractExpression expression : innerNode.getExpressions()) {
                    if (expression.getVariables().contains(collectionName)) {
                        collectionNameMatched = true;
                        break;
                    }
                }
                boolean currentElementNameMatched = false;
                for (StatementObject statement : innerNode.getLeaves()) {
                    VariableDeclaration variableDeclaration = statement.getVariableDeclaration(currentElementName);
                    if (variableDeclaration != null && statement.getVariables().contains(collectionName)) {
                        currentElementNameMatched = true;
                        break;
                    }
                }
                if (currentElementNameMatched && collectionNameMatched) {
                    return innerNode;
                }
            }
        }
        return null;
    }


    public Map<String, List<OperationInvocation>> getAllMethodInvocations() {
        Map<String, List<OperationInvocation>> map = new LinkedHashMap<>(getMethodInvocationMap());
        for (AbstractStatement statement : statementList) {
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) statement;
                Map<String, List<OperationInvocation>> compositeMap = composite.getAllMethodInvocations();
                for (String key : compositeMap.keySet()) {
                    if (map.containsKey(key)) {
                        map.get(key).addAll(compositeMap.get(key));
                    } else {
                        List<OperationInvocation> list = new ArrayList<>(compositeMap.get(key));
                        map.put(key, list);
                    }
                }
            } else if (statement instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) statement;
                Map<String, List<OperationInvocation>> statementMap = statementObject.getMethodInvocationMap();
                for (String key : statementMap.keySet()) {
                    if (map.containsKey(key)) {
                        map.get(key).addAll(statementMap.get(key));
                    } else {
                        List<OperationInvocation> list = new ArrayList<>(statementMap.get(key));
                        map.put(key, list);
                    }
                }
                for (LambdaExpressionObject lambda : statementObject.getLambdas()) {
                    if (lambda.getBody() != null) {
                        Map<String, List<OperationInvocation>> lambdaMap =
                            lambda.getBody().getCompositeStatement().getAllMethodInvocations();
                        for (String key : lambdaMap.keySet()) {
                            if (map.containsKey(key)) {
                                map.get(key).addAll(lambdaMap.get(key));
                            } else {
                                List<OperationInvocation> list = new ArrayList<>(lambdaMap.get(key));
                                map.put(key, list);
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    @Override
    public List<String> stringRepresentation() {
        List<String> stringRepresentation = new ArrayList<>();
        stringRepresentation.add(this.toString());
        for (AbstractStatement statement : statementList) {
            stringRepresentation.addAll(statement.stringRepresentation());
        }
        return stringRepresentation;
    }

    @Override
    public Map<String, List<OperationInvocation>> getMethodInvocationMap() {
        Map<String, List<OperationInvocation>> map = new LinkedHashMap<>();
        for (AbstractExpression expression : expressionList) {
            Map<String, List<OperationInvocation>> expressionMap = expression.getMethodInvocationMap();
            for (String key : expressionMap.keySet()) {
                if (map.containsKey(key)) {
                    map.get(key).addAll(expressionMap.get(key));
                } else {
                    List<OperationInvocation> list = new ArrayList<>(expressionMap.get(key));
                    map.put(key, list);
                }
            }
        }
        return map;
    }

    @Override
    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        //TODO: implement it
        return null;
    }

    @Override
    public List<String> getStringLiterals() {
        List<String> stringLiterals = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            stringLiterals.addAll(expression.getStringLiterals());
        }
        return stringLiterals;
    }

    @Override
    public List<String> getNumberLiterals() {
        List<String> numberLiterals = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            numberLiterals.addAll(expression.getNumberLiterals());
        }
        return numberLiterals;
    }

    @Override
    public List<String> getNullLiterals() {
        List<String> nullLiterals = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            nullLiterals.addAll(expression.getNullLiterals());
        }
        return nullLiterals;
    }

    @Override
    public List<String> getBooleanLiterals() {
        List<String> booleanLiterals = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            booleanLiterals.addAll(expression.getBooleanLiterals());
        }
        return booleanLiterals;
    }

    @Override
    public List<String> getTypeLiterals() {
        List<String> typeLiterals = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            typeLiterals.addAll(expression.getTypeLiterals());
        }
        return typeLiterals;
    }

    @Override
    public Map<String, List<ObjectCreation>> getCreationMap() {
        return new HashMap<>();
    }

    @Override
    public List<String> getArrayAccesses() {
        List<String> arrayAccesses = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            arrayAccesses.addAll(expression.getArrayAccesses());
        }
        return arrayAccesses;
    }

    @Override
    public List<String> getPrefixExpressions() {
        List<String> prefixExpressions = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            prefixExpressions.addAll(expression.getPrefixExpressions());
        }
        return prefixExpressions;
    }

    @Override
    public List<String> getPostfixExpressions() {
        List<String> postfixExpressions = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            postfixExpressions.addAll(expression.getPostfixExpressions());
        }
        return postfixExpressions;
    }

    @Override
    public List<String> getInfixOperators() {
        List<String> infixOperators = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            infixOperators.addAll(expression.getInfixOperators());
        }
        return infixOperators;
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            arguments.addAll(expression.getArguments());
        }
        return arguments;
    }

    @Override
    public List<LambdaExpressionObject> getLambdas() {
        List<LambdaExpressionObject> lambdas = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            lambdas.addAll(expression.getLambdas());
        }
        return lambdas;
    }
}
