package org.jetbrains.research.kotlinrminer.core.diff;

import org.jetbrains.research.kotlinrminer.core.RefactoringMinerTimedOutException;
import org.jetbrains.research.kotlinrminer.core.decomposition.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.core.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.core.uml.UMLParameter;
import org.jetbrains.research.kotlinrminer.core.diff.refactoring.ExtractOperationRefactoring;
import org.jetbrains.research.kotlinrminer.core.uml.UMLType;
import org.jetbrains.research.kotlinrminer.core.decomposition.*;

import java.util.*;

public class ExtractOperationDetection {
    private final UMLOperationBodyMapper mapper;
    private final List<UMLOperation> addedOperations;
    private final UMLClassBaseDiff classDiff;
    private final UMLModelDiff modelDiff;
    private final List<OperationInvocation> operationInvocations;
    private final Map<CallTreeNode, CallTree> callTreeMap = new LinkedHashMap<>();

    public ExtractOperationDetection(UMLOperationBodyMapper mapper,
                                     List<UMLOperation> addedOperations,
                                     UMLClassBaseDiff classDiff,
                                     UMLModelDiff modelDiff) {
        this.mapper = mapper;
        this.addedOperations = addedOperations;
        this.classDiff = classDiff;
        this.modelDiff = modelDiff;
        this.operationInvocations = getInvocationsInSourceOperationAfterExtraction(mapper);
    }

    public List<ExtractOperationRefactoring> check(UMLOperation addedOperation) throws
        RefactoringMinerTimedOutException {
        List<ExtractOperationRefactoring> refactorings = new ArrayList<>();
        if (!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
            !mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
            List<OperationInvocation> addedOperationInvocations =
                matchingInvocations(addedOperation, operationInvocations, mapper.getOperation2().variableTypeMap());
            if (addedOperationInvocations.size() > 0) {
                int otherAddedMethodsCalled = 0;
                for (UMLOperation addedOperation2 : this.addedOperations) {
                    if (!addedOperation.equals(addedOperation2)) {
                        List<OperationInvocation> addedOperationInvocations2 =
                            matchingInvocations(addedOperation2, operationInvocations,
                                mapper.getOperation2().variableTypeMap());
                        if (addedOperationInvocations2.size() > 0) {
                            otherAddedMethodsCalled++;
                        }
                    }
                }
                if (otherAddedMethodsCalled == 0) {
                    for (OperationInvocation addedOperationInvocation : addedOperationInvocations) {
                        processAddedOperation(mapper, addedOperation, refactorings, addedOperationInvocations,
                            addedOperationInvocation);
                    }
                } else {
                    processAddedOperation(mapper, addedOperation, refactorings, addedOperationInvocations,
                        addedOperationInvocations.get(0));
                }
            }
        }
        return refactorings;
    }

