package org.jetbrains.research.kotlinrminer.ide.diff;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.kotlinrminer.common.replacement.ConsistentReplacementDetector;
import org.jetbrains.research.kotlinrminer.common.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.ide.Refactoring;
import org.jetbrains.research.kotlinrminer.ide.RefactoringMinerTimedOutException;
import org.jetbrains.research.kotlinrminer.ide.decomposition.*;
import org.jetbrains.research.kotlinrminer.ide.decomposition.replacement.MethodInvocationReplacement;
import org.jetbrains.research.kotlinrminer.ide.diff.refactoring.RenameOperationRefactoring;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLFile;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLOperation;

import java.util.*;

public class UMLFileDiff implements Comparable<UMLFileDiff> {
    protected UMLFile originalFile;
    protected UMLFile nextFile;
    protected List<UMLOperation> addedOperations;
    protected List<UMLOperation> removedOperations;
    private final List<UMLOperationBodyMapper> operationBodyMapperList;
    private final List<UMLOperationDiff> operationDiffList;
    protected List<Refactoring> refactorings;
    private final UMLModelDiff modelDiff;
    private Set<MethodInvocationReplacement> consistentMethodInvocationRenames;

    public UMLFileDiff(UMLFile originalFile, UMLFile nextFile, UMLModelDiff modelDiff) {
        this.originalFile = originalFile;
        this.nextFile = nextFile;
        this.addedOperations = new ArrayList<>();
        this.removedOperations = new ArrayList<>();
        this.operationBodyMapperList = new ArrayList<>();
        this.operationDiffList = new ArrayList<>();
        this.refactorings = new ArrayList<>();
        this.modelDiff = modelDiff;
    }

    public void process() throws RefactoringMinerTimedOutException {
        processOperations();
        createBodyMappers();
        checkForOperationSignatureChanges();
    }

    protected void processOperations() {
        for (UMLOperation operation : originalFile.getOperations()) {
            if (!nextFile.getOperations().contains(operation)) {
                this.reportRemovedOperation(operation);
            }
        }
        for (UMLOperation operation : nextFile.getOperations()) {
            if (!originalFile.getOperations().contains(operation)) {
                this.reportAddedOperation(operation);
            }
        }
    }

    public boolean isEmpty() {
        return addedOperations.isEmpty() && removedOperations.isEmpty() &&
            operationDiffList.isEmpty() &&
            operationBodyMapperList.isEmpty();
    }