    private void processAddedOperation(UMLOperationBodyMapper mapper,
                                       UMLOperation addedOperation,
                                       List<ExtractOperationRefactoring> refactorings,
                                       List<OperationInvocation> addedOperationInvocations,
                                       OperationInvocation addedOperationInvocation)
        throws RefactoringMinerTimedOutException {
        CallTreeNode root = new CallTreeNode(mapper.getOperation1(), addedOperation, addedOperationInvocation);
        CallTree callTree;
        if (callTreeMap.containsKey(root)) {
            callTree = callTreeMap.get(root);
        } else {
            callTree = new CallTree(root);
            generateCallTree(addedOperation, root, callTree);
            callTreeMap.put(root, callTree);
        }
        UMLOperationBodyMapper operationBodyMapper =
            createMapperForExtractedMethod(mapper, mapper.getOperation1(), addedOperation, addedOperationInvocation);
        if (operationBodyMapper != null) {
            List<AbstractCodeMapping> additionalExactMatches = new ArrayList<>();
            List<CallTreeNode> nodesInBreadthFirstOrder = callTree.getNodesInBreadthFirstOrder();
            for (int i = 1; i < nodesInBreadthFirstOrder.size(); i++) {
                CallTreeNode node = nodesInBreadthFirstOrder.get(i);
                if (matchingInvocations(node.getInvokedOperation(), operationInvocations,
                    mapper.getOperation2().variableTypeMap()).size() == 0) {
                    UMLOperationBodyMapper nestedMapper =
                        createMapperForExtractedMethod(mapper, node.getOriginalOperation(), node.getInvokedOperation(),
                            node.getInvocation());
                    if (nestedMapper != null) {
                        additionalExactMatches.addAll(nestedMapper.getExactMatches());
                        if (extractMatchCondition(nestedMapper,
                            new ArrayList<>()) && extractMatchCondition(
                            operationBodyMapper, additionalExactMatches)) {
                            List<OperationInvocation> nestedMatchingInvocations =
                                matchingInvocations(node.getInvokedOperation(),
                                    node.getOriginalOperation().getAllOperationInvocations(),
                                    node.getOriginalOperation().variableTypeMap());
                            ExtractOperationRefactoring nestedRefactoring =
                                new ExtractOperationRefactoring(nestedMapper, mapper.getOperation2(),
                                    nestedMatchingInvocations);
                            refactorings.add(nestedRefactoring);
                            operationBodyMapper.addChildMapper(nestedMapper);
                        }
                        //add back to mapper non-exact matches
                        for (AbstractCodeMapping mapping : nestedMapper.getMappings()) {
                            if (!mapping.isExact() || mapping.getFragment1().getString().equals("{")) {
                                AbstractCodeFragment fragment1 = mapping.getFragment1();
                                if (fragment1 instanceof StatementObject) {
                                    if (!mapper.getNonMappedLeavesT1().contains(fragment1)) {
                                        mapper.getNonMappedLeavesT1().add((StatementObject) fragment1);
                                    }
                                } else if (fragment1 instanceof CompositeStatementObject) {
                                    if (!mapper.getNonMappedInnerNodesT1().contains(fragment1)) {
                                        mapper.getNonMappedInnerNodesT1().add((CompositeStatementObject) fragment1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            UMLOperation delegateMethod =
                findDelegateMethod(mapper.getOperation1(), addedOperation, addedOperationInvocation);
            if (extractMatchCondition(operationBodyMapper, additionalExactMatches)) {
                if (delegateMethod == null) {
                    refactorings.add(new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(),
                        addedOperationInvocations));
                } else {
                    refactorings.add(new ExtractOperationRefactoring(operationBodyMapper, addedOperation,
                        mapper.getOperation1(), mapper.getOperation2(),
                        addedOperationInvocations));
                }
            }
        }
    }

    public static List<OperationInvocation> getInvocationsInSourceOperationAfterExtraction(UMLOperationBodyMapper mapper) {
        List<OperationInvocation> operationInvocations = mapper.getOperation2().getAllOperationInvocations();
        for (StatementObject statement : mapper.getNonMappedLeavesT2()) {
            addStatementInvocations(operationInvocations, statement);
        }
        return operationInvocations;
    }

    public static void addStatementInvocations(List<OperationInvocation> operationInvocations,
                                               StatementObject statement) {
        Map<String, List<OperationInvocation>> statementMethodInvocationMap = statement.getMethodInvocationMap();
        for (String key : statementMethodInvocationMap.keySet()) {
            for (OperationInvocation statementInvocation : statementMethodInvocationMap.get(key)) {
                if (!containsInvocation(operationInvocations, statementInvocation)) {
                    operationInvocations.add(statementInvocation);
                }
            }
        }
        List<LambdaExpressionObject> lambdas = statement.getLambdas();
        for (LambdaExpressionObject lambda : lambdas) {
            if (lambda.getBody() != null) {
                for (OperationInvocation statementInvocation : lambda.getBody().getAllOperationInvocations()) {
                    if (!containsInvocation(operationInvocations, statementInvocation)) {
                        operationInvocations.add(statementInvocation);
                    }
                }
            }
            if (lambda.getExpression() != null) {
                Map<String, List<OperationInvocation>> methodInvocationMap =
                    lambda.getExpression().getMethodInvocationMap();
                for (String key : methodInvocationMap.keySet()) {
                    for (OperationInvocation statementInvocation : methodInvocationMap.get(key)) {
                        if (!containsInvocation(operationInvocations, statementInvocation)) {
                            operationInvocations.add(statementInvocation);
                        }
                    }
                }
            }
        }
    }

    public static boolean containsInvocation(List<OperationInvocation> operationInvocations,
                                             OperationInvocation invocation) {
        for (OperationInvocation operationInvocation : operationInvocations) {
            if (operationInvocation.getLocationInfo().equals(invocation.getLocationInfo())) {
                return true;
            }
        }
        return false;
    }

    private List<OperationInvocation> matchingInvocations(UMLOperation operation,
                                                          List<OperationInvocation> operationInvocations,
                                                          Map<String, UMLType> variableTypeMap) {
        List<OperationInvocation> addedOperationInvocations = new ArrayList<>();
        for (OperationInvocation invocation : operationInvocations) {
            if (invocation.matchesOperation(operation, variableTypeMap, modelDiff)) {
                addedOperationInvocations.add(invocation);
            }
        }
        return addedOperationInvocations;
    }

    private void generateCallTree(UMLOperation operation, CallTreeNode parent, CallTree callTree) {
        List<OperationInvocation> invocations = operation.getAllOperationInvocations();
        for (UMLOperation addedOperation : addedOperations) {
            for (OperationInvocation invocation : invocations) {
                if (invocation.matchesOperation(addedOperation, operation.variableTypeMap(), modelDiff)) {
                    if (!callTree.contains(addedOperation)) {
                        CallTreeNode node = new CallTreeNode(operation, addedOperation, invocation);
                        parent.addChild(node);
                        generateCallTree(addedOperation, node, callTree);
                    }
                }
            }
        }
    }

    private UMLOperationBodyMapper createMapperForExtractedMethod(UMLOperationBodyMapper mapper,
                                                                  UMLOperation originalOperation,
                                                                  UMLOperation addedOperation,
                                                                  OperationInvocation addedOperationInvocation) throws
        RefactoringMinerTimedOutException {
        List<UMLParameter> originalMethodParameters = originalOperation.getParametersWithoutReturnType();
        Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters =
            new LinkedHashMap<>();
        List<String> arguments = addedOperationInvocation.getArguments();
        List<UMLParameter> parameters = addedOperation.getParametersWithoutReturnType();
        Map<String, String> parameterToArgumentMap = new LinkedHashMap<>();
        //special handling for methods with varargs parameter for which no argument is passed in the matching invocation
        int size = Math.min(arguments.size(), parameters.size());
        for (int i = 0; i < size; i++) {
            String argumentName = arguments.get(i);
            String parameterName = parameters.get(i).getName();
            parameterToArgumentMap.put(parameterName, argumentName);
            for (UMLParameter originalMethodParameter : originalMethodParameters) {
                if (originalMethodParameter.getName().equals(argumentName)) {
                    originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.put(originalMethodParameter,
                        parameters.get(i));
                }
            }
        }
        if (parameterTypesMatch(originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters)) {
            UMLOperation delegateMethod =
                findDelegateMethod(originalOperation, addedOperation, addedOperationInvocation);
            return new UMLOperationBodyMapper(mapper,
                delegateMethod != null ? delegateMethod : addedOperation,
                new LinkedHashMap<>(), parameterToArgumentMap, classDiff);
        }
        return null;
    }

    private boolean extractMatchCondition(UMLOperationBodyMapper operationBodyMapper,
                                          List<AbstractCodeMapping> additionalExactMatches) {
        int mappings = operationBodyMapper.mappingsWithoutBlocks();
        int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
        int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
        List<AbstractCodeMapping> exactMatchList =
            new ArrayList<>(operationBodyMapper.getExactMatches());
        boolean exceptionHandlingExactMatch = false;
        boolean throwsNewExceptionExactMatch = false;
        if (exactMatchList.size() == 1) {
            AbstractCodeMapping mapping = exactMatchList.get(0);
            if (mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
                StatementObject statement1 = (StatementObject) mapping.getFragment1();
                StatementObject statement2 = (StatementObject) mapping.getFragment2();
                if (statement1.getParent().getString().startsWith("catch(") &&
                    statement2.getParent().getString().startsWith("catch(")) {
                    exceptionHandlingExactMatch = true;
                }
            }
            if (mapping.getFragment1().throwsNewException() && mapping.getFragment2().throwsNewException()) {
                throwsNewExceptionExactMatch = true;
            }
        }
        exactMatchList.addAll(additionalExactMatches);
        int exactMatches = exactMatchList.size();
        return mappings > 0 && (mappings > nonMappedElementsT2 || (mappings > 1 && mappings >= nonMappedElementsT2) ||
            (exactMatches >= mappings && nonMappedElementsT1 == 0) ||
            (exactMatches == 1 && !throwsNewExceptionExactMatch && nonMappedElementsT2 - exactMatches <= 10) ||
            (!exceptionHandlingExactMatch && exactMatches > 1 && additionalExactMatches.size() < exactMatches && nonMappedElementsT2 - exactMatches < 20) ||
            (mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2())) ||
            argumentExtractedWithDefaultReturnAdded(operationBodyMapper);
    }

    private boolean argumentExtractedWithDefaultReturnAdded(UMLOperationBodyMapper operationBodyMapper) {
        List<AbstractCodeMapping> totalMappings = new ArrayList<>(operationBodyMapper.getMappings());
        List<CompositeStatementObject> nonMappedInnerNodesT2 =
            new ArrayList<>(operationBodyMapper.getNonMappedInnerNodesT2());
        nonMappedInnerNodesT2.removeIf(compositeStatementObject -> compositeStatementObject.toString().equals("{"));
        List<StatementObject> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
        return totalMappings.size() == 1 && totalMappings.get(0).containsReplacement(
            Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) &&
            nonMappedInnerNodesT2.size() == 1 && nonMappedInnerNodesT2.get(0).toString().startsWith("if") &&
            nonMappedLeavesT2.size() == 1 && nonMappedLeavesT2.get(0).toString().startsWith("return ");
    }

    private UMLOperation findDelegateMethod(UMLOperation originalOperation,
                                            UMLOperation addedOperation,
                                            OperationInvocation addedOperationInvocation) {
        OperationInvocation delegateMethodInvocation = addedOperation.isDelegate();
        if (originalOperation.isDelegate() == null && delegateMethodInvocation != null && !originalOperation.getAllOperationInvocations().contains(
            addedOperationInvocation)) {
            for (UMLOperation operation : addedOperations) {
                if (delegateMethodInvocation.matchesOperation(operation, addedOperation.variableTypeMap(), modelDiff)) {
                    return operation;
                }
            }
        }
        return null;
    }

    private boolean parameterTypesMatch(Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters) {
        for (UMLParameter key : originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.keySet()) {
            UMLParameter value = originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.get(key);
            if (!key.getType().equals(value.getType()) && !key.getType().equalsWithSubType(value.getType()) &&
                !modelDiff.isSubclassOf(key.getType().getClassType(), value.getType().getClassType())) {
                return false;
            }
        }
        return true;
    }
}