    protected void createBodyMappers() throws RefactoringMinerTimedOutException {
        for (UMLOperation originalOperation : originalFile.getOperations()) {
            for (UMLOperation nextOperation : nextFile.getOperations()) {
                if (originalOperation.equalsQualified(nextOperation)) {
                    UMLOperationBodyMapper operationBodyMapper =
                        new UMLOperationBodyMapper(this, originalOperation, nextOperation);
                    UMLOperationDiff operationSignatureDiff =
                        new UMLOperationDiff(originalOperation, nextOperation, operationBodyMapper.getMappings());
                    refactorings.addAll(operationSignatureDiff.getRefactorings());
                    this.addOperationBodyMapper(operationBodyMapper);
                }
            }
        }
        for (UMLOperation operation : originalFile.getOperations()) {
            if (!containsMapperForOperation(operation) && nextFile.getOperations().contains(
                operation) && !removedOperations.contains(operation)) {
                int index = nextFile.getOperations().indexOf(operation);
                int lastIndex = nextFile.getOperations().lastIndexOf(operation);
                int finalIndex = index;
                if (index != lastIndex && operation.getReturnParameter() != null) {
                    double d1 = operation.getReturnParameter().getType().normalizedNameDistance(
                        nextFile.getOperations().get(index).getReturnParameter().getType());
                    double d2 = operation.getReturnParameter().getType().normalizedNameDistance(
                        nextFile.getOperations().get(lastIndex).getReturnParameter().getType());
                    if (d2 < d1) {
                        finalIndex = lastIndex;
                    }
                }
                UMLOperationBodyMapper operationBodyMapper =
                    new UMLOperationBodyMapper(this, operation, nextFile.getOperations().get(finalIndex));
                UMLOperationDiff operationSignatureDiff =
                    new UMLOperationDiff(operation, nextFile.getOperations().get(finalIndex),
                        operationBodyMapper.getMappings());
                refactorings.addAll(operationSignatureDiff.getRefactorings());
                this.addOperationBodyMapper(operationBodyMapper);
            }
        }
        List<UMLOperation> removedOperationsToBeRemoved = new ArrayList<>();
        List<UMLOperation> addedOperationsToBeRemoved = new ArrayList<>();
        for (UMLOperation removedOperation : removedOperations) {
            for (UMLOperation addedOperation : addedOperations) {
                if (removedOperation.equalsIgnoringVisibility(addedOperation)) {
                    UMLOperationBodyMapper operationBodyMapper =
                        new UMLOperationBodyMapper(this, removedOperation, addedOperation);
                    UMLOperationDiff operationSignatureDiff =
                        new UMLOperationDiff(removedOperation, addedOperation, operationBodyMapper.getMappings());
                    refactorings.addAll(operationSignatureDiff.getRefactorings());
                    this.addOperationBodyMapper(operationBodyMapper);
                    removedOperationsToBeRemoved.add(removedOperation);
                    addedOperationsToBeRemoved.add(addedOperation);
                } else if (removedOperation.equalsIgnoringNameCase(addedOperation)) {
                    UMLOperationBodyMapper operationBodyMapper =
                        new UMLOperationBodyMapper(this, removedOperation, addedOperation);
                    UMLOperationDiff operationSignatureDiff =
                        new UMLOperationDiff(removedOperation, addedOperation, operationBodyMapper.getMappings());
                    refactorings.addAll(operationSignatureDiff.getRefactorings());
                    if (!removedOperation.getName().equals(addedOperation.getName()) &&
                        !(removedOperation.isConstructor() && addedOperation.isConstructor())) {
                        RenameOperationRefactoring rename = new RenameOperationRefactoring(operationBodyMapper);
                        refactorings.add(rename);
                    }
                    this.addOperationBodyMapper(operationBodyMapper);
                    removedOperationsToBeRemoved.add(removedOperation);
                    addedOperationsToBeRemoved.add(addedOperation);
                }
            }
        }
        removedOperations.removeAll(removedOperationsToBeRemoved);
        addedOperations.removeAll(addedOperationsToBeRemoved);
    }

    private Set<MethodInvocationReplacement> findConsistentMethodInvocationRenames() {
        Set<MethodInvocationReplacement> allConsistentMethodInvocationRenames =
            new LinkedHashSet<>();
        Set<MethodInvocationReplacement> allInconsistentMethodInvocationRenames =
            new LinkedHashSet<>();
        for (UMLOperationBodyMapper bodyMapper : operationBodyMapperList) {
            Set<MethodInvocationReplacement> methodInvocationRenames =
                bodyMapper.getMethodInvocationRenameReplacements();
            ConsistentReplacementDetector.updateRenames(allConsistentMethodInvocationRenames,
                allInconsistentMethodInvocationRenames,
                methodInvocationRenames);
        }
        allConsistentMethodInvocationRenames.removeAll(allInconsistentMethodInvocationRenames);
        return allConsistentMethodInvocationRenames;
    }

    private void checkForOperationSignatureChanges() throws RefactoringMinerTimedOutException {
        consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
        if (removedOperations.size() <= addedOperations.size()) {
            for (UMLOperation removedOperation : removedOperations) {
                TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<>();
                for (UMLOperation addedOperation : addedOperations) {
                    int maxDifferenceInPosition;
                    if (removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation()) {
                        maxDifferenceInPosition = Math.abs(removedOperations.size() - addedOperations.size());
                    } else {
                        maxDifferenceInPosition = Math.max(removedOperations.size(), addedOperations.size());
                    }
                    updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
                }
                if (!mapperSet.isEmpty()) {
                    UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
                    if (bestMapper != null) {
                        removedOperation = bestMapper.getOperation1();
                        UMLOperation addedOperation = bestMapper.getOperation2();
                        addedOperations.remove(addedOperation);
                        //TODO  removedOperationIterator.remove();

                        UMLOperationDiff operationSignatureDiff =
                            new UMLOperationDiff(removedOperation, addedOperation, bestMapper.getMappings());
                        operationDiffList.add(operationSignatureDiff);
                        refactorings.addAll(operationSignatureDiff.getRefactorings());
                        if (!removedOperation.getName().equals(addedOperation.getName()) &&
                            !(removedOperation.isConstructor() && addedOperation.isConstructor())) {
                            RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper);
                            refactorings.add(rename);
                        }
                        this.addOperationBodyMapper(bestMapper);
                    }
                }
            }
        } else {
            for (Iterator<UMLOperation> addedOperationIterator =
                 addedOperations.iterator(); addedOperationIterator.hasNext(); ) {
                UMLOperation addedOperation = addedOperationIterator.next();
                TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<>();
                for (UMLOperation removedOperation : removedOperations) {
                    int maxDifferenceInPosition;
                    if (removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation()) {
                        maxDifferenceInPosition = Math.abs(removedOperations.size() - addedOperations.size());
                    } else {
                        maxDifferenceInPosition = Math.max(removedOperations.size(), addedOperations.size());
                    }
                    updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
                }
                if (!mapperSet.isEmpty()) {
                    UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
                    if (bestMapper != null) {
                        UMLOperation removedOperation = bestMapper.getOperation1();
                        addedOperation = bestMapper.getOperation2();
                        removedOperations.remove(removedOperation);
                        addedOperationIterator.remove();

                        UMLOperationDiff operationSignatureDiff =
                            new UMLOperationDiff(removedOperation, addedOperation, bestMapper.getMappings());
                        operationDiffList.add(operationSignatureDiff);
                        refactorings.addAll(operationSignatureDiff.getRefactorings());
                        if (!removedOperation.getName().equals(addedOperation.getName()) &&
                            !(removedOperation.isConstructor() && addedOperation.isConstructor())) {
                            RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper);
                            refactorings.add(rename);
                        }
                        this.addOperationBodyMapper(bestMapper);
                    }
                }
            }
        }
    }

    private int computeAbsoluteDifferenceInPositionWithinClass(UMLOperation removedOperation,
                                                               UMLOperation addedOperation) {
        int index1 = originalFile.getOperations().indexOf(removedOperation);
        int index2 = nextFile.getOperations().indexOf(addedOperation);
        return Math.abs(index1 - index2);
    }


    private boolean exactMappings(UMLOperationBodyMapper operationBodyMapper) {
        if (allMappingsAreExactMatches(operationBodyMapper)) {
            if (operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0)
                return true;
            else if (operationBodyMapper.nonMappedElementsT1() > 0 && operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 &&
                operationBodyMapper.nonMappedElementsT2() == 0) {
                int countableStatements = 0;
                int parameterizedVariableDeclarationStatements = 0;
                UMLOperation addedOperation = operationBodyMapper.getOperation2();
                List<String> nonMappedLeavesT1 = new ArrayList<>();
                for (StatementObject statement : operationBodyMapper.getNonMappedLeavesT1()) {
                    if (statement.countableStatement()) {
                        nonMappedLeavesT1.add(statement.getString());
                        for (String parameterName : addedOperation.getParameterNameList()) {
                            if (statement.getVariableDeclaration(parameterName) != null) {
                                parameterizedVariableDeclarationStatements++;
                                break;
                            }
                        }
                        countableStatements++;
                    }
                }
                int nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation = 0;
                for (UMLOperation operation : addedOperations) {
                    if (!operation.equals(addedOperation) && operation.getBody() != null) {
                        for (StatementObject statement : operation.getBody().getCompositeStatement().getLeaves()) {
                            if (nonMappedLeavesT1.contains(statement.getString())) {
                                nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation++;
                            }
                        }
                    }
                }
                return (countableStatements == parameterizedVariableDeclarationStatements ||
                    countableStatements == nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation + parameterizedVariableDeclarationStatements) &&
                    countableStatements > 0;
            } else if (operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() > 0 &&
                operationBodyMapper.getNonMappedInnerNodesT2().size() == 0) {
                int countableStatements = 0;
                int parameterizedVariableDeclarationStatements = 0;
                UMLOperation removedOperation = operationBodyMapper.getOperation1();
                for (StatementObject statement : operationBodyMapper.getNonMappedLeavesT2()) {
                    if (statement.countableStatement()) {
                        for (String parameterName : removedOperation.getParameterNameList()) {
                            if (statement.getVariableDeclaration(parameterName) != null) {
                                parameterizedVariableDeclarationStatements++;
                                break;
                            }
                        }
                        countableStatements++;
                    }
                }
                return countableStatements == parameterizedVariableDeclarationStatements && countableStatements > 0;
            } else if ((operationBodyMapper.nonMappedElementsT1() == 1 || operationBodyMapper.nonMappedElementsT2() == 1) &&
                operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.getNonMappedInnerNodesT2().size() == 0) {
                StatementObject statementUsingParameterAsInvoker1 = null;
                UMLOperation removedOperation = operationBodyMapper.getOperation1();
                for (StatementObject statement : operationBodyMapper.getNonMappedLeavesT1()) {
                    if (statement.countableStatement()) {
                        for (String parameterName : removedOperation.getParameterNameList()) {
                            OperationInvocation invocation = statement.invocationCoveringEntireFragment();
                            if (invocation != null && invocation.getExpression() != null && invocation.getExpression().equals(
                                parameterName)) {
                                statementUsingParameterAsInvoker1 = statement;
                                break;
                            }
                        }
                    }
                }
                StatementObject statementUsingParameterAsInvoker2 = null;
                UMLOperation addedOperation = operationBodyMapper.getOperation2();
                for (StatementObject statement : operationBodyMapper.getNonMappedLeavesT2()) {
                    if (statement.countableStatement()) {
                        for (String parameterName : addedOperation.getParameterNameList()) {
                            OperationInvocation invocation = statement.invocationCoveringEntireFragment();
                            if (invocation != null && invocation.getExpression() != null && invocation.getExpression().equals(
                                parameterName)) {
                                statementUsingParameterAsInvoker2 = statement;
                                break;
                            }
                        }
                    }
                }
                if (statementUsingParameterAsInvoker1 != null && statementUsingParameterAsInvoker2 != null) {
                    for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
                        if (mapping.getFragment1() instanceof CompositeStatementObject && mapping.getFragment2() instanceof CompositeStatementObject) {
                            CompositeStatementObject parent1 = (CompositeStatementObject) mapping.getFragment1();
                            CompositeStatementObject parent2 = (CompositeStatementObject) mapping.getFragment2();
                            if (parent1.getLeaves().contains(
                                statementUsingParameterAsInvoker1) && parent2.getLeaves().contains(
                                statementUsingParameterAsInvoker2)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


    private boolean mappedElementsMoreThanNonMappedT1AndT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
        int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
        int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
        return (mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
            (nonMappedElementsT1 == 0 && mappings > Math.floor(nonMappedElementsT2 / 2.0)) ||
            (mappings == 1 && nonMappedElementsT1 + nonMappedElementsT2 == 1 && operationBodyMapper.getOperation1().getName().equals(
                operationBodyMapper.getOperation2().getName()));
    }

    private boolean mappedElementsMoreThanNonMappedT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
        int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
        int nonMappedElementsT2CallingAddedOperation =
            operationBodyMapper.nonMappedElementsT2CallingAddedOperation(addedOperations);
        int nonMappedElementsT2WithoutThoseCallingAddedOperation =
            nonMappedElementsT2 - nonMappedElementsT2CallingAddedOperation;
        return mappings > nonMappedElementsT2 || (mappings >= nonMappedElementsT2WithoutThoseCallingAddedOperation &&
            nonMappedElementsT2CallingAddedOperation >= nonMappedElementsT2WithoutThoseCallingAddedOperation);
    }

    private boolean mappedElementsMoreThanNonMappedT1(int mappings, UMLOperationBodyMapper operationBodyMapper) {
        int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
        int nonMappedElementsT1CallingRemovedOperation =
            operationBodyMapper.nonMappedElementsT1CallingRemovedOperation(removedOperations);
        int nonMappedElementsT1WithoutThoseCallingRemovedOperation =
            nonMappedElementsT1 - nonMappedElementsT1CallingRemovedOperation;
        return mappings > nonMappedElementsT1 || (mappings >= nonMappedElementsT1WithoutThoseCallingRemovedOperation &&
            nonMappedElementsT1CallingRemovedOperation >= nonMappedElementsT1WithoutThoseCallingRemovedOperation);
    }


    private boolean operationsBeforeAndAfterMatch(UMLOperation removedOperation, UMLOperation addedOperation) {
        UMLOperation operationBefore1 = null;
        UMLOperation operationAfter1 = null;
        List<UMLOperation> originalClassOperations = originalFile.getOperations();
        for (int i = 0; i < originalClassOperations.size(); i++) {
            UMLOperation current = originalClassOperations.get(i);
            if (current.equals(removedOperation)) {
                if (i > 0) {
                    operationBefore1 = originalClassOperations.get(i - 1);
                }
                if (i < originalClassOperations.size() - 1) {
                    operationAfter1 = originalClassOperations.get(i + 1);
                }
            }
        }

        UMLOperation operationBefore2 = null;
        UMLOperation operationAfter2 = null;
        List<UMLOperation> nextClassOperations = nextFile.getOperations();
        for (int i = 0; i < nextClassOperations.size(); i++) {
            UMLOperation current = nextClassOperations.get(i);
            if (current.equals(addedOperation)) {
                if (i > 0) {
                    operationBefore2 = nextClassOperations.get(i - 1);
                }
                if (i < nextClassOperations.size() - 1) {
                    operationAfter2 = nextClassOperations.get(i + 1);
                }
            }
        }

        boolean operationsBeforeMatch = false;
        if (operationBefore1 != null && operationBefore2 != null) {
            operationsBeforeMatch =
                operationBefore1.equalParameterTypes(operationBefore2) && operationBefore1.getName().equals(
                    operationBefore2.getName());
        }

        boolean operationsAfterMatch = false;
        if (operationAfter1 != null && operationAfter2 != null) {
            operationsAfterMatch =
                operationAfter1.equalParameterTypes(operationAfter2) && operationAfter1.getName().equals(
                    operationAfter2.getName());
        }

        return operationsBeforeMatch || operationsAfterMatch;
    }


    private boolean gettersWithDifferentReturnType(UMLOperation removedOperation, UMLOperation addedOperation) {
/*   TODO     if(removedOperation.isGetter() && addedOperation.isGetter()) {
            UMLType type1 = removedOperation.getReturnParameter().getType();
            UMLType type2 = addedOperation.getReturnParameter().getType();
            if(!removedOperation.equalReturnParameter(addedOperation) && !type1.compatibleTypes(type2)) {
                return true;
            }
        }*/
        return false;
    }

    private boolean compatibleSignatures(UMLOperation removedOperation,
                                         UMLOperation addedOperation,
                                         int absoluteDifferenceInPosition) {
        return addedOperation.compatibleSignature(removedOperation) ||
            (
                (absoluteDifferenceInPosition == 0 || operationsBeforeAndAfterMatch(removedOperation,
                    addedOperation)) &&
                    !gettersWithDifferentReturnType(removedOperation, addedOperation) &&
                    (addedOperation.getParameterTypeList().equals(
                        removedOperation.getParameterTypeList()) || addedOperation.normalizedNameDistance(
                        removedOperation) <= 0.4)
            );
    }

    private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet,
                                 UMLOperation removedOperation,
                                 UMLOperation addedOperation,
                                 int differenceInPosition) throws RefactoringMinerTimedOutException {
        UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(this, removedOperation,
            addedOperation);
        List<AbstractCodeMapping> totalMappings = new ArrayList<>(operationBodyMapper.getMappings());
        int mappings = operationBodyMapper.mappingsWithoutBlocks();
        if (mappings > 0) {
            int absoluteDifferenceInPosition =
                computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
            if (exactMappings(operationBodyMapper)) {
                mapperSet.add(operationBodyMapper);
            } else if (mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
                absoluteDifferenceInPosition <= differenceInPosition &&
                compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
                mapperSet.add(operationBodyMapper);
            } else if (mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
                absoluteDifferenceInPosition <= differenceInPosition &&
                isPartOfMethodExtracted(removedOperation, addedOperation)) {
                mapperSet.add(operationBodyMapper);
            } else if (mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
                absoluteDifferenceInPosition <= differenceInPosition &&
                isPartOfMethodInlined(removedOperation, addedOperation)) {
                mapperSet.add(operationBodyMapper);
            }
        } else {
            for (MethodInvocationReplacement replacement : consistentMethodInvocationRenames) {
                if (replacement.getInvokedOperationBefore().matchesOperation(removedOperation) &&
                    replacement.getInvokedOperationAfter().matchesOperation(addedOperation)) {
                    mapperSet.add(operationBodyMapper);
                    break;
                }
            }
        }
        if (totalMappings.size() > 0) {
            int absoluteDifferenceInPosition =
                computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
            if (singleUnmatchedStatementCallsAddedOperation(operationBodyMapper) &&
                absoluteDifferenceInPosition <= differenceInPosition &&
                compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
                mapperSet.add(operationBodyMapper);
            }
        }
    }

    private UMLOperationBodyMapper findBestMapper(TreeSet<UMLOperationBodyMapper> mapperSet) {
        List<UMLOperationBodyMapper> mapperList = new ArrayList<>(mapperSet);
        UMLOperationBodyMapper bestMapper = mapperSet.first();
        UMLOperation bestMapperOperation1 = bestMapper.getOperation1();
        UMLOperation bestMapperOperation2 = bestMapper.getOperation2();
        if (bestMapperOperation1.equalReturnParameter(bestMapperOperation2) &&
            bestMapperOperation1.getName().equals(bestMapperOperation2.getName()) &&
            bestMapperOperation1.commonParameterTypes(bestMapperOperation2).size() > 0) {
            return bestMapper;
        }
        boolean identicalBodyWithOperation1OfTheBestMapper = identicalBodyWithAnotherAddedMethod(bestMapper);
        boolean identicalBodyWithOperation2OfTheBestMapper = identicalBodyWithAnotherRemovedMethod(bestMapper);
        for (int i = 1; i < mapperList.size(); i++) {
            UMLOperationBodyMapper mapper = mapperList.get(i);
            UMLOperation operation2 = mapper.getOperation2();
            List<OperationInvocation> operationInvocations2 = operation2.getAllOperationInvocations();
            boolean anotherMapperCallsOperation2OfTheBestMapper = false;
            for (OperationInvocation invocation : operationInvocations2) {
                if (invocation.matchesOperation(bestMapper.getOperation2(), operation2.variableTypeMap(),
                    modelDiff) && !invocation.matchesOperation(bestMapper.getOperation1(),
                    operation2.variableTypeMap(),
                    modelDiff) &&
                    !operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation,
                        removedOperations)) {
                    anotherMapperCallsOperation2OfTheBestMapper = true;
                    break;
                }
            }
            UMLOperation operation1 = mapper.getOperation1();
            List<OperationInvocation> operationInvocations1 = operation1.getAllOperationInvocations();
            boolean anotherMapperCallsOperation1OfTheBestMapper = false;
            for (OperationInvocation invocation : operationInvocations1) {
                if (invocation.matchesOperation(bestMapper.getOperation1(), operation1.variableTypeMap(),
                    modelDiff) && !invocation.matchesOperation(bestMapper.getOperation2(),
                    operation1.variableTypeMap(),
                    modelDiff) &&
                    !operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation,
                        addedOperations)) {
                    anotherMapperCallsOperation1OfTheBestMapper = true;
                    break;
                }
            }
            boolean nextMapperMatchesConsistentRename =
                matchesConsistentMethodInvocationRename(mapper, consistentMethodInvocationRenames);
            boolean bestMapperMismatchesConsistentRename =
                mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenames);
            if (bestMapperMismatchesConsistentRename && nextMapperMatchesConsistentRename) {
                bestMapper = mapper;
                break;
            }
            if (anotherMapperCallsOperation2OfTheBestMapper || anotherMapperCallsOperation1OfTheBestMapper) {
                bestMapper = mapper;
                break;
            }
            if (identicalBodyWithOperation2OfTheBestMapper || identicalBodyWithOperation1OfTheBestMapper) {
                bestMapper = mapper;
                break;
            }
        }
        if (mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenames)) {
            return null;
        }
        return bestMapper;
    }

    private boolean operationContainsMethodInvocationWithTheSameNameAndCommonArguments(OperationInvocation invocation,
                                                                                       List<UMLOperation> operations) {
        for (UMLOperation operation : operations) {
            List<OperationInvocation> operationInvocations = operation.getAllOperationInvocations();
            for (OperationInvocation operationInvocation : operationInvocations) {
                Set<String> argumentIntersection = new LinkedHashSet<>(operationInvocation.getArguments());
                argumentIntersection.retainAll(invocation.getArguments());
                if (operationInvocation.getMethodName().equals(
                    invocation.getMethodName()) && !argumentIntersection.isEmpty()) {
                    return true;
                } else if (argumentIntersection.size() > 0 && argumentIntersection.size() == invocation.getArguments().size()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean singleUnmatchedStatementCallsAddedOperation(UMLOperationBodyMapper operationBodyMapper) {
        List<StatementObject> nonMappedLeavesT1 = operationBodyMapper.getNonMappedLeavesT1();
        List<StatementObject> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
        if (nonMappedLeavesT1.size() == 1 && nonMappedLeavesT2.size() == 1) {
            StatementObject statementT2 = nonMappedLeavesT2.get(0);
            OperationInvocation invocationT2 = statementT2.invocationCoveringEntireFragment();
            if (invocationT2 != null) {
                for (UMLOperation addedOperation : addedOperations) {
                    if (invocationT2.matchesOperation(addedOperation,
                        operationBodyMapper.getOperation2().variableTypeMap(),
                        modelDiff)) {
                        StatementObject statementT1 = nonMappedLeavesT1.get(0);
                        OperationInvocation invocationT1 = statementT1.invocationCoveringEntireFragment();
                        if (invocationT1 != null && addedOperation.getAllOperationInvocations().contains(
                            invocationT1)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation) {
        List<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
        List<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
        Set<OperationInvocation> intersection = new LinkedHashSet<>(removedOperationInvocations);
        intersection.retainAll(addedOperationInvocations);
        int numberOfInvocationsMissingFromRemovedOperation =
            new LinkedHashSet<>(removedOperationInvocations).size() - intersection.size();

        Set<OperationInvocation> operationInvocationsInMethodsCalledByAddedOperation =
            new LinkedHashSet<>();
        for (OperationInvocation addedOperationInvocation : addedOperationInvocations) {
            if (!intersection.contains(addedOperationInvocation)) {
                for (UMLOperation operation : addedOperations) {
                    if (!operation.equals(addedOperation) && operation.getBody() != null) {
                        if (addedOperationInvocation.matchesOperation(operation, addedOperation.variableTypeMap(),
                            modelDiff)) {
                            //addedOperation calls another added method
                            operationInvocationsInMethodsCalledByAddedOperation.addAll(
                                operation.getAllOperationInvocations());
                        }
                    }
                }
            }
        }
        Set<OperationInvocation> newIntersection = new LinkedHashSet<>(removedOperationInvocations);
        newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);

        Set<OperationInvocation> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted =
            new LinkedHashSet<>(removedOperationInvocations);
        removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
        removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
        removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeIf(
            invocation -> invocation.getMethodName().startsWith("get"));
        int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
        int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations =
            numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
        return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations >
            numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations ||
            numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations >
                removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size();
    }

    private boolean isPartOfMethodInlined(UMLOperation removedOperation, UMLOperation addedOperation) {
        List<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
        List<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
        Set<OperationInvocation> intersection = new LinkedHashSet<>(removedOperationInvocations);
        intersection.retainAll(addedOperationInvocations);
        int numberOfInvocationsMissingFromAddedOperation =
            new LinkedHashSet<>(addedOperationInvocations).size() - intersection.size();

        Set<OperationInvocation> operationInvocationsInMethodsCalledByRemovedOperation =
            new LinkedHashSet<>();
        for (OperationInvocation removedOperationInvocation : removedOperationInvocations) {
            if (!intersection.contains(removedOperationInvocation)) {
                for (UMLOperation operation : removedOperations) {
                    if (!operation.equals(removedOperation) && operation.getBody() != null) {
                        if (removedOperationInvocation.matchesOperation(operation, removedOperation.variableTypeMap(),
                            modelDiff)) {
                            //removedOperation calls another removed method
                            operationInvocationsInMethodsCalledByRemovedOperation.addAll(
                                operation.getAllOperationInvocations());
                        }
                    }
                }
            }
        }
        Set<OperationInvocation> newIntersection = new LinkedHashSet<>(addedOperationInvocations);
        newIntersection.retainAll(operationInvocationsInMethodsCalledByRemovedOperation);

        int numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations = newIntersection.size();
        int numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations =
            numberOfInvocationsMissingFromAddedOperation - numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations;
        return numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations;
    }

    public static boolean allMappingsAreExactMatches(UMLOperationBodyMapper operationBodyMapper) {
        int mappings = operationBodyMapper.mappingsWithoutBlocks();
        int tryMappings = 0;
        int mappingsWithTypeReplacement = 0;
        for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
            if (mapping.getFragment1().getString().equals("try") && mapping.getFragment2().getString().equals("try")) {
                tryMappings++;
            }
            if (mapping.containsReplacement(Replacement.ReplacementType.TYPE)) {
                mappingsWithTypeReplacement++;
            }
        }
        if (mappings == operationBodyMapper.exactMatches() + tryMappings) {
            return true;
        }
        return mappings == operationBodyMapper.exactMatches() + tryMappings + mappingsWithTypeReplacement && mappings > mappingsWithTypeReplacement;
    }

    private boolean identicalBodyWithAnotherAddedMethod(UMLOperationBodyMapper mapper) {
        UMLOperation operation1 = mapper.getOperation1();
        List<String> stringRepresentation = operation1.stringRepresentation();
        if (stringRepresentation.size() > 2) {
            for (UMLOperation addedOperation : addedOperations) {
                if (!mapper.getOperation2().equals(addedOperation)) {
                    if (addedOperation.stringRepresentation().equals(stringRepresentation)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean identicalBodyWithAnotherRemovedMethod(UMLOperationBodyMapper mapper) {
        UMLOperation operation2 = mapper.getOperation2();
        List<String> stringRepresentation = operation2.stringRepresentation();
        if (stringRepresentation.size() > 2) {
            for (UMLOperation removedOperation : removedOperations) {
                if (!mapper.getOperation1().equals(removedOperation)) {
                    if (removedOperation.stringRepresentation().equals(stringRepresentation)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean matchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper,
                                                            Set<MethodInvocationReplacement> consistentMethodInvocationRenames) {
        for (MethodInvocationReplacement rename : consistentMethodInvocationRenames) {
            if (mapper.getOperation1().getName().equals(rename.getBefore()) && mapper.getOperation2().getName().equals(
                rename.getAfter())) {
                return true;
            }
        }
        return false;
    }

    private boolean mismatchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper,
                                                               Set<MethodInvocationReplacement> consistentMethodInvocationRenames) {
        for (MethodInvocationReplacement rename : consistentMethodInvocationRenames) {
            if (mapper.getOperation1().getName().equals(rename.getBefore()) && !mapper.getOperation2().getName().equals(
                rename.getAfter())) {
                return true;
            } else if (!mapper.getOperation1().getName().equals(
                rename.getBefore()) && mapper.getOperation2().getName().equals(rename.getAfter())) {
                return true;
            }
        }
        return false;
    }

    private void reportAddedOperation(UMLOperation umlOperation) {
        this.addedOperations.add(umlOperation);
    }

    private void reportRemovedOperation(UMLOperation umlOperation) {
        this.removedOperations.add(umlOperation);
    }

    private boolean containsMapperForOperation(UMLOperation operation) {
        for (UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
            if (mapper.getOperation1().equalsQualified(operation)) {
                return true;
            }
        }
        return false;
    }

    public List<UMLOperationBodyMapper> getOperationBodyMapperList() {
        return operationBodyMapperList;
    }

    public UMLModelDiff getModelDiff() {
        return modelDiff;
    }

    public void addOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper) {
        this.operationBodyMapperList.add(operationBodyMapper);
    }

    @Override
    public int compareTo(@NotNull UMLFileDiff o) {
        return this.originalFile.getFileName().compareTo(o.originalFile.getFileName());
    }

    public Collection<? extends Refactoring> getRefactorings() {
        return this.refactorings;
    }
}
