package org.jetbrains.research.kotlinrminer.ide.decomposition;

import org.jetbrains.annotations.NotNull;

import org.jetbrains.research.kotlinrminer.common.decomposition.CodeElementType;
import org.jetbrains.research.kotlinrminer.common.replacement.*;
import org.jetbrains.research.kotlinrminer.common.util.StringDistance;
import org.jetbrains.research.kotlinrminer.ide.Refactoring;
import org.jetbrains.research.kotlinrminer.ide.RefactoringMinerTimedOutException;
import org.jetbrains.research.kotlinrminer.ide.diff.*;
import org.jetbrains.research.kotlinrminer.ide.diff.refactoring.CandidateAttributeRefactoring;
import org.jetbrains.research.kotlinrminer.ide.diff.refactoring.CandidateMergeVariableRefactoring;
import org.jetbrains.research.kotlinrminer.ide.diff.refactoring.CandidateSplitVariableRefactoring;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLAttribute;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLParameter;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLType;
import org.jetbrains.research.kotlinrminer.ide.decomposition.replacement.*;
import org.jetbrains.research.kotlinrminer.common.util.PrefixSuffixUtils;
import org.jetbrains.research.kotlinrminer.ide.util.ReplacementUtil;
import org.jetbrains.research.kotlinrminer.ide.decomposition.replacement.VariableReplacementWithMethodInvocation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO: Figure out how to split this large class

/**
 * Performs matching of statements from methods' bodies from parent and child commit.
 */
public class UMLOperationBodyMapper implements Comparable<UMLOperationBodyMapper> {
    private final UMLOperation operation1;
    private final UMLOperation operation2;
    private final Set<AbstractCodeMapping> mappings;
    private final List<StatementObject> nonMappedLeavesT1;
    private final List<StatementObject> nonMappedLeavesT2;
    private final List<CompositeStatementObject> nonMappedInnerNodesT1;
    private final List<CompositeStatementObject> nonMappedInnerNodesT2;
    private final Set<Refactoring> refactorings = new LinkedHashSet<>();
    private final Set<CandidateAttributeRefactoring> candidateAttributeRenames = new
        LinkedHashSet<>();
    private final Set<CandidateMergeVariableRefactoring> candidateAttributeMerges =
        new LinkedHashSet<>();
    private final Set<CandidateSplitVariableRefactoring> candidateAttributeSplits =
        new LinkedHashSet<>();
    private final List<UMLOperationBodyMapper> childMappers = new ArrayList<>();
    private UMLOperationBodyMapper parentMapper;
    private static final Pattern SPLIT_CONDITIONAL_PATTERN = Pattern.compile("(\\|\\|)|(&&)|(\\?)|(:)");
    public static final String SPLIT_CONCAT_STRING_PATTERN = "(\\s)*(\\+)(\\s)*";
    private static final Pattern DOUBLE_QUOTES = Pattern.compile("\"([^\"]*)\"|(\\S+)");
    private final UMLClassBaseDiff classDiff;
    private UMLFileDiff fileDiff;
    private UMLModelDiff modelDiff;
    private UMLOperation callSiteOperation;
    private final Map<AbstractCodeFragment, UMLOperation> codeFragmentOperationMap1 = new LinkedHashMap<>();
    private final Map<AbstractCodeFragment, UMLOperation> codeFragmentOperationMap2 = new LinkedHashMap<>();

    public UMLOperationBodyMapper(UMLOperation operation1, UMLOperation operation2,
                                  UMLClassBaseDiff classDiff) throws RefactoringMinerTimedOutException {
        this.classDiff = classDiff;
        this.fileDiff = null;
        if (classDiff != null)
            this.modelDiff = classDiff.getModelDiff();
        this.operation1 = operation1;
        this.operation2 = operation2;
        this.mappings = new LinkedHashSet<>();
        this.nonMappedLeavesT1 = new ArrayList<>();
        this.nonMappedLeavesT2 = new ArrayList<>();
        this.nonMappedInnerNodesT1 = new ArrayList<>();
        this.nonMappedInnerNodesT2 = new ArrayList<>();
        OperationBody body1 = operation1.getBody();
        OperationBody body2 = operation2.getBody();
        if (body1 != null && body2 != null) {
            CompositeStatementObject composite1 = body1.getCompositeStatement();
            CompositeStatementObject composite2 = body2.getCompositeStatement();
            List<StatementObject> leaves1 = composite1.getLeaves();
            List<StatementObject> leaves2 = composite2.getLeaves();

            UMLOperationDiff operationDiff = new UMLOperationDiff(operation1, operation2);
            Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<>();
            Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<>();
            List<UMLParameter> addedParameters = operationDiff.getAddedParameters();
            if (addedParameters.size() == 1) {
                UMLParameter addedParameter = addedParameters.get(0);
                if (UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(),
                    operation1.getClassName())) {
                    parameterToArgumentMap1.put("this.", "");
                    //replace "parameterName." with ""
                    parameterToArgumentMap2.put(addedParameter.getName() + ".", "");
                }
            }
            List<UMLParameter> removedParameters = operationDiff.getRemovedParameters();
            if (removedParameters.size() == 1) {
                UMLParameter removedParameter = removedParameters.get(0);
                if (UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(),
                    operation2.getClassName())) {
                    parameterToArgumentMap1.put(removedParameter.getName() + ".", "");
                    parameterToArgumentMap2.put("this.", "");
                }
            }
            resetNodes(leaves1);
            //replace parameters with arguments in leaves1
            if (!parameterToArgumentMap1.isEmpty()) {
                for (StatementObject leave1 : leaves1) {
                    leave1.replaceParametersWithArguments(parameterToArgumentMap1);
                }
            }
            resetNodes(leaves2);
            //replace parameters with arguments in leaves2
            if (!parameterToArgumentMap2.isEmpty()) {
                for (StatementObject leave2 : leaves2) {
                    leave2.replaceParametersWithArguments(parameterToArgumentMap2);
                }
            }
            processLeaves(leaves1, leaves2, new LinkedHashMap<>());

            List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
            innerNodes1.remove(composite1);
            List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
            innerNodes2.remove(composite2);
            resetNodes(innerNodes1);
            //replace parameters with arguments in innerNodes1
            if (!parameterToArgumentMap1.isEmpty()) {
                for (CompositeStatementObject innerNode1 : innerNodes1) {
                    innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
                }
            }
            resetNodes(innerNodes2);
            //replace parameters with arguments in innerNodes2
            if (!parameterToArgumentMap2.isEmpty()) {
                for (CompositeStatementObject innerNode2 : innerNodes2) {
                    innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
                }
            }
            processInnerNodes(innerNodes1, innerNodes2, new LinkedHashMap<>());

            nonMappedLeavesT1.addAll(leaves1);
            nonMappedLeavesT2.addAll(leaves2);
            nonMappedInnerNodesT1.addAll(innerNodes1);
            nonMappedInnerNodesT2.addAll(innerNodes2);

            for (StatementObject statement : getNonMappedLeavesT2()) {
                temporaryVariableAssignment(statement, nonMappedLeavesT2);
            }
            for (StatementObject statement : getNonMappedLeavesT1()) {
                inlinedVariableAssignment(statement, nonMappedLeavesT2);
            }
        }
    }

    private UMLOperationBodyMapper(LambdaExpressionObject lambda1,
                                   LambdaExpressionObject lambda2,
                                   UMLOperationBodyMapper parentMapper) throws RefactoringMinerTimedOutException {
        this.classDiff = parentMapper.classDiff;
        this.fileDiff = null;
        if (classDiff != null)
            this.modelDiff = classDiff.getModelDiff();
        this.operation1 = parentMapper.operation1;
        this.operation2 = parentMapper.operation2;
        this.mappings = new LinkedHashSet<>();
        this.nonMappedLeavesT1 = new ArrayList<>();
        this.nonMappedLeavesT2 = new ArrayList<>();
        this.nonMappedInnerNodesT1 = new ArrayList<>();
        this.nonMappedInnerNodesT2 = new ArrayList<>();

        if (lambda1.getExpression() != null && lambda2.getExpression() != null) {
            List<AbstractExpression> leaves1 = new ArrayList<>();
            leaves1.add(lambda1.getExpression());
            List<AbstractExpression> leaves2 = new ArrayList<>();
            leaves2.add(lambda2.getExpression());
            processLeaves(leaves1, leaves2, new LinkedHashMap<>());
        } else if (lambda1.getBody() != null && lambda2.getBody() != null) {
            CompositeStatementObject composite1 = lambda1.getBody().getCompositeStatement();
            CompositeStatementObject composite2 = lambda2.getBody().getCompositeStatement();
            List<StatementObject> leaves1 = composite1.getLeaves();
            List<StatementObject> leaves2 = composite2.getLeaves();
            processLeaves(leaves1, leaves2, new LinkedHashMap<>());

            List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
            List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
            processInnerNodes(innerNodes1, innerNodes2, new LinkedHashMap<>());

            nonMappedLeavesT1.addAll(leaves1);
            nonMappedLeavesT2.addAll(leaves2);
            nonMappedInnerNodesT1.addAll(innerNodes1);
            nonMappedInnerNodesT2.addAll(innerNodes2);

            for (StatementObject statement : getNonMappedLeavesT2()) {
                temporaryVariableAssignment(statement, nonMappedLeavesT2);
            }
            for (StatementObject statement : getNonMappedLeavesT1()) {
                inlinedVariableAssignment(statement, nonMappedLeavesT2);
            }
        }
    }

    public UMLOperationBodyMapper(UMLFileDiff fileDiff, UMLOperation operation1, UMLOperation operation2) throws
        RefactoringMinerTimedOutException {
        this.classDiff = null;
        this.fileDiff = fileDiff;
        if (fileDiff != null)
            this.modelDiff = fileDiff.getModelDiff();
        this.operation1 = operation1;
        this.operation2 = operation2;
        this.mappings = new LinkedHashSet<>();
        this.nonMappedLeavesT1 = new ArrayList<>();
        this.nonMappedLeavesT2 = new ArrayList<>();
        this.nonMappedInnerNodesT1 = new ArrayList<>();
        this.nonMappedInnerNodesT2 = new ArrayList<>();
        OperationBody body1 = operation1.getBody();
        OperationBody body2 = operation2.getBody();
        if (body1 != null && body2 != null) {
            CompositeStatementObject composite1 = body1.getCompositeStatement();
            CompositeStatementObject composite2 = body2.getCompositeStatement();
            List<StatementObject> leaves1 = composite1.getLeaves();
            List<StatementObject> leaves2 = composite2.getLeaves();

            UMLOperationDiff operationDiff = new UMLOperationDiff(operation1, operation2);
            Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<>();
            Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<>();
            List<UMLParameter> addedParameters = operationDiff.getAddedParameters();
            if (addedParameters.size() == 1) {
                UMLParameter addedParameter = addedParameters.get(0);
                if (UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(),
                    operation1.getClassName())) {
                    parameterToArgumentMap1.put("this.", "");
                    //replace "parameterName." with ""
                    parameterToArgumentMap2.put(addedParameter.getName() + ".", "");
                }
            }
            List<UMLParameter> removedParameters = operationDiff.getRemovedParameters();
            if (removedParameters.size() == 1) {
                UMLParameter removedParameter = removedParameters.get(0);
                if (UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(),
                    operation2.getClassName())) {
                    parameterToArgumentMap1.put(removedParameter.getName() + ".", "");
                    parameterToArgumentMap2.put("this.", "");
                }
            }
            resetNodes(leaves1);
            //replace parameters with arguments in leaves1
            if (!parameterToArgumentMap1.isEmpty()) {
                for (StatementObject leave1 : leaves1) {
                    leave1.replaceParametersWithArguments(parameterToArgumentMap1);
                }
            }
            resetNodes(leaves2);
            //replace parameters with arguments in leaves2
            if (!parameterToArgumentMap2.isEmpty()) {
                for (StatementObject leave2 : leaves2) {
                    leave2.replaceParametersWithArguments(parameterToArgumentMap2);
                }
            }
            processLeaves(leaves1, leaves2, new LinkedHashMap<>());

            List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
            innerNodes1.remove(composite1);
            List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
            innerNodes2.remove(composite2);
            resetNodes(innerNodes1);
            //replace parameters with arguments in innerNodes1
            if (!parameterToArgumentMap1.isEmpty()) {
                for (CompositeStatementObject innerNode1 : innerNodes1) {
                    innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
                }
            }
            resetNodes(innerNodes2);
            //replace parameters with arguments in innerNodes2
            if (!parameterToArgumentMap2.isEmpty()) {
                for (CompositeStatementObject innerNode2 : innerNodes2) {
                    innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
                }
            }
            processInnerNodes(innerNodes1, innerNodes2, new LinkedHashMap<>());

            nonMappedLeavesT1.addAll(leaves1);
            nonMappedLeavesT2.addAll(leaves2);
            nonMappedInnerNodesT1.addAll(innerNodes1);
            nonMappedInnerNodesT2.addAll(innerNodes2);

            for (StatementObject statement : getNonMappedLeavesT2()) {
                temporaryVariableAssignment(statement, nonMappedLeavesT2);
            }
            for (StatementObject statement : getNonMappedLeavesT1()) {
                inlinedVariableAssignment(statement, nonMappedLeavesT2);
            }
        }
    }

    public UMLOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper,
                                  UMLOperation addedOperation,
                                  Map<String, String> parameterToArgumentMap1,
                                  Map<String, String> parameterToArgumentMap2,
                                  UMLClassBaseDiff classDiff) throws RefactoringMinerTimedOutException {
        this.parentMapper = operationBodyMapper;
        this.operation1 = operationBodyMapper.operation1;
        this.callSiteOperation = operationBodyMapper.operation2;
        this.operation2 = addedOperation;
        this.classDiff = classDiff;
        this.mappings = new LinkedHashSet<>();
        this.nonMappedLeavesT1 = new ArrayList<>();
        this.nonMappedLeavesT2 = new ArrayList<>();
        this.nonMappedInnerNodesT1 = new ArrayList<>();
        this.nonMappedInnerNodesT2 = new ArrayList<>();

        OperationBody addedOperationBody = addedOperation.getBody();
        if (addedOperationBody != null) {
            CompositeStatementObject composite2 = addedOperationBody.getCompositeStatement();
            List<StatementObject> leaves1 = operationBodyMapper.getNonMappedLeavesT1();
            List<CompositeStatementObject> innerNodes1 = operationBodyMapper.getNonMappedInnerNodesT1();
            //adding leaves that were mapped with replacements
            Set<StatementObject> addedLeaves1 = new LinkedHashSet<>();
            Set<CompositeStatementObject> addedInnerNodes1 = new LinkedHashSet<>();
            for (StatementObject nonMappedLeaf1 : new ArrayList<>(operationBodyMapper.getNonMappedLeavesT1())) {
                // expandAnonymousAndLambdas(nonMappedLeaf1, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1,operationBodyMapper);
            }
            for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
                if (!returnWithVariableReplacement(mapping) && !nullLiteralReplacements(
                    mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(
                    mapping.getFragment2()))) {
                    AbstractCodeFragment fragment = mapping.getFragment1();
                    // expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1,operationBodyMapper);
                }
            }
            List<StatementObject> leaves2 = composite2.getLeaves();
            List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
            Set<StatementObject> addedLeaves2 = new LinkedHashSet<>();
            Set<CompositeStatementObject> addedInnerNodes2 = new LinkedHashSet<>();
            for (StatementObject statement : leaves2) {
/*                if(!statement.getAnonymousClassDeclarations().isEmpty()) {
                    List<UMLAnonymousClass> anonymousList = operation2.getAnonymousClassList();
                    for(UMLAnonymousClass anonymous : anonymousList) {
                        if(anonymous.isDirectlyNested() && statement.getLocationInfo().subsumes(anonymous.getLocationInfo())) {
                            for(UMLOperation anonymousOperation : anonymous.getOperations()) {
                                List<StatementObject> anonymousClassLeaves = anonymousOperation.getBody().getCompositeStatement().getLeaves();
                                for(StatementObject anonymousLeaf : anonymousClassLeaves) {
                                    if(!leaves2.contains(anonymousLeaf)) {
                                        addedLeaves2.add(anonymousLeaf);
                                        codeFragmentOperationMap2.put(anonymousLeaf, anonymousOperation);
                                    }
                                }
                                List<CompositeStatementObject> anonymousClassInnerNodes = anonymousOperation.getBody().getCompositeStatement().getInnerNodes();
                                for(CompositeStatementObject anonymousInnerNode : anonymousClassInnerNodes) {
                                    if(!innerNodes2.contains(anonymousInnerNode)) {
                                        addedInnerNodes2.add(anonymousInnerNode);
                                        codeFragmentOperationMap2.put(anonymousInnerNode, anonymousOperation);
                                    }
                                }
                            }
                        }
                    }
                }*/
                if (!statement.getLambdas().isEmpty()) {
                    for (LambdaExpressionObject lambda : statement.getLambdas()) {
                        if (lambda.getBody() != null) {
                            List<StatementObject> lambdaLeaves = lambda.getBody().getCompositeStatement().getLeaves();
                            for (StatementObject lambdaLeaf : lambdaLeaves) {
                                if (!leaves2.contains(lambdaLeaf)) {
                                    addedLeaves2.add(lambdaLeaf);
                                    codeFragmentOperationMap2.put(lambdaLeaf, operation2);
                                }
                            }
                            List<CompositeStatementObject> lambdaInnerNodes =
                                lambda.getBody().getCompositeStatement().getInnerNodes();
                            for (CompositeStatementObject lambdaInnerNode : lambdaInnerNodes) {
                                if (!innerNodes2.contains(lambdaInnerNode)) {
                                    addedInnerNodes2.add(lambdaInnerNode);
                                    codeFragmentOperationMap2.put(lambdaInnerNode, operation2);
                                }
                            }
                        }
                    }
                }
            }
            leaves2.addAll(addedLeaves2);
            resetNodes(leaves1);
            //replace parameters with arguments in leaves1
            if (!parameterToArgumentMap1.isEmpty()) {
                for (StatementObject leave1 : leaves1) {
                    leave1.replaceParametersWithArguments(parameterToArgumentMap1);
                }
            }
            resetNodes(leaves2);
            //replace parameters with arguments in leaves2
            if (!parameterToArgumentMap2.isEmpty()) {
                for (StatementObject leave2 : leaves2) {
                    leave2.replaceParametersWithArguments(parameterToArgumentMap2);
                }
            }
            //compare leaves from T1 with leaves from T2
            processLeaves(leaves1, leaves2, parameterToArgumentMap2);

            //adding innerNodes that were mapped with replacements
            for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
                if (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(
                    mapping.getFragment2())) {
                    AbstractCodeFragment fragment = mapping.getFragment1();
                    if (fragment instanceof CompositeStatementObject) {
                        CompositeStatementObject statement = (CompositeStatementObject) fragment;
                        if (!innerNodes1.contains(statement)) {
                            innerNodes1.add(statement);
                            addedInnerNodes1.add(statement);
                        }
                    }
                }
            }
            innerNodes2.remove(composite2);
            innerNodes2.addAll(addedInnerNodes2);
            resetNodes(innerNodes1);
            //replace parameters with arguments in innerNodes1
            if (!parameterToArgumentMap1.isEmpty()) {
                for (CompositeStatementObject innerNode1 : innerNodes1) {
                    innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
                }
            }
            resetNodes(innerNodes2);
            //replace parameters with arguments in innerNode2
            if (!parameterToArgumentMap2.isEmpty()) {
                for (CompositeStatementObject innerNode2 : innerNodes2) {
                    innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
                }
            }
            //compare inner nodes from T1 with inner nodes from T2
            processInnerNodes(innerNodes1, innerNodes2, parameterToArgumentMap2);

            //match expressions in inner nodes from T1 with leaves from T2
            List<AbstractExpression> expressionsT1 = new ArrayList<>();
            for (CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
                for (AbstractExpression expression : composite.getExpressions()) {
                    expression.replaceParametersWithArguments(parameterToArgumentMap1);
                    expressionsT1.add(expression);
                }
            }
            int numberOfMappings = mappings.size();
            processLeaves(expressionsT1, leaves2, parameterToArgumentMap2);
            List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
            for (int i = numberOfMappings; i < mappings.size(); i++) {
                mappings.get(i).temporaryVariableAssignment(refactorings);
            }
            // TODO remove non-mapped inner nodes from T1 corresponding to mapped expressions

            //remove the leaves that were mapped with replacement, if they are not mapped again for a second time
            leaves1.removeAll(addedLeaves1);
            leaves2.removeAll(addedLeaves2);
            //remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
            innerNodes1.removeAll(addedInnerNodes1);
            innerNodes2.removeAll(addedInnerNodes2);
            nonMappedLeavesT1.addAll(leaves1);
            nonMappedLeavesT2.addAll(leaves2);
            nonMappedInnerNodesT1.addAll(innerNodes1);
            nonMappedInnerNodesT2.addAll(innerNodes2);

            for (StatementObject statement : getNonMappedLeavesT2()) {
                temporaryVariableAssignment(statement, nonMappedLeavesT2);
            }
            for (StatementObject statement : getNonMappedLeavesT1()) {
                inlinedVariableAssignment(statement, nonMappedLeavesT2);
            }
        }
    }

    public UMLOperationBodyMapper(UMLOperation removedOperation, UMLOperationBodyMapper operationBodyMapper,
                                  Map<String, String> parameterToArgumentMap, UMLClassBaseDiff classDiff) throws
        RefactoringMinerTimedOutException {
        this.parentMapper = operationBodyMapper;
        this.operation1 = removedOperation;
        this.operation2 = operationBodyMapper.operation2;
        this.callSiteOperation = operationBodyMapper.operation1;
        this.classDiff = classDiff;
        this.fileDiff = null;
        this.mappings = new LinkedHashSet<>();
        this.nonMappedLeavesT1 = new ArrayList<>();
        this.nonMappedLeavesT2 = new ArrayList<>();
        this.nonMappedInnerNodesT1 = new ArrayList<>();
        this.nonMappedInnerNodesT2 = new ArrayList<>();

        OperationBody removedOperationBody = removedOperation.getBody();
        if (removedOperationBody != null) {
            CompositeStatementObject composite1 = removedOperationBody.getCompositeStatement();
            List<StatementObject> leaves1 = composite1.getLeaves();
            List<StatementObject> leaves2 = operationBodyMapper.getNonMappedLeavesT2();
            //adding leaves that were mapped with replacements or are inexact matches
            Set<StatementObject> addedLeaves2 = new LinkedHashSet<>();
            for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
                if (!returnWithVariableReplacement(mapping) && !nullLiteralReplacements(
                    mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(
                    mapping.getFragment2()))) {
                    AbstractCodeFragment fragment = mapping.getFragment2();
                    if (fragment instanceof StatementObject) {
                        StatementObject statement = (StatementObject) fragment;
                        if (!leaves2.contains(statement)) {
                            leaves2.add(statement);
                            addedLeaves2.add(statement);
                        }
                    }
                }
            }
            resetNodes(leaves1);
            //replace parameters with arguments in leaves1
            if (!parameterToArgumentMap.isEmpty()) {
                //check for temporary variables that the argument might be assigned to
                for (StatementObject leave2 : leaves2) {
                    List<VariableDeclaration> variableDeclarations = leave2.getVariableDeclarations();
                    for (VariableDeclaration variableDeclaration : variableDeclarations) {
                        for (String parameter : parameterToArgumentMap.keySet()) {
                            String argument = parameterToArgumentMap.get(parameter);
                            if (variableDeclaration.getInitializer() != null && argument.equals(
                                variableDeclaration.getInitializer().toString())) {
                                parameterToArgumentMap.put(parameter, variableDeclaration.getVariableName());
                            }
                        }
                    }
                }
                for (StatementObject leave1 : leaves1) {
                    leave1.replaceParametersWithArguments(parameterToArgumentMap);
                }
            }
            //compare leaves from T1 with leaves from T2
            processLeaves(leaves1, leaves2, parameterToArgumentMap);

            List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
            innerNodes1.remove(composite1);
            List<CompositeStatementObject> innerNodes2 = operationBodyMapper.getNonMappedInnerNodesT2();
            //adding innerNodes that were mapped with replacements or are inexact matches
            Set<CompositeStatementObject> addedInnerNodes2 = new LinkedHashSet<>();
            for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
                if (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(
                    mapping.getFragment2())) {
                    AbstractCodeFragment fragment = mapping.getFragment2();
                    if (fragment instanceof CompositeStatementObject) {
                        CompositeStatementObject statement = (CompositeStatementObject) fragment;
                        if (!innerNodes2.contains(statement)) {
                            innerNodes2.add(statement);
                            addedInnerNodes2.add(statement);
                        }
                    }
                }
            }
            resetNodes(innerNodes1);
            //replace parameters with arguments in innerNodes1
            if (!parameterToArgumentMap.isEmpty()) {
                for (CompositeStatementObject innerNode1 : innerNodes1) {
                    innerNode1.replaceParametersWithArguments(parameterToArgumentMap);
                }
            }
            //compare inner nodes from T1 with inner nodes from T2
            processInnerNodes(innerNodes1, innerNodes2, parameterToArgumentMap);

            //match expressions in inner nodes from T2 with leaves from T1
            List<AbstractExpression> expressionsT2 = new ArrayList<>();
            for (CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
                expressionsT2.addAll(composite.getExpressions());
            }
            processLeaves(leaves1, expressionsT2, parameterToArgumentMap);

            //remove the leaves that were mapped with replacement, if they are not mapped again for a second time
            leaves2.removeAll(addedLeaves2);
            //remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
            innerNodes2.removeAll(addedInnerNodes2);
            nonMappedLeavesT1.addAll(leaves1);
            nonMappedLeavesT2.addAll(leaves2);
            nonMappedInnerNodesT1.addAll(innerNodes1);
            nonMappedInnerNodesT2.addAll(innerNodes2);

            for (StatementObject statement : getNonMappedLeavesT2()) {
                temporaryVariableAssignment(statement, nonMappedLeavesT2);
            }
            for (StatementObject statement : getNonMappedLeavesT1()) {
                inlinedVariableAssignment(statement, nonMappedLeavesT2);
            }
        }
    }

    private boolean returnWithVariableReplacement(AbstractCodeMapping mapping) {
        if (mapping.getReplacements().size() == 1) {
            Replacement r = mapping.getReplacements().iterator().next();
            if (r.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                String fragment1 = mapping.getFragment1().getString();
                String fragment2 = mapping.getFragment2().getString();
                return fragment1.equals("return " + r.getBefore() + "\n") && fragment2.equals(
                    "return " + r.getAfter() + "\n");
            }
        }
        return false;
    }

    private boolean nullLiteralReplacements(AbstractCodeMapping mapping) {
        int numberOfReplacements = mapping.getReplacements().size();
        int nullLiteralReplacements = 0;
        int methodInvocationReplacementsToIgnore = 0;
        int variableNameReplacementsToIgnore = 0;
        for (Replacement replacement : mapping.getReplacements()) {
            if (replacement.getType().equals(Replacement.ReplacementType.NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION) ||
                replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL) ||
                (replacement.getType().equals(
                    Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_VARIABLE) && (replacement.getBefore().equals(
                    "Null") || replacement.getAfter().equals("Null")))) {
                nullLiteralReplacements++;
            } else if (replacement instanceof MethodInvocationReplacement) {
                MethodInvocationReplacement invocationReplacement = (MethodInvocationReplacement) replacement;
                OperationInvocation invokedOperationBefore = invocationReplacement.getInvokedOperationBefore();
                OperationInvocation invokedOperationAfter = invocationReplacement.getInvokedOperationAfter();
                if (invokedOperationBefore.getName().equals(invokedOperationAfter.getName()) &&
                    invokedOperationBefore.getArguments().size() == invokedOperationAfter.getArguments().size()) {
                    methodInvocationReplacementsToIgnore++;
                }
            } else if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                variableNameReplacementsToIgnore++;
            }
        }
        return nullLiteralReplacements > 0 && numberOfReplacements == nullLiteralReplacements + methodInvocationReplacementsToIgnore + variableNameReplacementsToIgnore;
    }

    private void resetNodes(List<? extends AbstractCodeFragment> nodes) {
        for (AbstractCodeFragment node : nodes) {
            node.resetArgumentization();
        }
    }

    public void processLeaves(List<? extends AbstractCodeFragment> leaves1,
                              List<? extends AbstractCodeFragment> leaves2,
                              Map<String, String> parameterToArgumentMap) throws RefactoringMinerTimedOutException {
        List<TreeSet<LeafMapping>> postponedMappingSets = new ArrayList<>();
        if (leaves1.size() <= leaves2.size()) {
            //exact string+depth matching - leaf nodes
            for (ListIterator<? extends AbstractCodeFragment> leafIterator1 =
                 leaves1.listIterator(); leafIterator1.hasNext(); ) {
                AbstractCodeFragment leaf1 = leafIterator1.next();
                TreeSet<LeafMapping> mappingSet = new TreeSet<>();
                for (ListIterator<? extends AbstractCodeFragment> leafIterator2 =
                     leaves2.listIterator(); leafIterator2.hasNext(); ) {
                    AbstractCodeFragment leaf2 = leafIterator2.next();
                    String argumentizedString1 = preprocessInput1(leaf1, leaf2);
                    String argumentizedString2 = preprocessInput2(leaf1, leaf2);
                    if ((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(
                        argumentizedString2)) && leaf1.getDepth() == leaf2.getDepth()) {
                        LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
                        mappingSet.add(mapping);
                    }
                }
                if (!mappingSet.isEmpty()) {
                    LeafMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    leaves2.remove(minStatementMapping.getFragment2());
                    leafIterator1.remove();
                }
            }

            //exact string matching - leaf nodes - finds moves to another level
            for (ListIterator<? extends AbstractCodeFragment> leafIterator1 =
                 leaves1.listIterator(); leafIterator1.hasNext(); ) {
                AbstractCodeFragment leaf1 = leafIterator1.next();
                TreeSet<LeafMapping> mappingSet = new TreeSet<>();
                for (ListIterator<? extends AbstractCodeFragment> leafIterator2 =
                     leaves2.listIterator(); leafIterator2.hasNext(); ) {
                    AbstractCodeFragment leaf2 = leafIterator2.next();
                    String argumentizedString1 = preprocessInput1(leaf1, leaf2);
                    String argumentizedString2 = preprocessInput2(leaf1, leaf2);
                    if ((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(
                        argumentizedString2))) {
                        LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
                        mappingSet.add(mapping);
                    }
                }
                if (!mappingSet.isEmpty()) {
                    LeafMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    leaves2.remove(minStatementMapping.getFragment2());
                    leafIterator1.remove();
                }
            }

            // exact matching with variable renames
            for (ListIterator<? extends AbstractCodeFragment> leafIterator1 =
                 leaves1.listIterator(); leafIterator1.hasNext(); ) {
                AbstractCodeFragment leaf1 = leafIterator1.next();
                TreeSet<LeafMapping> mappingSet = new TreeSet<>();
                for (ListIterator<? extends AbstractCodeFragment> leafIterator2 =
                     leaves2.listIterator(); leafIterator2.hasNext(); ) {
                    AbstractCodeFragment leaf2 = leafIterator2.next();

                    ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf2, leaves1, leaves2);
                    Set<Replacement> replacements =
                        findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap, replacementInfo);
                    if (replacements != null) {
                        LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
                        mapping.addReplacements(replacements);
                        for (AbstractCodeFragment leaf : leaves2) {
                            if (leaf.equals(leaf2)) {
                                break;
                            }
                            UMLClassBaseDiff classDiff = this.classDiff != null ? this.classDiff :
                                parentMapper != null ? parentMapper.classDiff : null;
                            mapping.temporaryVariableAssignment(leaf, leaves2, refactorings, classDiff);
                            if (mapping.isIdenticalWithExtractedVariable()) {
                                break;
                            }
                        }
                        for (AbstractCodeFragment leaf : leaves1) {
                            if (leaf.equals(leaf1)) {
                                break;
                            }
                            mapping.inlinedVariableAssignment(leaf, leaves2, refactorings);
                            if (mapping.isIdenticalWithInlinedVariable()) {
                                break;
                            }
                        }
                        mappingSet.add(mapping);
                    }
                }
                if (!mappingSet.isEmpty()) {
                    AbstractMap.SimpleEntry<CompositeStatementObject, CompositeStatementObject> switchParentEntry =
                        null;
                    if (variableDeclarationMappingsWithSameReplacementTypes(mappingSet)) {
                        //postpone mapping
                        postponedMappingSets.add(mappingSet);
/*                    } TODO: Analyze `when` expression instead of switch-case
                        else if ((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
                        LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
                        mappings.add(bestMapping);
                        leaves2.remove(bestMapping.getFragment1());
                        leafIterator1.remove();*/
                    } else {
                        LeafMapping minStatementMapping = mappingSet.first();
                        mappings.add(minStatementMapping);
                        leaves2.remove(minStatementMapping.getFragment2());
                        leafIterator1.remove();
                    }
                }
            }
        } else {
            //exact string+depth matching - leaf nodes
            for (ListIterator<? extends AbstractCodeFragment> leafIterator2 =
                 leaves2.listIterator(); leafIterator2.hasNext(); ) {
                AbstractCodeFragment leaf2 = leafIterator2.next();
                TreeSet<LeafMapping> mappingSet = new TreeSet<>();
                for (ListIterator<? extends AbstractCodeFragment> leafIterator1 =
                     leaves1.listIterator(); leafIterator1.hasNext(); ) {
                    AbstractCodeFragment leaf1 = leafIterator1.next();
                    String argumentizedString1 = preprocessInput1(leaf1, leaf2);
                    String argumentizedString2 = preprocessInput2(leaf1, leaf2);
                    if ((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(
                        argumentizedString2)) && leaf1.getDepth() == leaf2.getDepth()) {
                        LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
                        mappingSet.add(mapping);
                    }
                }
                if (!mappingSet.isEmpty()) {
                    LeafMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    leaves1.remove(minStatementMapping.getFragment1());
                    leafIterator2.remove();
                }
            }

            //exact string matching - leaf nodes - finds moves to another level
            for (ListIterator<? extends AbstractCodeFragment> leafIterator2 =
                 leaves2.listIterator(); leafIterator2.hasNext(); ) {
                AbstractCodeFragment leaf2 = leafIterator2.next();
                TreeSet<LeafMapping> mappingSet = new TreeSet<>();
                for (ListIterator<? extends AbstractCodeFragment> leafIterator1 =
                     leaves1.listIterator(); leafIterator1.hasNext(); ) {
                    AbstractCodeFragment leaf1 = leafIterator1.next();
                    String argumentizedString1 = preprocessInput1(leaf1, leaf2);
                    String argumentizedString2 = preprocessInput2(leaf1, leaf2);
                    if ((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(
                        argumentizedString2))) {
                        LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
                        mappingSet.add(mapping);
                    }
                }
                if (!mappingSet.isEmpty()) {
                    LeafMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    leaves1.remove(minStatementMapping.getFragment1());
                    leafIterator2.remove();
                }
            }

            // exact matching with variable renames
            for (ListIterator<? extends AbstractCodeFragment> leafIterator2 =
                 leaves2.listIterator(); leafIterator2.hasNext(); ) {
                AbstractCodeFragment leaf2 = leafIterator2.next();
                TreeSet<LeafMapping> mappingSet = new TreeSet<>();
                for (ListIterator<? extends AbstractCodeFragment> leafIterator1 =
                     leaves1.listIterator(); leafIterator1.hasNext(); ) {
                    AbstractCodeFragment leaf1 = leafIterator1.next();

                    ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf2, leaves1, leaves2);
                    Set<Replacement> replacements =
                        findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap, replacementInfo);
                    if (replacements != null) {
                        LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
                        mapping.addReplacements(replacements);
                        for (AbstractCodeFragment leaf : leaves2) {
                            if (leaf.equals(leaf2)) {
                                break;
                            }
                            UMLClassBaseDiff classDiff = this.classDiff != null ? this.classDiff :
                                parentMapper != null ? parentMapper.classDiff : null;
                            mapping.temporaryVariableAssignment(leaf, leaves2, refactorings, classDiff);
                            if (mapping.isIdenticalWithExtractedVariable()) {
                                break;
                            }
                        }
                        for (AbstractCodeFragment leaf : leaves1) {
                            if (leaf.equals(leaf1)) {
                                break;
                            }
                            mapping.inlinedVariableAssignment(leaf, leaves2, refactorings);
                            if (mapping.isIdenticalWithInlinedVariable()) {
                                break;
                            }
                        }
                        mappingSet.add(mapping);
                    }
                }
                if (!mappingSet.isEmpty()) {
                    AbstractMap.SimpleEntry<CompositeStatementObject, CompositeStatementObject> switchParentEntry =
                        null;
                    if (variableDeclarationMappingsWithSameReplacementTypes(mappingSet)) {
                        //postpone mapping
                        postponedMappingSets.add(mappingSet);
                 /* TODO: Analyze `when` expression instead of switch-case
                     } else if ((switchParentEntry =
                     multipleMappingsUnderTheSameSwitch
                     (mappingSet)) != null) {
                        LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
                        mappings.add(bestMapping);
                        leaves1.remove(bestMapping.getFragment1());
                        leafIterator2.remove();*/
                    } else {
                        LeafMapping minStatementMapping = mappingSet.first();
                        mappings.add(minStatementMapping);
                        leaves1.remove(minStatementMapping.getFragment1());
                        leafIterator2.remove();
                    }
                }
            }
        }
        for (TreeSet<LeafMapping> postponed : postponedMappingSets) {
            Set<LeafMapping> mappingsToBeAdded = new LinkedHashSet<>();
            for (LeafMapping variableDeclarationMapping : postponed) {
                for (AbstractCodeMapping previousMapping : this.mappings) {
                    Set<Replacement> intersection = variableDeclarationMapping.commonReplacements(previousMapping);
                    if (!intersection.isEmpty()) {
                        for (Replacement commonReplacement : intersection) {
                            if (commonReplacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) &&
                                variableDeclarationMapping.getFragment1().getVariableDeclaration(
                                    commonReplacement.getBefore()) != null &&
                                variableDeclarationMapping.getFragment2().getVariableDeclaration(
                                    commonReplacement.getAfter()) != null) {
                                mappingsToBeAdded.add(variableDeclarationMapping);
                            }
                        }
                    }
                }
            }
            if (mappingsToBeAdded.size() == 1) {
                LeafMapping minStatementMapping = mappingsToBeAdded.iterator().next();
                this.mappings.add(minStatementMapping);
                leaves1.remove(minStatementMapping.getFragment1());
                leaves2.remove(minStatementMapping.getFragment2());
            } else {
                LeafMapping minStatementMapping = postponed.first();
                this.mappings.add(minStatementMapping);
                leaves1.remove(minStatementMapping.getFragment1());
                leaves2.remove(minStatementMapping.getFragment2());
            }
        }
    }

    public UMLOperation getOperation1() {
        return operation1;
    }

    public UMLOperation getOperation2() {
        return operation2;
    }

    public List<UMLOperationBodyMapper> getChildMappers() {
        return childMappers;
    }

    public UMLOperationBodyMapper getParentMapper() {
        return parentMapper;
    }

    public UMLOperation getCallSiteOperation() {
        return callSiteOperation;
    }

    private boolean variableDeclarationMappingsWithSameReplacementTypes(Set<LeafMapping> mappingSet) {
        if (mappingSet.size() > 1) {
            Set<LeafMapping> variableDeclarationMappings = new LinkedHashSet<>();
            for (LeafMapping mapping : mappingSet) {
                if (mapping.getFragment1().getVariableDeclarations().size() > 0 &&
                    mapping.getFragment2().getVariableDeclarations().size() > 0) {
                    variableDeclarationMappings.add(mapping);
                }
            }
            if (variableDeclarationMappings.size() == mappingSet.size()) {
                Set<Replacement.ReplacementType> replacementTypes = null;
                Set<LeafMapping> mappingsWithSameReplacementTypes = new LinkedHashSet<>();
                for (LeafMapping mapping : variableDeclarationMappings) {
                    if (replacementTypes == null) {
                        replacementTypes = mapping.getReplacementTypes();
                        mappingsWithSameReplacementTypes.add(mapping);
                    } else if (mapping.getReplacementTypes().equals(replacementTypes)) {
                        mappingsWithSameReplacementTypes.add(mapping);
                    } else if (mapping.getReplacementTypes().containsAll(replacementTypes) ||
                        replacementTypes.containsAll(mapping.getReplacementTypes())) {
                        OperationInvocation invocation1 = mapping.getFragment1().invocationCoveringEntireFragment();
                        OperationInvocation invocation2 = mapping.getFragment2().invocationCoveringEntireFragment();
                        if (invocation1 != null && invocation2 != null) {
                            for (Replacement replacement : mapping.getReplacements()) {
                                if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                                    if (invocation1.getName().equals(replacement.getBefore()) &&
                                        invocation2.getName().equals(replacement.getAfter())) {
                                        mappingsWithSameReplacementTypes.add(mapping);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                return mappingsWithSameReplacementTypes.size() == mappingSet.size();
            }
        }
        return false;
    }

    private LeafMapping createLeafMapping(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2,
                                          Map<String, String> parameterToArgumentMap) {
        UMLOperation operation1 = codeFragmentOperationMap1.getOrDefault(leaf1, this.operation1);
        UMLOperation operation2 = codeFragmentOperationMap2.getOrDefault(leaf2, this.operation2);
        LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
        for (String key : parameterToArgumentMap.keySet()) {
            String value = parameterToArgumentMap.get(key);
            if (!key.equals(value) && ReplacementUtil.contains(leaf2.getString(), key) &&
                ReplacementUtil.contains(leaf1.getString(), value)) {
                mapping.addReplacement(new Replacement(value, key, Replacement.ReplacementType.VARIABLE_NAME));
            }
        }
        return mapping;
    }

    private ReplacementInfo initializeReplacementInfo(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2,
                                                      List<? extends AbstractCodeFragment> leaves1,
                                                      List<? extends AbstractCodeFragment> leaves2) {
        List<? extends AbstractCodeFragment> l1 = new ArrayList<AbstractCodeFragment>(leaves1);
        l1.remove(leaf1);
        List<? extends AbstractCodeFragment> l2 = new ArrayList<AbstractCodeFragment>(leaves2);
        l2.remove(leaf2);
        return new ReplacementInfo(
            preprocessInput1(leaf1, leaf2),
            preprocessInput2(leaf1, leaf2),
            l1, l2);
    }

    public void processInnerNodes(List<CompositeStatementObject> innerNodes1,
                                  List<CompositeStatementObject> innerNodes2,
                                  Map<String, String> parameterToArgumentMap) throws RefactoringMinerTimedOutException {
        List<UMLOperation> removedOperations = classDiff != null ? classDiff.getRemovedOperations() : new ArrayList<>();
        List<UMLOperation> addedOperations = classDiff != null ? classDiff.getAddedOperations() : new ArrayList<>();
        if (innerNodes1.size() <= innerNodes2.size()) {
            //exact string+depth matching - inner nodes
            for (ListIterator<CompositeStatementObject> innerNodeIterator1 =
                 innerNodes1.listIterator(); innerNodeIterator1.hasNext(); ) {
                CompositeStatementObject statement1 = innerNodeIterator1.next();
                TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
                innerNodes2.forEach(statement2 -> {
                    double score = computeScore(statement1, statement2, removedOperations, addedOperations);
                    if ((statement1.getString().equals(statement2.getString()) ||
                        statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) &&
                        statement1.getDepth() == statement2.getDepth() &&
                        (score > 0 || Math.max(statement1.getStatements().size(),
                            statement2.getStatements().size()) == 0)) {
                        CompositeStatementObjectMapping mapping =
                            createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
                        mappingSet.add(mapping);
                    }
                });
                if (!mappingSet.isEmpty()) {
                    CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    innerNodes2.remove(minStatementMapping.getFragment2());
                    innerNodeIterator1.remove();
                }
            }

            //exact string matching - inner nodes - finds moves to another level
            for (ListIterator<CompositeStatementObject> innerNodeIterator1 =
                 innerNodes1.listIterator(); innerNodeIterator1.hasNext(); ) {
                CompositeStatementObject statement1 = innerNodeIterator1.next();
                TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
                innerNodes2.forEach(statement2 -> {
                    double score = computeScore(statement1, statement2, removedOperations, addedOperations);
                    if ((statement1.getString().equals(statement2.getString()) ||
                        statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) &&
                        (score > 0 || Math.max(statement1.getStatements().size(),
                            statement2.getStatements().size()) == 0)) {
                        CompositeStatementObjectMapping mapping =
                            createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
                        mappingSet.add(mapping);
                    }
                });
                if (!mappingSet.isEmpty()) {
                    CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    innerNodes2.remove(minStatementMapping.getFragment2());
                    innerNodeIterator1.remove();
                }
            }

            // exact matching - inner nodes - with variable renames
            for (ListIterator<CompositeStatementObject> innerNodeIterator1 =
                 innerNodes1.listIterator(); innerNodeIterator1.hasNext(); ) {
                CompositeStatementObject statement1 = innerNodeIterator1.next();
                TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
                for (CompositeStatementObject statement2 : innerNodes2) {
                    ReplacementInfo replacementInfo =
                        initializeReplacementInfo(statement1, statement2, innerNodes1, innerNodes2);
                    Set<Replacement> replacements =
                        findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap,
                            replacementInfo);

                    double score = computeScore(statement1, statement2, removedOperations, addedOperations);
                    if (score == 0 && replacements != null && replacements.size() == 1 &&
                        (replacements.iterator().next().getType().equals(Replacement.ReplacementType.INFIX_OPERATOR) ||
                            replacements.iterator().next().getType().equals(
                                Replacement.ReplacementType.INVERT_CONDITIONAL))) {
                        //special handling when there is only an infix operator or invert conditional replacement, but no children mapped
                        score = 1;
                    }
                    if (replacements != null &&
                        (score > 0 || Math.max(statement1.getStatements().size(),
                            statement2.getStatements().size()) == 0)) {
                        CompositeStatementObjectMapping mapping =
                            createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
                        mapping.addReplacements(replacements);
                        mappingSet.add(mapping);
                    }
                }
                if (!mappingSet.isEmpty()) {
                    CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    innerNodes2.remove(minStatementMapping.getFragment2());
                    innerNodeIterator1.remove();
                }
            }
        } else {
            //exact string+depth matching - inner nodes
            for (ListIterator<CompositeStatementObject> innerNodeIterator2 =
                 innerNodes2.listIterator(); innerNodeIterator2.hasNext(); ) {
                CompositeStatementObject statement2 = innerNodeIterator2.next();
                TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
                innerNodes1.forEach(statement1 -> {
                    double score = computeScore(statement1, statement2, removedOperations, addedOperations);
                    if ((statement1.getString().equals(statement2.getString()) ||
                        statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) &&
                        statement1.getDepth() == statement2.getDepth() &&
                        (score > 0 || Math.max(statement1.getStatements().size(),
                            statement2.getStatements().size()) == 0)) {
                        CompositeStatementObjectMapping mapping =
                            createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
                        mappingSet.add(mapping);
                    }
                });
                if (!mappingSet.isEmpty()) {
                    CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    innerNodes1.remove(minStatementMapping.getFragment1());
                    innerNodeIterator2.remove();
                }
            }

            //exact string matching - inner nodes - finds moves to another level
            for (ListIterator<CompositeStatementObject> innerNodeIterator2 =
                 innerNodes2.listIterator(); innerNodeIterator2.hasNext(); ) {
                CompositeStatementObject statement2 = innerNodeIterator2.next();
                TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
                innerNodes1.forEach(statement1 -> {
                    double score = computeScore(statement1, statement2, removedOperations, addedOperations);
                    if ((statement1.getString().equals(statement2.getString()) ||
                        statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) &&
                        (score > 0 || Math.max(statement1.getStatements().size(),
                            statement2.getStatements().size()) == 0)) {
                        CompositeStatementObjectMapping mapping =
                            createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
                        mappingSet.add(mapping);
                    }
                });
                if (!mappingSet.isEmpty()) {
                    CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    innerNodes1.remove(minStatementMapping.getFragment1());
                    innerNodeIterator2.remove();
                }
            }

            // exact matching - inner nodes - with variable renames
            for (ListIterator<CompositeStatementObject> innerNodeIterator2 =
                 innerNodes2.listIterator(); innerNodeIterator2.hasNext(); ) {
                CompositeStatementObject statement2 = innerNodeIterator2.next();
                TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
                for (CompositeStatementObject statement1 : innerNodes1) {
                    ReplacementInfo replacementInfo =
                        initializeReplacementInfo(statement1, statement2, innerNodes1, innerNodes2);
                    Set<Replacement> replacements =
                        findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap,
                            replacementInfo);

                    double score = computeScore(statement1, statement2, removedOperations, addedOperations);
                    if (score == 0 && replacements != null && replacements.size() == 1 &&
                        (replacements.iterator().next().getType().equals(Replacement.ReplacementType.INFIX_OPERATOR) ||
                            replacements.iterator().next().getType().equals(
                                Replacement.ReplacementType.INVERT_CONDITIONAL))) {
                        //special handling when there is only an infix operator or invert conditional replacement, but no children mapped
                        score = 1;
                    }
                    if (replacements != null &&
                        (score > 0 || Math.max(statement1.getStatements().size(),
                            statement2.getStatements().size()) == 0)) {
                        CompositeStatementObjectMapping mapping =
                            createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
                        mapping.addReplacements(replacements);
                        mappingSet.add(mapping);
                    }
                }
                if (!mappingSet.isEmpty()) {
                    CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
                    mappings.add(minStatementMapping);
                    innerNodes1.remove(minStatementMapping.getFragment1());
                    innerNodeIterator2.remove();
                }
            }
        }
    }

    private CompositeStatementObjectMapping createCompositeMapping(CompositeStatementObject statement1,
                                                                   CompositeStatementObject statement2,
                                                                   Map<String, String> parameterToArgumentMap,
                                                                   double score) {
        UMLOperation operation1 = codeFragmentOperationMap1.getOrDefault(statement1, this.operation1);
        UMLOperation operation2 = codeFragmentOperationMap2.getOrDefault(statement2, this.operation2);
        CompositeStatementObjectMapping mapping =
            new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
        for (String key : parameterToArgumentMap.keySet()) {
            String value = parameterToArgumentMap.get(key);
            if (!key.equals(value) && ReplacementUtil.contains(statement2.getString(), key) &&
                ReplacementUtil.contains(statement1.getString(), value)) {
                mapping.addReplacement(new Replacement(value, key, Replacement.ReplacementType.VARIABLE_NAME));
            }
        }
        return mapping;
    }

    private double computeScore(CompositeStatementObject statement1, CompositeStatementObject statement2,
                                List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
/*    TODO:    if (statement1 instanceof TryStatementObject && statement2 instanceof TryStatementObject) {
            return compositeChildMatchingScore((TryStatementObject) statement1, (TryStatementObject) statement2, mappings, removedOperations, addedOperations);
        }
        return compositeChildMatchingScore(statement1, statement2, mappings, removedOperations, addedOperations);*/
        return compositeChildMatchingScore(statement1, statement2, mappings, removedOperations, addedOperations);
    }

    private boolean matchesOperation(OperationInvocation invocation,
                                     List<UMLOperation> operations,
                                     Map<String, UMLType> variableTypeMap) {
        for (UMLOperation operation : operations) {
            if (invocation.matchesOperation(operation, variableTypeMap, modelDiff))
                return true;
        }
        return false;
    }

    private double compositeChildMatchingScore(CompositeStatementObject comp1,
                                               CompositeStatementObject comp2,
                                               Set<AbstractCodeMapping> mappings,
                                               List<UMLOperation> removedOperations,
                                               List<UMLOperation> addedOperations) {
        List<AbstractStatement> compStatements1 = comp1.getStatements();
        List<AbstractStatement> compStatements2 = comp2.getStatements();
        int childrenSize1 = compStatements1.size();
        int childrenSize2 = compStatements2.size();

        if (parentMapper != null && comp1.getLocationInfo().getCodeElementType().equals(
            comp2.getLocationInfo().getCodeElementType()) &&
            childrenSize1 == 1 && childrenSize2 == 1 && !comp1.getString().equals("{") &&
            !comp2.getString().equals("{")) {
            if (compStatements1.get(0).getString().equals("{") && !compStatements2.get(0).getString().equals("{")) {
                CompositeStatementObject block = (CompositeStatementObject) compStatements1.get(0);
                compStatements1.addAll(block.getStatements());
            }
            if (!compStatements1.get(0).getString().equals("{") && compStatements2.get(0).getString().equals("{")) {
                CompositeStatementObject block = (CompositeStatementObject) compStatements2.get(0);
                compStatements2.addAll(block.getStatements());
            }
        }
        int mappedChildrenSize = 0;
        for (AbstractCodeMapping mapping : mappings) {
            if (compStatements1.contains(mapping.getFragment1()) && compStatements2.contains(mapping.getFragment2())) {
                mappedChildrenSize++;
            }
        }
        if (mappedChildrenSize == 0) {
            List<StatementObject> leaves1 = comp1.getLeaves();
            List<StatementObject> leaves2 = comp2.getLeaves();
            int leaveSize1 = leaves1.size();
            int leaveSize2 = leaves2.size();
            int mappedLeavesSize = 0;
            for (AbstractCodeMapping mapping : mappings) {
                if (leaves1.contains(mapping.getFragment1()) && leaves2.contains(mapping.getFragment2())) {
                    mappedLeavesSize++;
                }
            }
            if (mappedLeavesSize == 0) {
                //check for possible extract or inline
                if (leaveSize2 <= 2) {
                    for (StatementObject leaf2 : leaves2) {
                        OperationInvocation invocation = leaf2.invocationCoveringEntireFragment();
                        if (invocation != null && matchesOperation(invocation, addedOperations,
                            operation2.variableTypeMap())) {
                            mappedLeavesSize++;
                        }
                    }
                } else if (leaveSize1 <= 2) {
                    for (StatementObject leaf1 : leaves1) {
                        OperationInvocation invocation = leaf1.invocationCoveringEntireFragment();
                        if (invocation != null && matchesOperation(invocation, removedOperations,
                            operation1.variableTypeMap())) {
                            mappedLeavesSize++;
                        }
                    }
                }
                if (leaveSize1 == 1 && leaveSize2 == 1 && leaves1.get(0).getString().equals(
                    "continue;\n") && leaves2.get(0).getString().equals("return null\n")) {
                    mappedLeavesSize++;
                }
            }
            int max = Math.max(leaveSize1, leaveSize2);
            if (max == 0)
                return 0;
            else
                return (double) mappedLeavesSize / (double) max;
        }

        int max = Math.max(childrenSize1, childrenSize2);
        if (max == 0)
            return 0;
        else
            return (double) mappedChildrenSize / (double) max;
    }

    private void temporaryVariableAssignment(StatementObject statement, List<StatementObject> nonMappedLeavesT2) {
        for (AbstractCodeMapping mapping : getMappings()) {
            UMLClassBaseDiff classDiff =
                this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
            mapping.temporaryVariableAssignment(statement, nonMappedLeavesT2, refactorings, classDiff);
        }
    }

    private VariableDeclaration declarationWithArrayInitializer(List<VariableDeclaration> declarations) {
        for (VariableDeclaration declaration : declarations) {
            AbstractExpression initializer = declaration.getInitializer();
            if (initializer != null && initializer.getString().startsWith("{") &&
                initializer.getString().endsWith("}")) {
                return declaration;
            }
        }
        return null;
    }

    private boolean nonMatchedStatementUsesVariableInArgument(List<? extends AbstractCodeFragment> statements,
                                                              String variable, String otherArgument) {
        for (AbstractCodeFragment statement : statements) {
            OperationInvocation invocation = statement.invocationCoveringEntireFragment();
            if (invocation != null) {
                for (String argument : invocation.getArguments()) {
                    String argumentNoWhiteSpace = argument.replaceAll("\\s", "");
                    if (argument.contains(variable) && !argument.equals(variable) &&
                        !argumentNoWhiteSpace.contains("+" + variable + "+") &&
                        !argumentNoWhiteSpace.contains(variable + "+") &&
                        !argumentNoWhiteSpace.contains("+" + variable) &&
                        !argument.equals(otherArgument)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void replaceVariablesWithArguments(Set<String> variables, Map<String, String> parameterToArgumentMap) {
        for (String parameter : parameterToArgumentMap.keySet()) {
            String argument = parameterToArgumentMap.get(parameter);
            if (variables.contains(parameter)) {
                variables.add(argument);
                if (argument.contains("(") && argument.contains(")")) {
                    int indexOfOpeningParenthesis = argument.indexOf("(");
                    int indexOfClosingParenthesis = argument.lastIndexOf(")");
                    boolean openingParenthesisInsideSingleQuotes =
                        isInsideSingleQuotes(argument, indexOfOpeningParenthesis);
                    boolean closingParenthesisInsideSingleQuotes =
                        isInsideSingleQuotes(argument, indexOfClosingParenthesis);
                    boolean openingParenthesisInsideDoubleQuotes =
                        isInsideDoubleQuotes(argument, indexOfOpeningParenthesis);
                    boolean closingParenthesisIndideDoubleQuotes =
                        isInsideDoubleQuotes(argument, indexOfClosingParenthesis);
                    if (indexOfOpeningParenthesis < indexOfClosingParenthesis &&
                        !openingParenthesisInsideSingleQuotes && !closingParenthesisInsideSingleQuotes &&
                        !openingParenthesisInsideDoubleQuotes && !closingParenthesisIndideDoubleQuotes) {
                        String arguments = argument.substring(indexOfOpeningParenthesis + 1, indexOfClosingParenthesis);
                        if (!arguments.isEmpty() && !arguments.contains(",") && !arguments.contains("(") &&
                            !arguments.contains(")")) {
                            variables.add(arguments);
                        }
                    }
                }
            }
        }
    }

    private static boolean isInsideSingleQuotes(String argument, int indexOfChar) {
        if (indexOfChar > 0 && indexOfChar < argument.length() - 1) {
            return argument.charAt(indexOfChar - 1) == '\'' &&
                argument.charAt(indexOfChar + 1) == '\'';
        }
        return false;
    }

    private static boolean isInsideDoubleQuotes(String argument, int indexOfChar) {
        Matcher m = DOUBLE_QUOTES.matcher(argument);
        while (m.find()) {
            if (m.group(1) != null) {
                if (indexOfChar > m.start() && indexOfChar < m.end()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeCommonElements(Set<String> strings1, Set<String> strings2) {
        Set<String> intersection = new LinkedHashSet<>(strings1);
        intersection.retainAll(strings2);
        strings1.removeAll(intersection);
        strings2.removeAll(intersection);
    }

    private boolean argumentsWithIdenticalMethodCalls(Set<String> arguments1, Set<String> arguments2,
                                                      Set<String> variables1, Set<String> variables2) {
        int identicalMethodCalls = 0;
        if (arguments1.size() == arguments2.size()) {
            Iterator<String> it1 = arguments1.iterator();
            Iterator<String> it2 = arguments2.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                String arg1 = it1.next();
                String arg2 = it2.next();
                if (arg1.contains("(") && arg2.contains("(") && arg1.contains(")") && arg2.contains(")")) {
                    int indexOfOpeningParenthesis1 = arg1.indexOf("(");
                    int indexOfClosingParenthesis1 = arg1.indexOf(")");
                    boolean openingParenthesisInsideSingleQuotes1 =
                        isInsideSingleQuotes(arg1, indexOfOpeningParenthesis1);
                    boolean openingParenthesisInsideDoubleQuotes1 =
                        isInsideDoubleQuotes(arg1, indexOfOpeningParenthesis1);
                    boolean closingParenthesisInsideSingleQuotes1 =
                        isInsideSingleQuotes(arg1, indexOfClosingParenthesis1);
                    boolean closingParenthesisInsideDoubleQuotes1 =
                        isInsideDoubleQuotes(arg1, indexOfClosingParenthesis1);
                    int indexOfOpeningParenthesis2 = arg2.indexOf("(");
                    int indexOfClosingParenthesis2 = arg2.indexOf(")");
                    boolean openingParenthesisInsideSingleQuotes2 =
                        isInsideSingleQuotes(arg2, indexOfOpeningParenthesis2);
                    boolean openingParenthesisInsideDoubleQuotes2 =
                        isInsideDoubleQuotes(arg2, indexOfOpeningParenthesis2);
                    boolean closingParenthesisInsideSingleQuotes2 =
                        isInsideSingleQuotes(arg2, indexOfClosingParenthesis2);
                    boolean closingParenthesisInsideDoubleQuotes2 =
                        isInsideDoubleQuotes(arg2, indexOfClosingParenthesis2);
                    if (!openingParenthesisInsideSingleQuotes1 && !closingParenthesisInsideSingleQuotes1 &&
                        !openingParenthesisInsideDoubleQuotes1 && !closingParenthesisInsideDoubleQuotes1 &&
                        !openingParenthesisInsideSingleQuotes2 && !closingParenthesisInsideSingleQuotes2 &&
                        !openingParenthesisInsideDoubleQuotes2 && !closingParenthesisInsideDoubleQuotes2) {
                        String s1 = arg1.substring(0, indexOfOpeningParenthesis1);
                        String s2 = arg2.substring(0, indexOfOpeningParenthesis2);
                        if (s1.equals(s2) && s1.length() > 0) {
                            String args1 = arg1.substring(indexOfOpeningParenthesis1 + 1, indexOfClosingParenthesis1);
                            String args2 = arg2.substring(indexOfOpeningParenthesis2 + 1, indexOfClosingParenthesis2);
                            if (variables1.contains(args1) && variables2.contains(args2)) {
                                identicalMethodCalls++;
                            }
                        }
                    }
                }
            }
        }
        return identicalMethodCalls == arguments1.size() && arguments1.size() > 0;
    }

    private void removeCommonTypes(Set<String> strings1,
                                   Set<String> strings2,
                                   List<String> types1,
                                   List<String> types2) {
        if (types1.size() == types2.size()) {
            Set<String> removeFromIntersection = new LinkedHashSet<>();
            for (int i = 0; i < types1.size(); i++) {
                String type1 = types1.get(i);
                String type2 = types2.get(i);
                if (!type1.equals(type2)) {
                    removeFromIntersection.add(type1);
                    removeFromIntersection.add(type2);
                }
            }
            Set<String> intersection = new LinkedHashSet<>(strings1);
            intersection.retainAll(strings2);
            intersection.removeAll(removeFromIntersection);
            strings1.removeAll(intersection);
            strings2.removeAll(intersection);
        } else {
            removeCommonElements(strings1, strings2);
        }
    }

    private LinkedHashSet<String> getVariablesToBeRemovedFromTheIntersection(AbstractCodeFragment statement1,
                                                                             AbstractCodeFragment statement2,
                                                                             Set<String> variableIntersection,
                                                                             Set<String> variables1,
                                                                             Set<String> variables2,
                                                                             ReplacementInfo replacementInfo) {
        LinkedHashSet<String> variablesToBeRemovedFromTheIntersection = new LinkedHashSet<>();
        OperationInvocation invocationCoveringTheEntireStatement1 = statement1.invocationCoveringEntireFragment();
        OperationInvocation invocationCoveringTheEntireStatement2 = statement2.invocationCoveringEntireFragment();
        for (String variable : variableIntersection) {
            // ignore the variables in the intersection that appear with "this." prefix in the sets of variables
            if (!variable.startsWith("this.") && !variableIntersection.contains("this." + variable) &&
                (variables1.contains("this." + variable) || variables2.contains("this." + variable))) {
                variablesToBeRemovedFromTheIntersection.add(variable);
            }
            if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
                invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
                if (!invocationCoveringTheEntireStatement1.getArguments().contains(variable) &&
                    invocationCoveringTheEntireStatement2.getArguments().contains(variable)) {
                    for (String argument : invocationCoveringTheEntireStatement1.getArguments()) {
                        String argumentNoWhiteSpace = argument.replaceAll("\\s", "");
                        if (argument.contains(variable) && !argument.equals(variable) &&
                            !argumentNoWhiteSpace.contains("+" + variable + "+") &&
                            !argumentNoWhiteSpace.contains(variable + "+") &&
                            !argumentNoWhiteSpace.contains("+" + variable) &&
                            !nonMatchedStatementUsesVariableInArgument(replacementInfo.statements1, variable,
                                argument)) {
                            variablesToBeRemovedFromTheIntersection.add(variable);
                        }
                    }
                } else if (invocationCoveringTheEntireStatement1.getArguments().contains(variable) &&
                    !invocationCoveringTheEntireStatement2.getArguments().contains(variable)) {
                    for (String argument : invocationCoveringTheEntireStatement2.getArguments()) {
                        String argumentNoWhiteSpace = argument.replaceAll("\\s", "");
                        if (argument.contains(variable) && !argument.equals(variable) &&
                            !argumentNoWhiteSpace.contains("+" + variable + "+") &&
                            !argumentNoWhiteSpace.contains(variable + "+") &&
                            !argumentNoWhiteSpace.contains("+" + variable) &&
                            !nonMatchedStatementUsesVariableInArgument(replacementInfo.statements2, variable,
                                argument)) {
                            variablesToBeRemovedFromTheIntersection.add(variable);
                        }
                    }
                }
            }
            if (variable.toUpperCase().equals(variable) &&
                !ReplacementUtil.sameCharsBeforeAfter(statement1.getString(), statement2.getString(), variable)) {
                variablesToBeRemovedFromTheIntersection.add(variable);
            }
        }
        return variablesToBeRemovedFromTheIntersection;
    }

    private Set<Replacement> findReplacementsWithExactMatching(AbstractCodeFragment statement1,
                                                               AbstractCodeFragment statement2,
                                                               Map<String, String> parameterToArgumentMap,
                                                               ReplacementInfo replacementInfo) throws
        RefactoringMinerTimedOutException {
        List<VariableDeclaration> variableDeclarations1 = new ArrayList<>(statement1.getVariableDeclarations());
        List<VariableDeclaration> variableDeclarations2 = new ArrayList<>(statement2.getVariableDeclarations());
        VariableDeclaration variableDeclarationWithArrayInitializer1 =
            declarationWithArrayInitializer(variableDeclarations1);
        VariableDeclaration variableDeclarationWithArrayInitializer2 =
            declarationWithArrayInitializer(variableDeclarations2);
        OperationInvocation invocationCoveringTheEntireStatement1 = statement1.invocationCoveringEntireFragment();
        OperationInvocation invocationCoveringTheEntireStatement2 = statement2.invocationCoveringEntireFragment();
        Set<String> variables1 = new LinkedHashSet<>(statement1.getVariables());
        Set<String> variables2 = new LinkedHashSet<>(statement2.getVariables());
        Set<String> variableIntersection = new LinkedHashSet<>(variables1);
        variableIntersection.retainAll(variables2);
        // ignore the variables in the intersection that also appear with "this." prefix in the sets of variables
        // ignore the variables in the intersection that are static fields
        Set<String> variablesToBeRemovedFromTheIntersection = new LinkedHashSet<>();
        for (String variable : variableIntersection) {
            if (!variable.startsWith("this.") && !variableIntersection.contains("this." + variable) &&
                (variables1.contains("this." + variable) || variables2.contains("this." + variable))) {
                variablesToBeRemovedFromTheIntersection.add(variable);
            }
            if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
                invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
                if (!invocationCoveringTheEntireStatement1.getArguments().contains(variable) &&
                    invocationCoveringTheEntireStatement2.getArguments().contains(variable)) {
                    for (String argument : invocationCoveringTheEntireStatement1.getArguments()) {
                        String argumentNoWhiteSpace = argument.replaceAll("\\s", "");
                        if (argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains(
                            "+" + variable + "+") &&
                            !argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains(
                            "+" + variable) &&
                            !nonMatchedStatementUsesVariableInArgument(replacementInfo.statements1, variable,
                                argument)) {
                            variablesToBeRemovedFromTheIntersection.add(variable);
                        }
                    }
                } else if (invocationCoveringTheEntireStatement1.getArguments().contains(variable) &&
                    !invocationCoveringTheEntireStatement2.getArguments().contains(variable)) {
                    for (String argument : invocationCoveringTheEntireStatement2.getArguments()) {
                        String argumentNoWhiteSpace = argument.replaceAll("\\s", "");
                        if (argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains(
                            "+" + variable + "+") &&
                            !argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains(
                            "+" + variable) &&
                            !nonMatchedStatementUsesVariableInArgument(replacementInfo.statements2, variable,
                                argument)) {
                            variablesToBeRemovedFromTheIntersection.add(variable);
                        }
                    }
                }
            }
            if (variable.toUpperCase().equals(variable) && !ReplacementUtil.sameCharsBeforeAfter(statement1.getString(),
                statement2.getString(),
                variable)) {
                variablesToBeRemovedFromTheIntersection.add(variable);
            }
        }
        variableIntersection.removeAll(variablesToBeRemovedFromTheIntersection);
        // remove common variables from the two sets
        variables1.removeAll(variableIntersection);
        variables2.removeAll(variableIntersection);

        // replace variables with the corresponding arguments
        replaceVariablesWithArguments(variables1, parameterToArgumentMap);
        replaceVariablesWithArguments(variables2, parameterToArgumentMap);

        Map<String, List<? extends AbstractCall>> methodInvocationMap1 =
            new LinkedHashMap<>(statement1.getMethodInvocationMap());
        Map<String, List<? extends AbstractCall>> methodInvocationMap2 =
            new LinkedHashMap<>(statement2.getMethodInvocationMap());
        Set<String> methodInvocations1 = new LinkedHashSet<>(methodInvocationMap1.keySet());
        Set<String> methodInvocations2 = new LinkedHashSet<>(methodInvocationMap2.keySet());

        Map<String, List<? extends AbstractCall>> creationMap1 = new LinkedHashMap<>(statement1.getCreationMap());
        Map<String, List<? extends AbstractCall>> creationMap2 = new LinkedHashMap<>(statement2.getCreationMap());
        Set<String> creations1 = new LinkedHashSet<>(creationMap1.keySet());
        Set<String> creations2 = new LinkedHashSet<>(creationMap2.keySet());

        Set<String> arguments1 = new LinkedHashSet<>(statement1.getArguments());
        Set<String> arguments2 = new LinkedHashSet<>(statement2.getArguments());
        removeCommonElements(arguments1, arguments2);

        if (!argumentsWithIdenticalMethodCalls(arguments1, arguments2, variables1, variables2)) {
            findReplacements(arguments1, variables2, replacementInfo, Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_VARIABLE);
        }

        Map<String, String> map = new LinkedHashMap<>();
        Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<>();
        Set<Replacement> replacementsToBeAdded = new LinkedHashSet<>();
        for (Replacement r : replacementInfo.getReplacements()) {
            map.put(r.getBefore(), r.getAfter());
            if (methodInvocationMap1.containsKey(r.getBefore())) {
                Replacement replacement = new VariableReplacementWithMethodInvocation(r.getBefore(), r.getAfter(),
                    (OperationInvocation) methodInvocationMap1.get(
                        r.getBefore()).get(0),
                    Direction.INVOCATION_TO_VARIABLE);
                replacementsToBeAdded.add(replacement);
                replacementsToBeRemoved.add(r);
            }
        }
        replacementInfo.getReplacements().removeAll(replacementsToBeRemoved);
        replacementInfo.getReplacements().addAll(replacementsToBeAdded);

        // replace variables with the corresponding arguments in method invocations
        replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, parameterToArgumentMap);
        replaceVariablesWithArguments(methodInvocationMap2, methodInvocations2, parameterToArgumentMap);

        replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, map);

        //remove methodInvocation covering the entire statement
        if (invocationCoveringTheEntireStatement1 != null) {
            for (String methodInvocation1 : methodInvocationMap1.keySet()) {
                for (AbstractCall call : methodInvocationMap1.get(methodInvocation1)) {
                    if (invocationCoveringTheEntireStatement1.getLocationInfo().equals(call.getLocationInfo())) {
                        methodInvocations1.remove(methodInvocation1);
                    }
                }
            }
        }
        if (invocationCoveringTheEntireStatement2 != null) {
            for (String methodInvocation2 : methodInvocationMap2.keySet()) {
                for (AbstractCall call : methodInvocationMap2.get(methodInvocation2)) {
                    if (invocationCoveringTheEntireStatement2.getLocationInfo().equals(call.getLocationInfo())) {
                        methodInvocations2.remove(methodInvocation2);
                    }
                }
            }
        }
        Set<String> methodInvocationIntersection = new LinkedHashSet<>(methodInvocations1);
        methodInvocationIntersection.retainAll(methodInvocations2);
        Set<String> methodInvocationsToBeRemovedFromTheIntersection = new LinkedHashSet<>();
        for (String methodInvocation : methodInvocationIntersection) {
            if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
                invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
                if (!invocationCoveringTheEntireStatement1.getArguments().contains(methodInvocation) &&
                    invocationCoveringTheEntireStatement2.getArguments().contains(methodInvocation)) {
                    methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
                } else if (invocationCoveringTheEntireStatement1.getArguments().contains(methodInvocation) &&
                    !invocationCoveringTheEntireStatement2.getArguments().contains(methodInvocation)) {
                    methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
                }
            }
        }
        methodInvocationIntersection.removeAll(methodInvocationsToBeRemovedFromTheIntersection);
        // remove common methodInvocations from the two sets
        methodInvocations1.removeAll(methodInvocationIntersection);
        methodInvocations2.removeAll(methodInvocationIntersection);

        Set<String> variablesAndMethodInvocations1 = new LinkedHashSet<>();
        //variablesAndMethodInvocations1.addAll(methodInvocations1);
        //variablesAndMethodInvocations1.addAll(variables1);

        Set<String> variablesAndMethodInvocations2 = new LinkedHashSet<>();
        variablesAndMethodInvocations2.addAll(methodInvocations2);
        variablesAndMethodInvocations2.addAll(variables2);

        Set<String> types1 = new LinkedHashSet<>(statement1.getTypes());
        Set<String> types2 = new LinkedHashSet<>(statement2.getTypes());
        removeCommonTypes(types1, types2, statement1.getTypes(), statement2.getTypes());

        // replace variables with the corresponding arguments in object creations
        replaceVariablesWithArguments(creationMap1, creations1, parameterToArgumentMap);
        replaceVariablesWithArguments(creationMap2, creations2, parameterToArgumentMap);

        replaceVariablesWithArguments(creationMap1, creations1, map);

        ObjectCreation creationCoveringTheEntireStatement1 = statement1.creationCoveringEntireFragment();
        ObjectCreation creationCoveringTheEntireStatement2 = statement2.creationCoveringEntireFragment();
        //remove objectCreation covering the entire statement
        for (String objectCreation1 : creationMap1.keySet()) {
            for (AbstractCall creation1 : creationMap1.get(objectCreation1)) {
                if (creationCoveringTheEntireStatement1 != null &&
                    creationCoveringTheEntireStatement1.getLocationInfo().equals(creation1.getLocationInfo())) {
                    creations1.remove(objectCreation1);
                }
                if (((ObjectCreation) creation1).getAnonymousClassDeclaration() != null) {
                    creations1.remove(objectCreation1);
                }
            }
        }
        for (String objectCreation2 : creationMap2.keySet()) {
            for (AbstractCall creation2 : creationMap2.get(objectCreation2)) {
                if (creationCoveringTheEntireStatement2 != null &&
                    creationCoveringTheEntireStatement2.getLocationInfo().equals(creation2.getLocationInfo())) {
                    creations2.remove(objectCreation2);
                }
                if (((ObjectCreation) creation2).getAnonymousClassDeclaration() != null) {
                    creations2.remove(objectCreation2);
                }
            }
        }
        Set<String> creationIntersection = new LinkedHashSet<>(creations1);
        creationIntersection.retainAll(creations2);
        // remove common creations from the two sets
        creations1.removeAll(creationIntersection);
        creations2.removeAll(creationIntersection);

        Set<String> stringLiterals1 = new LinkedHashSet<>(statement1.getStringLiterals());
        Set<String> stringLiterals2 = new LinkedHashSet<>(statement2.getStringLiterals());
        removeCommonElements(stringLiterals1, stringLiterals2);

        Set<String> numberLiterals1 = new LinkedHashSet<>(statement1.getNumberLiterals());
        Set<String> numberLiterals2 = new LinkedHashSet<>(statement2.getNumberLiterals());
        removeCommonElements(numberLiterals1, numberLiterals2);

        Set<String> booleanLiterals1 = new LinkedHashSet<>(statement1.getBooleanLiterals());
        Set<String> booleanLiterals2 = new LinkedHashSet<>(statement2.getBooleanLiterals());
        removeCommonElements(booleanLiterals1, booleanLiterals2);

        Set<String> infixOperators1 = new LinkedHashSet<>(statement1.getInfixOperators());
        Set<String> infixOperators2 = new LinkedHashSet<>(statement2.getInfixOperators());
        removeCommonElements(infixOperators1, infixOperators2);

        Set<String> arrayAccesses1 = new LinkedHashSet<>(statement1.getArrayAccesses());
        Set<String> arrayAccesses2 = new LinkedHashSet<>(statement2.getArrayAccesses());
        removeCommonElements(arrayAccesses1, arrayAccesses2);

        Set<String> prefixExpressions1 = new LinkedHashSet<>(statement1.getPrefixExpressions());
        Set<String> prefixExpressions2 = new LinkedHashSet<>(statement2.getPrefixExpressions());
        removeCommonElements(prefixExpressions1, prefixExpressions2);

        //perform type replacements
        findReplacements(types1, types2, replacementInfo, Replacement.ReplacementType.TYPE);

        //perform operator replacements
        findReplacements(infixOperators1, infixOperators2, replacementInfo, Replacement.ReplacementType.INFIX_OPERATOR);

        //apply existing replacements on method invocations
        for (String methodInvocation1 : methodInvocations1) {
            String temp = methodInvocation1;
            for (Replacement replacement : replacementInfo.getReplacements()) {
                temp = ReplacementUtil.performReplacement(temp, replacement.getBefore(), replacement.getAfter());
            }
            if (!temp.equals(methodInvocation1)) {
                variablesAndMethodInvocations1.add(temp);
                methodInvocationMap1.put(temp, methodInvocationMap1.get(methodInvocation1));
            }
        }
        //add updated method invocation to the original list of invocations
        methodInvocations1.addAll(variablesAndMethodInvocations1);
        variablesAndMethodInvocations1.addAll(methodInvocations1);
        variablesAndMethodInvocations1.addAll(variables1);

        if (replacementInfo.getRawDistance() > 0) {
            for (String s1 : variablesAndMethodInvocations1) {
                TreeMap<Double, Replacement> replacementMap = new TreeMap<>();
                int minDistance = replacementInfo.getRawDistance();
                for (String s2 : variablesAndMethodInvocations2) {
                    if (Thread.interrupted()) {
                        throw new RefactoringMinerTimedOutException();
                    }
                    String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(),
                        replacementInfo.getArgumentizedString2(), s1, s2);
                    int distanceRaw =
                        StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2(), minDistance);
                    boolean multipleInstances = ReplacementUtil.countInstances(temp, s2) > 1;
                    if (distanceRaw == -1 && multipleInstances) {
                        distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
                    }
                    boolean multipleInstanceRule = multipleInstances && Math.abs(s1.length() - s2.length()) == Math.abs(
                        distanceRaw - minDistance) && !s1.equals(s2);
                    if (distanceRaw >= 0 && (distanceRaw < replacementInfo.getRawDistance() || multipleInstanceRule)) {
                        minDistance = distanceRaw;
                        Replacement replacement = null;
                        if (variables1.contains(s1) && variables2.contains(s2) && variablesStartWithSameCase(s1, s2,
                            parameterToArgumentMap)) {
                            replacement = new Replacement(s1, s2, Replacement.ReplacementType.VARIABLE_NAME);
                            if (s1.startsWith("(") && s2.startsWith("(") && s1.contains(")") && s2.contains(")")) {
                                String prefix1 = s1.substring(0, s1.indexOf(")") + 1);
                                String prefix2 = s2.substring(0, s2.indexOf(")") + 1);
                                if (prefix1.equals(prefix2)) {
                                    String suffix1 = s1.substring(prefix1.length());
                                    String suffix2 = s2.substring(prefix2.length());
                                    replacement = new Replacement(suffix1, suffix2, Replacement.ReplacementType.VARIABLE_NAME);
                                }
                            }
                            VariableDeclaration v1 = statement1.searchVariableDeclaration(s1);
                            VariableDeclaration v2 = statement2.searchVariableDeclaration(s2);
                            if (inconsistentVariableMappingCount(statement1, statement2, v1,
                                v2) > 1 && operation2.loopWithVariables(
                                v1.getVariableName(), v2.getVariableName()) == null) {
                                replacement = null;
                            }
                        } else if (variables1.contains(s1) && methodInvocations2.contains(s2)) {
                            OperationInvocation invokedOperationAfter =
                                (OperationInvocation) methodInvocationMap2.get(s2).get(0);
                            replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationAfter,
                                Direction.VARIABLE_TO_INVOCATION);
                        } else if (methodInvocations1.contains(s1) && methodInvocations2.contains(s2)) {
                            OperationInvocation invokedOperationBefore =
                                (OperationInvocation) methodInvocationMap1.get(s1).get(0);
                            OperationInvocation invokedOperationAfter =
                                (OperationInvocation) methodInvocationMap2.get(s2).get(0);
                            if (invokedOperationBefore.compatibleExpression(invokedOperationAfter)) {
                                replacement = new MethodInvocationReplacement(s1, s2, invokedOperationBefore,
                                    invokedOperationAfter,
                                    Replacement.ReplacementType.METHOD_INVOCATION);
                            }
                        } else if (methodInvocations1.contains(s1) && variables2.contains(s2)) {
                            OperationInvocation invokedOperationBefore =
                                (OperationInvocation) methodInvocationMap1.get(s1).get(0);
                            replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationBefore,
                                Direction.INVOCATION_TO_VARIABLE);
                        }
                        if (replacement != null) {
                            double distancenormalized = (double) distanceRaw / (double) Math.max(temp.length(),
                                replacementInfo.getArgumentizedString2().length());
                            replacementMap.put(distancenormalized, replacement);
                        }
                        if (distanceRaw == 0 && !replacementInfo.getReplacements().isEmpty()) {
                            break;
                        }
                    }
                }
                if (!replacementMap.isEmpty()) {
                    Replacement replacement = replacementMap.firstEntry().getValue();
                    replacementInfo.addReplacement(replacement);
                    replacementInfo.setArgumentizedString1(
                        ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(),
                            replacementInfo.getArgumentizedString2(),
                            replacement.getBefore(), replacement.getAfter()));
                    if (replacementMap.firstEntry().getKey() == 0) {
                        break;
                    }
                }
            }
        }

        //perform creation replacements
        findReplacements(creations1, creations2, replacementInfo, Replacement.ReplacementType.CLASS_INSTANCE_CREATION);

        //perform literal replacements
        findReplacements(stringLiterals1, stringLiterals2, replacementInfo, Replacement.ReplacementType.STRING_LITERAL);
        findReplacements(numberLiterals1, numberLiterals2, replacementInfo, Replacement.ReplacementType.NUMBER_LITERAL);
        if (!statement1.containsInitializerOfVariableDeclaration(numberLiterals1) &&
            !statement2.containsInitializerOfVariableDeclaration(variables2) &&
            !statement1.getString().endsWith("=0\n")) {
            findReplacements(numberLiterals1, variables2, replacementInfo,
                Replacement.ReplacementType.VARIABLE_REPLACED_WITH_NUMBER_LITERAL);
        }
        findReplacements(variables1, arrayAccesses2, replacementInfo,
            Replacement.ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS);
        findReplacements(arrayAccesses1, variables2, replacementInfo,
            Replacement.ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS);

        findReplacements(methodInvocations1, arrayAccesses2, replacementInfo,
            Replacement.ReplacementType.ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION);
        findReplacements(arrayAccesses1, methodInvocations2, replacementInfo,
            Replacement.ReplacementType.ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION);

        findReplacements(variables1, prefixExpressions2, replacementInfo,
            Replacement.ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION);
        findReplacements(prefixExpressions1, variables2, replacementInfo,
            Replacement.ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION);
        findReplacements(stringLiterals1, variables2, replacementInfo,
            Replacement.ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL);
        if (statement1.getNullLiterals().isEmpty() && !statement2.getNullLiterals().isEmpty()) {
            Set<String> nullLiterals2 = new LinkedHashSet<>();
            nullLiterals2.add("Null");
            findReplacements(variables1, nullLiterals2, replacementInfo,
                Replacement.ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL);
        }

        if (!statement1.getString().endsWith("=True\n") && !statement1.getString().endsWith("=False\n")) {
            findReplacements(booleanLiterals1, variables2, replacementInfo,
                Replacement.ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE);
        }
        if (!statement2.getString().endsWith("=True\n") && !statement2.getString().endsWith("=False\n")) {
            findReplacements(arguments1, booleanLiterals2, replacementInfo,
                Replacement.ReplacementType.BOOLEAN_REPLACED_WITH_ARGUMENT);
        }

        String s1 = preprocessInput1(statement1, statement2);
        String s2 = preprocessInput2(statement1, statement2);
        replacementsToBeRemoved = new LinkedHashSet<>();
        replacementsToBeAdded = new LinkedHashSet<>();
        for (Replacement replacement : replacementInfo.getReplacements()) {
            s1 = ReplacementUtil.performReplacement(s1, s2, replacement.getBefore(), replacement.getAfter());
            //find variable replacements within method invocation replacements
            Set<Replacement> set =
                replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1,
                    methodInvocations2, methodInvocationMap2,
                    Direction.VARIABLE_TO_INVOCATION);
            set.addAll(
                replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), methodInvocations1,
                    variables2, methodInvocationMap1,
                    Direction.INVOCATION_TO_VARIABLE));
            if (!set.isEmpty()) {
                replacementsToBeRemoved.add(replacement);
                replacementsToBeAdded.addAll(set);
            }
            Replacement r =
                variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1,
                    variables2);
            if (r != null) {
                replacementsToBeRemoved.add(replacement);
                replacementsToBeAdded.add(r);
            }
            Replacement r2 = variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(),
                stringLiterals1, variables2);
            if (r2 != null) {
                replacementsToBeRemoved.add(replacement);
                replacementsToBeAdded.add(r2);
            }
        }
        replacementInfo.removeReplacements(replacementsToBeRemoved);
        replacementInfo.addReplacements(replacementsToBeAdded);
        boolean isEqualWithReplacement = s1.equals(s2) || replacementInfo.argumentizedString1.equals(
            replacementInfo.argumentizedString2) || differOnlyInCastExpressionOrPrefixOperator(s1, s2,
            replacementInfo) ||
            oneIsVariableDeclarationTheOtherIsVariableAssignment(s1, s2,
                replacementInfo) || identicalVariableDeclarationsWithDifferentNames(
            s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo) ||
            oneIsVariableDeclarationTheOtherIsReturnStatement(s1,
                s2) || oneIsVariableDeclarationTheOtherIsReturnStatement(
            statement1.getString(), statement2.getString()) ||
            (containsValidOperatorReplacements(replacementInfo) && (equalAfterInfixExpressionExpansion(s1, s2,
                replacementInfo,
                statement1.getInfixExpressions()) || commonConditional(
                s1, s2, replacementInfo))) ||
            equalAfterArgumentMerge(s1, s2, replacementInfo) ||
            equalAfterNewArgumentAdditions(s1, s2, replacementInfo) ||
            (validStatementForConcatComparison(statement1, statement2) && commonConcat(s1, s2, replacementInfo));
        List<AnonymousClassDeclarationObject> anonymousClassDeclarations1 = statement1.getAnonymousClassDeclarations();
        List<AnonymousClassDeclarationObject> anonymousClassDeclarations2 = statement2.getAnonymousClassDeclarations();
        if (isEqualWithReplacement) {
            List<Replacement> typeReplacements = replacementInfo.getReplacements(Replacement.ReplacementType.TYPE);
            if (typeReplacements.size() > 0 && invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
                for (Replacement typeReplacement : typeReplacements) {
                    if (invocationCoveringTheEntireStatement1.getMethodName().contains(
                        typeReplacement.getBefore()) && invocationCoveringTheEntireStatement2.getMethodName().contains(
                        typeReplacement.getAfter())) {
                        if (invocationCoveringTheEntireStatement1.identicalExpression(
                            invocationCoveringTheEntireStatement2) && invocationCoveringTheEntireStatement1.equalArguments(
                            invocationCoveringTheEntireStatement2)) {
                            Replacement replacement =
                                new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
                                    invocationCoveringTheEntireStatement2.getName(),
                                    invocationCoveringTheEntireStatement1,
                                    invocationCoveringTheEntireStatement2,
                                    Replacement.ReplacementType.METHOD_INVOCATION_NAME);
                            replacementInfo.addReplacement(replacement);
                        } else {
                            Replacement replacement =
                                new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
                                    invocationCoveringTheEntireStatement2.actualString(),
                                    invocationCoveringTheEntireStatement1,
                                    invocationCoveringTheEntireStatement2,
                                    Replacement.ReplacementType.METHOD_INVOCATION);
                            replacementInfo.addReplacement(replacement);
                        }
                        break;
                    }
                }
            }
            if (variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2,
                replacementInfo) &&
                !statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
                !statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
                return null;
            }
            if (variableAssignmentWithEverythingReplaced(statement1, statement2, replacementInfo)) {
                return null;
            }
            if (classInstanceCreationWithEverythingReplaced(statement1, statement2, replacementInfo,
                parameterToArgumentMap)) {
                return null;
            }
            if (!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty()) {
                Set<Replacement> replacementsInsideAnonymous = new LinkedHashSet<>();
                for (Replacement replacement : replacementInfo.getReplacements()) {
                    if (replacement instanceof MethodInvocationReplacement) {
                        for (int i = 0; i < anonymousClassDeclarations1.size(); i++) {
                            for (int j = 0; j < anonymousClassDeclarations2.size(); j++) {
                                AnonymousClassDeclarationObject anonymousClassDeclaration1 =
                                    anonymousClassDeclarations1.get(i);
                                AnonymousClassDeclarationObject anonymousClassDeclaration2 =
                                    anonymousClassDeclarations2.get(j);
                                if (anonymousClassDeclaration1.getMethodInvocationMap().containsKey(
                                    replacement.getBefore()) &&
                                    anonymousClassDeclaration2.getMethodInvocationMap().containsKey(
                                        replacement.getAfter())) {
                                    replacementsInsideAnonymous.add(replacement);
                                    break;
                                }
                            }
                            if (replacementsInsideAnonymous.contains(replacement)) {
                                break;
                            }
                        }
                    }
                }
                for (Replacement replacement : replacementsInsideAnonymous) {
                    equalAfterNewArgumentAdditions(replacement.getBefore(), replacement.getAfter(), replacementInfo);
                }
            }
            return replacementInfo.getReplacements();
        }
        List<LambdaExpressionObject> lambdas1 = statement1.getLambdas();
        List<LambdaExpressionObject> lambdas2 = statement2.getLambdas();
        List<UMLOperationBodyMapper> lambdaMappers = new ArrayList<>();
        if (!lambdas1.isEmpty() && !lambdas2.isEmpty()) {
            for (int i = 0; i < lambdas1.size(); i++) {
                for (int j = 0; j < lambdas2.size(); j++) {
                    LambdaExpressionObject lambda1 = lambdas1.get(i);
                    LambdaExpressionObject lambda2 = lambdas2.get(j);
                    UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(lambda1, lambda2, this);
                    int mappings = mapper.mappingsWithoutBlocks();
                    if (mappings > 0) {
                        int nonMappedElementsT1 = mapper.nonMappedElementsT1();
                        int nonMappedElementsT2 = mapper.nonMappedElementsT2();
                        if (mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) {
                            this.mappings.addAll(mapper.mappings);
                            this.nonMappedInnerNodesT1.addAll(mapper.nonMappedInnerNodesT1);
                            this.nonMappedInnerNodesT2.addAll(mapper.nonMappedInnerNodesT2);
                            this.nonMappedLeavesT1.addAll(mapper.nonMappedLeavesT1);
                            this.nonMappedLeavesT2.addAll(mapper.nonMappedLeavesT2);
                            this.refactorings.addAll(mapper.getRefactorings());
                            lambdaMappers.add(mapper);
                        }
                    }
                }
            }
        }
        OperationInvocation assignmentInvocationCoveringTheEntireStatement1 =
            invocationCoveringTheEntireStatement1 == null ? statement1.assignmentInvocationCoveringEntireStatement() :
                invocationCoveringTheEntireStatement1;
        //method invocation is identical
        if (assignmentInvocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
            for (String key1 : methodInvocationMap1.keySet()) {
                for (AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
                    if (invocation1.identical(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(),
                        lambdaMappers) &&
                        !assignmentInvocationCoveringTheEntireStatement1.getArguments().contains(key1)) {
                        String expression1 = assignmentInvocationCoveringTheEntireStatement1.getExpression();
                        if (expression1 == null || !expression1.contains(key1)) {
                            return replacementInfo.getReplacements();
                        }
                    } else if (invocation1.identicalName(
                        invocationCoveringTheEntireStatement2) && invocation1.equalArguments(
                        invocationCoveringTheEntireStatement2) &&
                        !assignmentInvocationCoveringTheEntireStatement1.getArguments().contains(
                            key1) && invocationCoveringTheEntireStatement2.getExpression() != null) {
                        boolean expressionMatched = false;
                        Set<AbstractCodeFragment> additionallyMatchedStatements2 =
                            new LinkedHashSet<>();
                        for (AbstractCodeFragment codeFragment : replacementInfo.statements2) {
                            VariableDeclaration variableDeclaration = codeFragment.getVariableDeclaration(
                                invocationCoveringTheEntireStatement2.getExpression());
                            OperationInvocation invocationCoveringEntireCodeFragment =
                                codeFragment.invocationCoveringEntireFragment();
                            if (variableDeclaration != null && variableDeclaration.getInitializer() != null && invocation1.getExpression() != null && invocation1.getExpression().equals(
                                variableDeclaration.getInitializer().getString())) {
                                Replacement r =
                                    new Replacement(invocation1.getExpression(), variableDeclaration.getVariableName(),
                                        Replacement.ReplacementType.VARIABLE_REPLACED_WITH_EXPRESSION_OF_METHOD_INVOCATION);
                                replacementInfo.getReplacements().add(r);
                                additionallyMatchedStatements2.add(codeFragment);
                                expressionMatched = true;
                            }
                            if (invocationCoveringEntireCodeFragment != null && assignmentInvocationCoveringTheEntireStatement1.identicalName(
                                invocationCoveringEntireCodeFragment) &&
                                assignmentInvocationCoveringTheEntireStatement1.equalArguments(
                                    invocationCoveringEntireCodeFragment)) {
                                additionallyMatchedStatements2.add(codeFragment);
                            }
                        }
                        if (expressionMatched) {
                            if (additionallyMatchedStatements2.size() > 0) {
                                Replacement r = new CompositeReplacement(statement1.getString(), statement2.getString(),
                                    new LinkedHashSet<>(),
                                    additionallyMatchedStatements2);
                                replacementInfo.getReplacements().add(r);
                            }
                            return replacementInfo.getReplacements();
                        }
                    }
                }
            }
        }
        //method invocation is identical with a difference in the expression call chain
        if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
            if (invocationCoveringTheEntireStatement1.identicalWithExpressionCallChainDifference(
                invocationCoveringTheEntireStatement2)) {
                List<? extends AbstractCall> invokedOperationsBefore =
                    methodInvocationMap1.get(invocationCoveringTheEntireStatement1.getExpression());
                List<? extends AbstractCall> invokedOperationsAfter =
                    methodInvocationMap2.get(invocationCoveringTheEntireStatement2.getExpression());
                if (invokedOperationsBefore != null && invokedOperationsBefore.size() > 0 && invokedOperationsAfter != null && invokedOperationsAfter.size() > 0) {
                    OperationInvocation invokedOperationBefore = (OperationInvocation) invokedOperationsBefore.get(0);
                    OperationInvocation invokedOperationAfter = (OperationInvocation) invokedOperationsAfter.get(0);
                    Replacement replacement =
                        new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getExpression(),
                            invocationCoveringTheEntireStatement2.getExpression(),
                            invokedOperationBefore, invokedOperationAfter,
                            Replacement.ReplacementType.METHOD_INVOCATION_EXPRESSION);
                    replacementInfo.addReplacement(replacement);
                    return replacementInfo.getReplacements();
                } else if (invokedOperationsBefore != null && invokedOperationsBefore.size() > 0) {
                    OperationInvocation invokedOperationBefore = (OperationInvocation) invokedOperationsBefore.get(0);
                    Replacement replacement = new VariableReplacementWithMethodInvocation(
                        invocationCoveringTheEntireStatement1.getExpression(),
                        invocationCoveringTheEntireStatement2.getExpression(), invokedOperationBefore,
                        Direction.INVOCATION_TO_VARIABLE);
                    replacementInfo.addReplacement(replacement);
                    return replacementInfo.getReplacements();
                } else if (invokedOperationsAfter != null && invokedOperationsAfter.size() > 0) {
                    OperationInvocation invokedOperationAfter = (OperationInvocation) invokedOperationsAfter.get(0);
                    Replacement replacement = new VariableReplacementWithMethodInvocation(
                        invocationCoveringTheEntireStatement1.getExpression(),
                        invocationCoveringTheEntireStatement2.getExpression(), invokedOperationAfter,
                        Direction.VARIABLE_TO_INVOCATION);
                    replacementInfo.addReplacement(replacement);
                    return replacementInfo.getReplacements();
                }
                if (invocationCoveringTheEntireStatement1.numberOfSubExpressions() == invocationCoveringTheEntireStatement2.numberOfSubExpressions() &&
                    invocationCoveringTheEntireStatement1.getExpression().contains(
                        ".") == invocationCoveringTheEntireStatement2.getExpression().contains(".")) {
                    return replacementInfo.getReplacements();
                }
            }
        }
        //method invocation is identical if arguments are replaced !!!!!!!!!!!!!!!!DEBUG HERE!!!!!!!!!!!!!!!!!!!!!!
        if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
            invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2,
                replacementInfo.getReplacements()) &&
            invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
            for (String key : methodInvocationMap2.keySet()) {
                for (AbstractCall invocation2 : methodInvocationMap2.get(key)) {
                    if (invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocation2,
                        replacementInfo.getReplacements(),
                        lambdaMappers)) {
                        return replacementInfo.getReplacements();
                    }
                }
            }
        }
        //method invocation is identical if arguments are wrapped or concatenated
        if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
            invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2,
                replacementInfo.getReplacements()) &&
            invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
            for (String key : methodInvocationMap2.keySet()) {
                for (AbstractCall invocation2 : methodInvocationMap2.get(key)) {
                    if (invocationCoveringTheEntireStatement1.identicalOrWrappedArguments(invocation2)) {
                        Replacement replacement =
                            new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
                                invocationCoveringTheEntireStatement2.actualString(),
                                invocationCoveringTheEntireStatement1,
                                invocationCoveringTheEntireStatement2,
                                Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT_WRAPPED);
                        replacementInfo.addReplacement(replacement);
                        return replacementInfo.getReplacements();
                    }
                    if (invocationCoveringTheEntireStatement1.identicalOrConcatenatedArguments(invocation2)) {
                        Replacement replacement =
                            new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
                                invocationCoveringTheEntireStatement2.actualString(),
                                invocationCoveringTheEntireStatement1,
                                invocationCoveringTheEntireStatement2,
                                Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT_CONCATENATED);
                        replacementInfo.addReplacement(replacement);
                        return replacementInfo.getReplacements();
                    }
                }
            }
        }
        //method invocation has been renamed but the expression and arguments are identical
        if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
            invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndArguments(
                invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(),
                UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers)) {
            Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
                invocationCoveringTheEntireStatement2.getName(),
                invocationCoveringTheEntireStatement1,
                invocationCoveringTheEntireStatement2,
                Replacement.ReplacementType.METHOD_INVOCATION_NAME);
            replacementInfo.addReplacement(replacement);
            return replacementInfo.getReplacements();
        }
        //method invocation has been renamed but the expressions are null and arguments are identical
        if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
            invocationCoveringTheEntireStatement1.renamedWithIdenticalArgumentsAndNoExpression(
                invocationCoveringTheEntireStatement2, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers)) {
            Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
                invocationCoveringTheEntireStatement2.getName(),
                invocationCoveringTheEntireStatement1,
                invocationCoveringTheEntireStatement2,
                Replacement.ReplacementType.METHOD_INVOCATION_NAME);
            replacementInfo.addReplacement(replacement);
            return replacementInfo.getReplacements();
        }
        //method invocation has been renamed (one name contains the other), one expression is null, but the other is not null, and arguments are identical
        if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
            invocationCoveringTheEntireStatement1.renamedWithDifferentExpressionAndIdenticalArguments(
                invocationCoveringTheEntireStatement2)) {
            Replacement replacement =
                new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
                    invocationCoveringTheEntireStatement2.actualString(),
                    invocationCoveringTheEntireStatement1,
                    invocationCoveringTheEntireStatement2,
                    Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
            replacementInfo.addReplacement(replacement);
            return replacementInfo.getReplacements();
        }
        //method invocation has been renamed and arguments changed, but the expressions are identical
        if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
            invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndDifferentNumberOfArguments(
                invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(),
                UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers)) {
            Replacement.ReplacementType type = invocationCoveringTheEntireStatement1.getName().equals(
                invocationCoveringTheEntireStatement2.getName()) ? Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT :
                Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT;
            Replacement replacement =
                new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
                    invocationCoveringTheEntireStatement2.actualString(),
                    invocationCoveringTheEntireStatement1,
                    invocationCoveringTheEntireStatement2, type);
            replacementInfo.addReplacement(replacement);
            return replacementInfo.getReplacements();
        }
        if (!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
            for (String methodInvocation1 : methodInvocations1) {
                for (AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
                    if (operationInvocation1.renamedWithIdenticalExpressionAndDifferentNumberOfArguments(
                        invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(),
                        UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers) &&
                        !isExpressionOfAnotherMethodInvocation(operationInvocation1, methodInvocationMap1)) {
                        Replacement.ReplacementType type =
                            operationInvocation1.getName().equals(invocationCoveringTheEntireStatement2.getName()) ?
                                Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT :
                                Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT;
                        Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
                            invocationCoveringTheEntireStatement2.actualString(),
                            (OperationInvocation) operationInvocation1,
                            invocationCoveringTheEntireStatement2,
                            type);
                        replacementInfo.addReplacement(replacement);
                        return replacementInfo.getReplacements();
                    }
                }
            }
        }
        //method invocation has only changes in the arguments (different number of arguments)
        if (invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
            if (invocationCoveringTheEntireStatement1.identicalWithMergedArguments(
                invocationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
                return replacementInfo.getReplacements();
            } else if (invocationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(
                invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
                Replacement replacement =
                    new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
                        invocationCoveringTheEntireStatement2.actualString(),
                        invocationCoveringTheEntireStatement1,
                        invocationCoveringTheEntireStatement2,
                        Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT);
                replacementInfo.addReplacement(replacement);
                return replacementInfo.getReplacements();
            }
        }
        if (!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
            for (String methodInvocation1 : methodInvocations1) {
                for (AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
                    if (operationInvocation1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2,
                        replacementInfo.getReplacements())) {
                        return replacementInfo.getReplacements();
                    } else if (operationInvocation1.identicalWithDifferentNumberOfArguments(
                        invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(),
                        parameterToArgumentMap)) {
                        Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
                            invocationCoveringTheEntireStatement2.actualString(),
                            (OperationInvocation) operationInvocation1,
                            invocationCoveringTheEntireStatement2,
                            Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT);
                        replacementInfo.addReplacement(replacement);
                        return replacementInfo.getReplacements();
                    }
                }
            }
        }
        //check if the argument of the method call in the first statement is returned in the second statement
        Replacement r;
        if (invocationCoveringTheEntireStatement1 != null && (r =
            invocationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(
                replacementInfo.getArgumentizedString2())) != null) {
            replacementInfo.addReplacement(r);
            return replacementInfo.getReplacements();
        }
        for (String methodInvocation1 : methodInvocations1) {
            for (AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
                if (statement1.getString().endsWith(methodInvocation1 + ";\n") && (r =
                    operationInvocation1.makeReplacementForReturnedArgument(
                        replacementInfo.getArgumentizedString2())) != null) {
                    if (operationInvocation1.makeReplacementForReturnedArgument(statement2.getString()) != null) {
                        replacementInfo.addReplacement(r);
                        return replacementInfo.getReplacements();
                    }
                }
            }
        }
        //check if the argument of the method call in the second statement is returned in the first statement
        if (invocationCoveringTheEntireStatement2 != null && (r =
            invocationCoveringTheEntireStatement2.makeReplacementForWrappedCall(
                replacementInfo.getArgumentizedString1())) != null) {
            replacementInfo.addReplacement(r);
            return replacementInfo.getReplacements();
        }
        for (String methodInvocation2 : methodInvocations2) {
            for (AbstractCall operationInvocation2 : methodInvocationMap2.get(methodInvocation2)) {
                if (statement2.getString().endsWith(methodInvocation2 + ";\n") && (r =
                    operationInvocation2.makeReplacementForWrappedCall(
                        replacementInfo.getArgumentizedString1())) != null) {
                    if (operationInvocation2.makeReplacementForWrappedCall(statement1.getString()) != null) {
                        replacementInfo.addReplacement(r);
                        return replacementInfo.getReplacements();
                    }
                }
            }
        }
        //check if the argument of the method call in the second statement is the right hand side of an assignment in the first statement
        if (invocationCoveringTheEntireStatement2 != null &&
            (r = invocationCoveringTheEntireStatement2.makeReplacementForAssignedArgument(
                replacementInfo.getArgumentizedString1())) != null &&
            methodInvocationMap1.containsKey(invocationCoveringTheEntireStatement2.getArguments().get(0))) {
            replacementInfo.addReplacement(r);
            return replacementInfo.getReplacements();
        }
        //check if the method call in the second statement is the expression (or sub-expression) of the method invocation in the first statement
        if (invocationCoveringTheEntireStatement2 != null) {
            for (String key1 : methodInvocationMap1.keySet()) {
                for (AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
                    if (statement1.getString().endsWith(key1 + ";\n")) {
                        if (methodInvocationMap2.containsKey(invocation1.getExpression())) {
                            Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
                                invocationCoveringTheEntireStatement2.actualString(),
                                (OperationInvocation) invocation1,
                                invocationCoveringTheEntireStatement2,
                                Replacement.ReplacementType.METHOD_INVOCATION);
                            replacementInfo.addReplacement(replacement);
                            return replacementInfo.getReplacements();
                        }
                        if (invocation1 instanceof OperationInvocation) {
                            for (String subExpression1 : ((OperationInvocation) invocation1).getSubExpressions()) {
                                if (methodInvocationMap2.containsKey(subExpression1)) {
                                    AbstractCall subOperationInvocation1 = null;
                                    for (String key : methodInvocationMap1.keySet()) {
                                        if (key.endsWith(subExpression1)) {
                                            subOperationInvocation1 = methodInvocationMap1.get(key).get(0);
                                            break;
                                        }
                                    }
                                    Replacement replacement = new MethodInvocationReplacement(subExpression1,
                                        invocationCoveringTheEntireStatement2.actualString(),
                                        (OperationInvocation) subOperationInvocation1,
                                        invocationCoveringTheEntireStatement2,
                                        Replacement.ReplacementType.METHOD_INVOCATION);
                                    replacementInfo.addReplacement(replacement);
                                    return replacementInfo.getReplacements();
                                }
                            }
                        }
                    }
                }
            }
        }
        //check if the method call in the first statement is the expression (or sub-expression) of the method invocation in the second statement
        if (invocationCoveringTheEntireStatement1 != null) {
            for (String key2 : methodInvocationMap2.keySet()) {
                for (AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
                    if (statement2.getString().endsWith(key2 + ";\n")) {
                        if (methodInvocationMap1.containsKey(invocation2.getExpression())) {
                            Replacement replacement =
                                new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
                                    invocation2.actualString(),
                                    invocationCoveringTheEntireStatement1,
                                    (OperationInvocation) invocation2,
                                    Replacement.ReplacementType.METHOD_INVOCATION);
                            replacementInfo.addReplacement(replacement);
                            return replacementInfo.getReplacements();
                        }
                        if (invocation2 instanceof OperationInvocation) {
                            for (String subExpression2 : ((OperationInvocation) invocation2).getSubExpressions()) {
                                if (methodInvocationMap1.containsKey(subExpression2)) {
                                    AbstractCall subOperationInvocation2 = null;
                                    for (String key : methodInvocationMap2.keySet()) {
                                        if (key.endsWith(subExpression2)) {
                                            subOperationInvocation2 = methodInvocationMap2.get(key).get(0);
                                            break;
                                        }
                                    }
                                    Replacement replacement = new MethodInvocationReplacement(
                                        invocationCoveringTheEntireStatement1.actualString(),
                                        subExpression2, invocationCoveringTheEntireStatement1,
                                        (OperationInvocation) subOperationInvocation2,
                                        Replacement.ReplacementType.METHOD_INVOCATION);
                                    replacementInfo.addReplacement(replacement);
                                    return replacementInfo.getReplacements();
                                }
                            }
                        }
                    }
                }
            }
        }
        //check if the argument of the class instance creation in the first statement is the expression of the method invocation in the second statement
        if (creationCoveringTheEntireStatement1 != null) {
            for (String key2 : methodInvocationMap2.keySet()) {
                for (AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
                    if (statement2.getString().endsWith(key2 + ";\n") &&
                        creationCoveringTheEntireStatement1.getArguments().contains(invocation2.getExpression())) {
                        Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(
                            creationCoveringTheEntireStatement1.getName(),
                            invocation2.getName(),
                            Replacement.ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION,
                            creationCoveringTheEntireStatement1, (OperationInvocation) invocation2);
                        replacementInfo.addReplacement(replacement);
                        return replacementInfo.getReplacements();
                    }
                }
            }
        }
        //builder call chain in the first statement is replaced with class instance creation in the second statement
        if (invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
            if (invocationCoveringTheEntireStatement1.getName().equals("build")) {
                int commonArguments = 0;
                for (String key1 : methodInvocationMap1.keySet()) {
                    if (invocationCoveringTheEntireStatement1.actualString().startsWith(key1)) {
                        for (AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
                            Set<String> argumentIntersection =
                                invocation1.argumentIntersection(creationCoveringTheEntireStatement2);
                            commonArguments += argumentIntersection.size();
                        }
                    }
                }
                if (commonArguments > 0) {
                    Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(
                        invocationCoveringTheEntireStatement1.getName(),
                        creationCoveringTheEntireStatement2.getName(),
                        Replacement.ReplacementType.BUILDER_REPLACED_WITH_CLASS_INSTANCE_CREATION,
                        invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2);
                    replacementInfo.addReplacement(replacement);
                    return replacementInfo.getReplacements();
                }
            }
        }
        //object creation is identical
        if (creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
            creationCoveringTheEntireStatement1.identical(creationCoveringTheEntireStatement2,
                replacementInfo.getReplacements(), lambdaMappers)) {
            boolean identicalArrayInitializer = true;
            if (creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray()) {
                identicalArrayInitializer =
                    creationCoveringTheEntireStatement1.identicalArrayInitializer(creationCoveringTheEntireStatement2);
            }
            if (identicalArrayInitializer) {
                String anonymousClassDeclaration1 = creationCoveringTheEntireStatement1.getAnonymousClassDeclaration();
                String anonymousClassDeclaration2 = creationCoveringTheEntireStatement2.getAnonymousClassDeclaration();
                if (anonymousClassDeclaration1 != null && anonymousClassDeclaration2 != null && !anonymousClassDeclaration1.equals(
                    anonymousClassDeclaration2)) {
                    Replacement replacement = new Replacement(anonymousClassDeclaration1, anonymousClassDeclaration2,
                        Replacement.ReplacementType.ANONYMOUS_CLASS_DECLARATION);
                    replacementInfo.addReplacement(replacement);
                }
                return replacementInfo.getReplacements();
            }
        }
        //object creation has identical arguments, but different type
        if (creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
            creationCoveringTheEntireStatement1.getArguments().size() > 0 && creationCoveringTheEntireStatement1.equalArguments(
            creationCoveringTheEntireStatement2)) {
            Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.getName(),
                creationCoveringTheEntireStatement2.getName(),
                creationCoveringTheEntireStatement1,
                creationCoveringTheEntireStatement2,
                Replacement.ReplacementType.CLASS_INSTANCE_CREATION);
            replacementInfo.addReplacement(replacement);
            return replacementInfo.getReplacements();
        }
        //object creation has only changes in the arguments
        if (creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
            if (creationCoveringTheEntireStatement1.identicalWithMergedArguments(creationCoveringTheEntireStatement2,
                replacementInfo.getReplacements())) {
                return replacementInfo.getReplacements();
            } else if (creationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(
                creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
                Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.getName(),
                    creationCoveringTheEntireStatement2.getName(),
                    creationCoveringTheEntireStatement1,
                    creationCoveringTheEntireStatement2,
                    Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
                replacementInfo.addReplacement(replacement);
                return replacementInfo.getReplacements();
            }
        }
        //check if the argument lists are identical after replacements
        if (creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
            creationCoveringTheEntireStatement1.identicalName(creationCoveringTheEntireStatement2) &&
            creationCoveringTheEntireStatement1.identicalExpression(creationCoveringTheEntireStatement2,
                replacementInfo.getReplacements())) {
            if (creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() && s1.contains(
                "[") && s2.contains("[") &&
                s1.substring(s1.indexOf("[") + 1, s1.lastIndexOf("]")).equals(
                    s2.substring(s2.indexOf("[") + 1, s2.lastIndexOf("]"))) &&
                s1.substring(s1.indexOf("[") + 1, s1.lastIndexOf("]")).length() > 0) {
                return replacementInfo.getReplacements();
            }
            if (!creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() && s1.contains(
                "(") && s2.contains("(") &&
                s1.substring(s1.indexOf("(") + 1, s1.lastIndexOf(")")).equals(
                    s2.substring(s2.indexOf("(") + 1, s2.lastIndexOf(")"))) &&
                s1.substring(s1.indexOf("(") + 1, s1.lastIndexOf(")")).length() > 0) {
                return replacementInfo.getReplacements();
            }
        }
        //check if array creation is replaced with data structure creation
        if (creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
            variableDeclarations1.size() == 1 && variableDeclarations2.size() == 1) {
            VariableDeclaration v1 = variableDeclarations1.get(0);
            VariableDeclaration v2 = variableDeclarations2.get(0);
            String initializer1 = v1.getInitializer() != null ? v1.getInitializer().getString() : null;
            String initializer2 = v2.getInitializer() != null ? v2.getInitializer().getString() : null;
            if (v1.getType().getArrayDimension() == 1 && v2.getType().containsTypeArgument(
                v1.getType().getClassType()) &&
                creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() &&
                initializer1 != null && initializer2 != null &&
                initializer1.substring(initializer1.indexOf("[") + 1, initializer1.lastIndexOf("]")).equals(
                    initializer2.substring(initializer2.indexOf("(") + 1, initializer2.lastIndexOf(")")))) {
                r = new ObjectCreationReplacement(initializer1, initializer2,
                    creationCoveringTheEntireStatement1,
                    creationCoveringTheEntireStatement2,
                    Replacement.ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
                replacementInfo.addReplacement(r);
                return replacementInfo.getReplacements();
            }
            if (v2.getType().getArrayDimension() == 1 && v1.getType().containsTypeArgument(
                v2.getType().getClassType()) &&
                !creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() &&
                initializer1 != null && initializer2 != null &&
                initializer1.substring(initializer1.indexOf("(") + 1, initializer1.lastIndexOf(")")).equals(
                    initializer2.substring(initializer2.indexOf("[") + 1, initializer2.lastIndexOf("]")))) {
                r = new ObjectCreationReplacement(initializer1, initializer2,
                    creationCoveringTheEntireStatement1,
                    creationCoveringTheEntireStatement2,
                    Replacement.ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
                replacementInfo.addReplacement(r);
                return replacementInfo.getReplacements();
            }
        }
        if (!creations1.isEmpty() && creationCoveringTheEntireStatement2 != null) {
            for (String creation1 : creations1) {
                for (AbstractCall objectCreation1 : creationMap1.get(creation1)) {
                    if (objectCreation1.identicalWithMergedArguments(creationCoveringTheEntireStatement2,
                        replacementInfo.getReplacements())) {
                        return replacementInfo.getReplacements();
                    } else if (objectCreation1.identicalWithDifferentNumberOfArguments(
                        creationCoveringTheEntireStatement2, replacementInfo.getReplacements(),
                        parameterToArgumentMap)) {
                        Replacement replacement = new ObjectCreationReplacement(objectCreation1.getName(),
                            creationCoveringTheEntireStatement2.getName(),
                            (ObjectCreation) objectCreation1,
                            creationCoveringTheEntireStatement2,
                            Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
                        replacementInfo.addReplacement(replacement);
                        return replacementInfo.getReplacements();
                    }
                    //check if the argument lists are identical after replacements
                    if (objectCreation1.identicalName(creationCoveringTheEntireStatement2) &&
                        objectCreation1.identicalExpression(creationCoveringTheEntireStatement2,
                            replacementInfo.getReplacements())) {
                        if (((ObjectCreation) objectCreation1).isArray() && creationCoveringTheEntireStatement2.isArray() && s1.contains(
                            "[") && s2.contains("[") &&
                            s1.substring(s1.indexOf("[") + 1, s1.lastIndexOf("]")).equals(
                                s2.substring(s2.indexOf("[") + 1, s2.lastIndexOf("]"))) &&
                            s1.substring(s1.indexOf("[") + 1, s1.lastIndexOf("]")).length() > 0) {
                            return replacementInfo.getReplacements();
                        }
                        if (!((ObjectCreation) objectCreation1).isArray() && !creationCoveringTheEntireStatement2.isArray() && s1.contains(
                            "(") && s2.contains("(") &&
                            s1.substring(s1.indexOf("(") + 1, s1.lastIndexOf(")")).equals(
                                s2.substring(s2.indexOf("(") + 1, s2.lastIndexOf(")"))) &&
                            s1.substring(s1.indexOf("(") + 1, s1.lastIndexOf(")")).length() > 0) {
                            return replacementInfo.getReplacements();
                        }
                    }
                }
            }
        }
        if (creationCoveringTheEntireStatement1 != null && (r =
            creationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(
                replacementInfo.getArgumentizedString2())) != null) {
            replacementInfo.addReplacement(r);
            return replacementInfo.getReplacements();
        }
        for (String creation1 : creations1) {
            for (AbstractCall objectCreation1 : creationMap1.get(creation1)) {
                if (statement1.getString().endsWith(creation1 + ";\n") && (r =
                    objectCreation1.makeReplacementForReturnedArgument(
                        replacementInfo.getArgumentizedString2())) != null) {
                    replacementInfo.addReplacement(r);
                    return replacementInfo.getReplacements();
                }
            }
        }
        if (variableDeclarationWithArrayInitializer1 != null && invocationCoveringTheEntireStatement2 != null && variableDeclarations2.isEmpty() &&
            !containsMethodSignatureOfAnonymousClass(
                statement1.getString()) && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
            String args1 = s1.substring(s1.indexOf("{") + 1, s1.lastIndexOf("}"));
            String args2 = s2.substring(s2.indexOf("(") + 1, s2.lastIndexOf(")"));
            if (args1.equals(args2)) {
                r = new Replacement(args1, args2,
                    Replacement.ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS);
                replacementInfo.addReplacement(r);
                return replacementInfo.getReplacements();
            }
        }
        if (variableDeclarationWithArrayInitializer2 != null && invocationCoveringTheEntireStatement1 != null && variableDeclarations1.isEmpty() &&
            !containsMethodSignatureOfAnonymousClass(
                statement1.getString()) && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
            String args1 = s1.substring(s1.indexOf("(") + 1, s1.lastIndexOf(")"));
            String args2 = s2.substring(s2.indexOf("{") + 1, s2.lastIndexOf("}"));
            if (args1.equals(args2)) {
                r = new Replacement(args1, args2,
                    Replacement.ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS);
                replacementInfo.addReplacement(r);
                return replacementInfo.getReplacements();
            }
        }

        if (invocationCoveringTheEntireStatement2 != null && statement2.getString().equals(
            invocationCoveringTheEntireStatement2.actualString() + "\n") &&
            invocationCoveringTheEntireStatement2.getArguments().size() == 1 && statement1.getString().endsWith(
            "=" + invocationCoveringTheEntireStatement2.getArguments().get(0) + "\n") &&
            invocationCoveringTheEntireStatement2.expressionIsNullOrThis() && invocationCoveringTheEntireStatement2.getName().startsWith(
            "set")) {
            String prefix1 = statement1.getString().substring(0, statement1.getString().lastIndexOf("="));
            if (variables1.contains(prefix1)) {
                String before = prefix1 + "=" + invocationCoveringTheEntireStatement2.getArguments().get(0);
                String after = invocationCoveringTheEntireStatement2.actualString();
                r = new Replacement(before, after,
                    Replacement.ReplacementType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER_METHOD_INVOCATION);
                replacementInfo.addReplacement(r);
                return replacementInfo.getReplacements();
            }
        }
        return null;
    }

    private Replacement getReplacementFromArrayCreation(List<VariableDeclaration> variableDeclarations1,
                                                        List<VariableDeclaration> variableDeclarations2,
                                                        ObjectCreation creationCoveringTheEntireStatement1,
                                                        ObjectCreation creationCoveringTheEntireStatement2) {
        Replacement r = null;
        VariableDeclaration v1 = variableDeclarations1.get(0);
        VariableDeclaration v2 = variableDeclarations2.get(0);
        String initializer1 = v1.getInitializer() != null ? v1.getInitializer().getString() : null;
        String initializer2 = v2.getInitializer() != null ? v2.getInitializer().getString() : null;
        if (v1.getType().getArrayDimension() == 1 && v2.getType().containsTypeArgument(v1.getType().getClassType()) &&
            creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() &&
            initializer1 != null && initializer2 != null &&
            initializer1.substring(initializer1.indexOf("[") + 1, initializer1.lastIndexOf("]")).equals(
                initializer2.substring(initializer2.indexOf("(") + 1, initializer2.lastIndexOf(")")))) {
            r = new ObjectCreationReplacement(initializer1, initializer2,
                creationCoveringTheEntireStatement1,
                creationCoveringTheEntireStatement2,
                Replacement.ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
        }
        if (v2.getType().getArrayDimension() == 1 && v1.getType().containsTypeArgument(
            v2.getType().getClassType()) &&
            !creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() &&
            initializer1 != null && initializer2 != null &&
            initializer1.substring(initializer1.indexOf("(") + 1, initializer1.lastIndexOf(")")).equals(
                initializer2.substring(initializer2.indexOf("[") + 1, initializer2.lastIndexOf("]")))) {
            r = new ObjectCreationReplacement(initializer1, initializer2,
                creationCoveringTheEntireStatement1,
                creationCoveringTheEntireStatement2,
                Replacement.ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
        }
        return r;
    }

    private List<UMLOperationBodyMapper> getLambdaMappers(List<LambdaExpressionObject> lambdas1,
                                                          List<LambdaExpressionObject> lambdas2) throws
        RefactoringMinerTimedOutException {
        List<UMLOperationBodyMapper> lambdaMappers = new ArrayList<>();
        if (!lambdas1.isEmpty() && !lambdas2.isEmpty()) {
            for (LambdaExpressionObject lambdaExpressionObject : lambdas1) {
                for (LambdaExpressionObject expressionObject : lambdas2) {
                    UMLOperationBodyMapper mapper =
                        new UMLOperationBodyMapper(lambdaExpressionObject, expressionObject, this);
                    int mappings = mapper.mappingsWithoutBlocks();
                    if (mappings > 0) {
                        int nonMappedElementsT1 = mapper.nonMappedElementsT1();
                        int nonMappedElementsT2 = mapper.nonMappedElementsT2();
                        if (mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) {
                            this.mappings.addAll(mapper.mappings);
                            this.nonMappedInnerNodesT1.addAll(mapper.nonMappedInnerNodesT1);
                            this.nonMappedInnerNodesT2.addAll(mapper.nonMappedInnerNodesT2);
                            this.nonMappedLeavesT1.addAll(mapper.nonMappedLeavesT1);
                            this.nonMappedLeavesT2.addAll(mapper.nonMappedLeavesT2);
                            this.refactorings.addAll(mapper.getRefactorings());
                            lambdaMappers.add(mapper);
                        }
                    }
                }
            }
        }
        return lambdaMappers;
    }

    private boolean cast(String diff1, String diff2) {
        return (diff1.isEmpty() && diff2.startsWith("(") && diff2.endsWith(")")) || diff2.equals("(" + diff1 + ")");
    }

    private boolean differOnlyInCastExpressionOrPrefixOperator(String s1, String s2, ReplacementInfo info) {
        String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(s1, s2);
        String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
        if (!commonPrefix.isEmpty() && !commonSuffix.isEmpty()) {
            int beginIndexS1 = s1.indexOf(commonPrefix) + commonPrefix.length();
            int endIndexS1 = s1.lastIndexOf(commonSuffix);
            String diff1 = beginIndexS1 > endIndexS1 ? "" : s1.substring(beginIndexS1, endIndexS1);
            int beginIndexS2 = s2.indexOf(commonPrefix) + commonPrefix.length();
            int endIndexS2 = s2.lastIndexOf(commonSuffix);
            String diff2 = beginIndexS2 > endIndexS2 ? "" : s2.substring(beginIndexS2, endIndexS2);
            if (cast(diff1, diff2)) {
                return true;
            }
            if (cast(diff2, diff1)) {
                return true;
            }
            if (diff1.isEmpty() && (diff2.equals("!") || diff2.equals("~"))) {
                Replacement r = new Replacement(s1, s2, Replacement.ReplacementType.INVERT_CONDITIONAL);
                info.addReplacement(r);
                return true;
            }
            if (diff2.isEmpty() && (diff1.equals("!") || diff1.equals("~"))) {
                Replacement r = new Replacement(s1, s2, Replacement.ReplacementType.INVERT_CONDITIONAL);
                info.addReplacement(r);
                return true;
            }
        }
        return false;
    }


    private boolean equalAfterInfixExpressionExpansion(String s1,
                                                       String s2,
                                                       ReplacementInfo replacementInfo,
                                                       List<String> infixExpressions1) {
        Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<>();
        Set<Replacement> replacementsToBeAdded = new LinkedHashSet<>();
        String originalArgumentizedString1 = replacementInfo.getArgumentizedString1();
        for (Replacement replacement : replacementInfo.getReplacements()) {
            String before = replacement.getBefore();
            for (String infixExpression1 : infixExpressions1) {
                if (infixExpression1.startsWith(before)) {
                    String suffix = infixExpression1.substring(before.length());
                    String after = replacement.getAfter();
                    if (s1.contains(after + suffix)) {
                        String temp =
                            ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), after + suffix,
                                after);
                        int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
                        if (distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
                            replacementsToBeRemoved.add(replacement);
                            Replacement newReplacement =
                                new Replacement(infixExpression1, after, Replacement.ReplacementType.INFIX_EXPRESSION);
                            replacementsToBeAdded.add(newReplacement);
                            replacementInfo.setArgumentizedString1(temp);
                        }
                    }
                }
            }
        }
        if (replacementInfo.getRawDistance() == 0) {
            replacementInfo.removeReplacements(replacementsToBeRemoved);
            replacementInfo.addReplacements(replacementsToBeAdded);
            return true;
        } else {
            replacementInfo.setArgumentizedString1(originalArgumentizedString1);
            return false;
        }
    }

    private String prepareConditional(String s) {
        String conditional = s;
        if (s.startsWith("if (") && s.endsWith(")")) {
            conditional = s.substring(3, s.length() - 1);
        }
        if (s.startsWith("while (") && s.endsWith(")")) {
            conditional = s.substring(6, s.length() - 1);
        }
        if (s.startsWith("return ") && s.endsWith(";\n")) {
            conditional = s.substring(7, s.length() - 2);
        }
        int indexOfEquals = s.indexOf("=");
        if (indexOfEquals > -1 && s.charAt(indexOfEquals + 1) != '=' && s.charAt(
            indexOfEquals - 1) != '!' && s.endsWith(";\n")) {
            conditional = s.substring(indexOfEquals + 1, s.length() - 2);
        }
        return conditional;
    }

    private boolean commonConditional(String s1, String s2, ReplacementInfo info) {
        if (!containsMethodSignatureOfAnonymousClass(s1) && !containsMethodSignatureOfAnonymousClass(s2)) {
            if ((s1.contains("||") || s1.contains("&&") || s2.contains("||") || s2.contains("&&"))) {
                String conditional1 = prepareConditional(s1);
                String conditional2 = prepareConditional(s2);
                String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(conditional1);
                String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(conditional2);
                List<String> subConditionsAsList1 = new ArrayList<>();
                for (String s : subConditions1) {
                    subConditionsAsList1.add(s.trim());
                }
                List<String> subConditionsAsList2 = new ArrayList<>();
                for (String s : subConditions2) {
                    subConditionsAsList2.add(s.trim());
                }
                Set<String> intersection = new LinkedHashSet<>(subConditionsAsList1);
                intersection.retainAll(subConditionsAsList2);
                int matches = 0;
                if (!intersection.isEmpty()) {
                    for (String element : intersection) {
                        boolean replacementFound = false;
                        for (Replacement r : info.getReplacements()) {
                            if (element.equals(r.getAfter()) || element.equals("(" + r.getAfter()) || element.equals(
                                r.getAfter() + ")")) {
                                replacementFound = true;
                                break;
                            }
                            if (element.equals("!" + r.getAfter())) {
                                replacementFound = true;
                                break;
                            }
                            if (r.getType().equals(Replacement.ReplacementType.INFIX_OPERATOR) && element.contains(r.getAfter())) {
                                replacementFound = true;
                                break;
                            }
                            if (ReplacementUtil.contains(element, r.getAfter()) && element.startsWith(r.getAfter()) &&
                                (element.endsWith(" != Null") || element.endsWith(" == Null"))) {
                                replacementFound = true;
                                break;
                            }
                        }
                        if (!replacementFound) {
                            matches++;
                        }
                    }
                }
                if (matches > 0) {
                    Replacement r = new IntersectionReplacement(s1, s2, intersection, Replacement.ReplacementType.CONDITIONAL);
                    info.addReplacement(r);
                }
                boolean invertConditionalFound = false;
                for (String subCondition1 : subConditionsAsList1) {
                    for (String subCondition2 : subConditionsAsList2) {
                        if (subCondition1.equals("!" + subCondition2)) {
                            Replacement r =
                                new Replacement(subCondition1, subCondition2, Replacement.ReplacementType.INVERT_CONDITIONAL);
                            info.addReplacement(r);
                            invertConditionalFound = true;
                        }
                        if (subCondition2.equals("!" + subCondition1)) {
                            Replacement r =
                                new Replacement(subCondition1, subCondition2, Replacement.ReplacementType.INVERT_CONDITIONAL);
                            info.addReplacement(r);
                            invertConditionalFound = true;
                        }
                    }
                }
                if (invertConditionalFound || matches > 0) {
                    return true;
                }
            }
            if (s1.contains(" >= ") && s2.contains(" <= ")) {
                Replacement r = invertConditionalDirection(s1, s2, " >= ", " <= ");
                if (r != null) {
                    info.addReplacement(r);
                    return true;
                }
            }
            if (s1.contains(" <= ") && s2.contains(" >= ")) {
                Replacement r = invertConditionalDirection(s1, s2, " <= ", " >= ");
                if (r != null) {
                    info.addReplacement(r);
                    return true;
                }
            }
            if (s1.contains(" > ") && s2.contains(" < ")) {
                Replacement r = invertConditionalDirection(s1, s2, " > ", " < ");
                if (r != null) {
                    info.addReplacement(r);
                    return true;
                }
            }
            if (s1.contains(" < ") && s2.contains(" > ")) {
                Replacement r = invertConditionalDirection(s1, s2, " < ", " > ");
                if (r != null) {
                    info.addReplacement(r);
                    return true;
                }
            }
        }
        return false;
    }

    private Replacement invertConditionalDirection(String s1, String s2, String operator1, String operator2) {
        int indexS1 = s1.indexOf(operator1);
        int indexS2 = s2.indexOf(operator2);
        //s1 goes right, s2 goes left
        int i = indexS1 + operator1.length();
        int j = indexS2 - 1;
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        while (i < s1.length() && j >= 0) {
            sb1.append(s1.charAt(i));
            sb2.insert(0, s2.charAt(j));
            if (sb1.toString().equals(sb2.toString())) {
                String subCondition1 = operator1 + sb1;
                String subCondition2 = sb2 + operator2;
                return new Replacement(subCondition1, subCondition2, Replacement.ReplacementType.INVERT_CONDITIONAL);
            }
            i++;
            j--;
        }
        //s1 goes left, s2 goes right
        i = indexS1 - 1;
        j = indexS2 + operator2.length();
        sb1 = new StringBuilder();
        sb2 = new StringBuilder();
        while (i >= 0 && j < s2.length()) {
            sb1.insert(0, s1.charAt(i));
            sb2.append(s2.charAt(j));
            if (sb1.toString().equals(sb2.toString())) {
                String subCondition1 = sb1 + operator1;
                String subCondition2 = operator2 + sb2;
                return new Replacement(subCondition1, subCondition2, Replacement.ReplacementType.INVERT_CONDITIONAL);
            }
            i--;
            j++;
        }
        return null;
    }

    private boolean oneIsVariableDeclarationTheOtherIsVariableAssignment(String s1,
                                                                         String s2,
                                                                         ReplacementInfo replacementInfo) {
        String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
        if (s1.contains("=") && s2.contains("=") && (s1.equals(commonSuffix) || s2.equals(commonSuffix))) {
            if (replacementInfo.getReplacements().size() == 2) {
                StringBuilder sb = new StringBuilder();
                int counter = 0;
                for (Replacement r : replacementInfo.getReplacements()) {
                    sb.append(r.getAfter());
                    if (counter == 0) {
                        sb.append("=");
                    } else if (counter == 1) {
                        sb.append(";\n");
                    }
                    counter++;
                }
                return !commonSuffix.equals(sb.toString());
            }
            return true;
        }
        return false;
    }

    private boolean oneIsVariableDeclarationTheOtherIsReturnStatement(String s1, String s2) {
        String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
        if (!commonSuffix.equals("Null\n") && !commonSuffix.equals("True\n") && !commonSuffix.equals(
            "False\n") && !commonSuffix.equals("0\n")) {
            if (s1.startsWith("return ") && s1.substring(7).equals(commonSuffix) &&
                s2.contains("=") && s2.substring(s2.indexOf("=") + 1).equals(commonSuffix)) {
                return true;
            }
            return s2.startsWith("return ") && s2.substring(7).equals(commonSuffix) &&
                s1.contains("=") && s1.substring(s1.indexOf("=") + 1).equals(commonSuffix);
        }
        return false;
    }

    private boolean identicalVariableDeclarationsWithDifferentNames(String s1,
                                                                    String s2,
                                                                    List<VariableDeclaration> variableDeclarations1,
                                                                    List<VariableDeclaration> variableDeclarations2,
                                                                    ReplacementInfo replacementInfo) {
        if (variableDeclarations1.size() == variableDeclarations2.size() && variableDeclarations1.size() == 1) {
            VariableDeclaration declaration1 = variableDeclarations1.get(0);
            VariableDeclaration declaration2 = variableDeclarations2.get(0);
            if (!declaration1.getVariableName().equals(declaration2.getVariableName())) {
                String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
                String composedString1 = declaration1.getType() + " " + declaration1.getVariableName() + commonSuffix;
                String composedString2 = declaration2.getType() + " " + declaration2.getVariableName() + commonSuffix;
                if (s1.equals(composedString1) && s2.equals(composedString2)) {
                    Replacement replacement =
                        new Replacement(declaration1.getVariableName(), declaration2.getVariableName(),
                            Replacement.ReplacementType.VARIABLE_NAME);
                    replacementInfo.addReplacement(replacement);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsValidOperatorReplacements(ReplacementInfo replacementInfo) {
        List<Replacement> operatorReplacements = replacementInfo.getReplacements(Replacement.ReplacementType.INFIX_OPERATOR);
        for (Replacement replacement : operatorReplacements) {
            if (replacement.getBefore().equals("==") && !replacement.getAfter().equals("!="))
                return false;
            if (replacement.getBefore().equals("!=") && !replacement.getAfter().equals("=="))
                return false;
            if (replacement.getBefore().equals("&&") && !replacement.getAfter().equals("||"))
                return false;
            if (replacement.getBefore().equals("||") && !replacement.getAfter().equals("&&"))
                return false;
        }
        return true;
    }


    private Replacement getReplacement(AbstractCodeFragment statement1,
                                       AbstractCodeFragment statement2,
                                       Map<String, String> parameterToArgumentMap,
                                       Set<String> variables1,
                                       Set<String> variables2,
                                       Map<String, List<? extends AbstractCall>> methodInvocationMap1,
                                       Map<String, List<? extends AbstractCall>> methodInvocationMap2,
                                       Set<String> methodInvocations1,
                                       Set<String> methodInvocations2,
                                       String s1,
                                       String s2) {
        Replacement replacement = null;
        if (variables1.contains(s1) && variables2.contains(s2) &&
            variablesStartWithSameCase(s1, s2, parameterToArgumentMap)) {
            replacement = new Replacement(s1, s2, Replacement.ReplacementType.VARIABLE_NAME);
            if (s1.startsWith("(") && s2.startsWith("(") && s1.contains(")") && s2.contains(")")) {
                String prefix1 = s1.substring(0, s1.indexOf(")") + 1);
                String prefix2 = s2.substring(0, s2.indexOf(")") + 1);
                if (prefix1.equals(prefix2)) {
                    String suffix1 = s1.substring(prefix1.length());
                    String suffix2 = s2.substring(prefix2.length());
                    replacement = new Replacement(suffix1, suffix2, Replacement.ReplacementType.VARIABLE_NAME);
                }
            }
            VariableDeclaration v1 = statement1.searchVariableDeclaration(s1);
            VariableDeclaration v2 = statement2.searchVariableDeclaration(s2);
            if (inconsistentVariableMappingCount(statement1, statement2, v1, v2) > 1 &&
                operation2.loopWithVariables(v1.getVariableName(), v2.getVariableName()) == null) {
                replacement = null;
            }
        } else if (variables1.contains(s1) && methodInvocations2.contains(s2)) {
            OperationInvocation invokedOperationAfter =
                (OperationInvocation) methodInvocationMap2.get(s2).get(0);
            replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationAfter,
                Direction.VARIABLE_TO_INVOCATION);
        } else if (methodInvocations1.contains(s1) && methodInvocations2.contains(s2)) {
            OperationInvocation invokedOperationBefore =
                (OperationInvocation) methodInvocationMap1.get(s1).get(0);
            OperationInvocation invokedOperationAfter =
                (OperationInvocation) methodInvocationMap2.get(s2).get(0);
            if (invokedOperationBefore.compatibleExpression(invokedOperationAfter)) {
                replacement = new MethodInvocationReplacement(s1, s2, invokedOperationBefore,
                    invokedOperationAfter,
                    Replacement.ReplacementType.METHOD_INVOCATION);
            }
        } else if (methodInvocations1.contains(s1) && variables2.contains(s2)) {
            OperationInvocation invokedOperationBefore =
                (OperationInvocation) methodInvocationMap1.get(s1).get(0);
            replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationBefore,
                Direction.INVOCATION_TO_VARIABLE);
        }
        return replacement;
    }

    private int getCommonArgumentsCount(OperationInvocation invocationCoveringTheEntireStatement1,
                                        Map<String, List<? extends AbstractCall>> methodInvocationMap1,
                                        ObjectCreation creationCoveringTheEntireStatement2) {
        int commonArguments = 0;
        for (String key1 : methodInvocationMap1.keySet()) {
            if (invocationCoveringTheEntireStatement1.actualString().startsWith(key1)) {
                for (AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
                    Set<String> argumentIntersection =
                        invocation1.argumentIntersection(creationCoveringTheEntireStatement2);
                    commonArguments += argumentIntersection.size();
                }
            }
        }
        return commonArguments;
    }

    private boolean commonConcat(String s1, String s2, ReplacementInfo info) {
        if (s1.contains("+") && s2.contains("+") && !s1.contains("++") && !s2.contains("++") &&
            !containsMethodSignatureOfAnonymousClass(s1) && !containsMethodSignatureOfAnonymousClass(s2)) {
            Set<String> tokens1 = new LinkedHashSet<>(Arrays.asList(s1.split(SPLIT_CONCAT_STRING_PATTERN)));
            Set<String> tokens2 = new LinkedHashSet<>(Arrays.asList(s2.split(SPLIT_CONCAT_STRING_PATTERN)));
            Set<String> intersection = new LinkedHashSet<>(tokens1);
            intersection.retainAll(tokens2);
            Set<String> filteredIntersection = new LinkedHashSet<>();
            for (String common : intersection) {
                boolean foundInReplacements = false;
                for (Replacement r : info.replacements) {
                    if (r.getBefore().contains(common) || r.getAfter().contains(common)) {
                        foundInReplacements = true;
                        break;
                    }
                }
                if (!foundInReplacements) {
                    filteredIntersection.add(common);
                }
            }
            int size = filteredIntersection.size();
            int threshold = Math.max(tokens1.size(), tokens2.size()) - size;
            if ((size > 0 && size > threshold) || (size > 1 && size >= threshold)) {
                IntersectionReplacement r =
                    new IntersectionReplacement(s1, s2, intersection, Replacement.ReplacementType.CONCATENATION);
                info.getReplacements().add(r);
                return true;
            }
        }
        return false;
    }

    private boolean validStatementForConcatComparison(AbstractCodeFragment statement1,
                                                      AbstractCodeFragment statement2) {
        List<VariableDeclaration> variableDeclarations1 = statement1.getVariableDeclarations();
        List<VariableDeclaration> variableDeclarations2 = statement2.getVariableDeclarations();
        if (variableDeclarations1.size() == variableDeclarations2.size()) {
            return true;
        } else {
            if (variableDeclarations1.size() > 0 && variableDeclarations2.size() == 0 && statement2.getString().startsWith(
                "return ")) {
                return true;
            } else return variableDeclarations1.size() == 0 && variableDeclarations2.size() > 0
                && statement1.getString().startsWith("return ");
        }
    }

    private boolean equalAfterNewArgumentAdditions(String s1, String s2, ReplacementInfo replacementInfo) {
        UMLOperationDiff operationDiff = classDiff != null ? classDiff.getOperationDiff(operation1, operation2) : null;
        if (operationDiff == null) {
            operationDiff = new UMLOperationDiff(operation1, operation2);
        }
        String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(s1, s2);
        String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
        if (!commonPrefix.isEmpty() && !commonSuffix.isEmpty() && !commonPrefix.equals("return ")) {
            int beginIndexS1 = s1.indexOf(commonPrefix) + commonPrefix.length();
            int endIndexS1 = s1.lastIndexOf(commonSuffix);
            String diff1 = beginIndexS1 > endIndexS1 ? "" : s1.substring(beginIndexS1, endIndexS1);
            int beginIndexS2 = s2.indexOf(commonPrefix) + commonPrefix.length();
            int endIndexS2 = s2.lastIndexOf(commonSuffix);
            String diff2 = beginIndexS2 > endIndexS2 ? "" : s2.substring(beginIndexS2, endIndexS2);
            if (beginIndexS1 > endIndexS1) {
                diff2 = diff2 + commonSuffix.substring(0, beginIndexS1 - endIndexS1);
                if (diff2.charAt(diff2.length() - 1) == ',') {
                    diff2 = diff2.substring(0, diff2.length() - 1);
                }
            }
            String characterAfterCommonPrefix =
                s1.equals(commonPrefix) ? "" : Character.toString(s1.charAt(commonPrefix.length()));
            if (commonPrefix.contains(",") && commonPrefix.lastIndexOf(",") < commonPrefix.length() - 1 &&
                !characterAfterCommonPrefix.equals(",") && !characterAfterCommonPrefix.equals(")")) {
                String prepend = commonPrefix.substring(commonPrefix.lastIndexOf(",") + 1);
                diff1 = prepend + diff1;
                diff2 = prepend + diff2;
            }
            //if there is a variable replacement diff1 should be empty, otherwise diff1 should include a single variable
            if (diff1.isEmpty() ||
                (operation1.getParameterNameList().contains(diff1) && !operation2.getParameterNameList().contains(
                    diff1) && !containsMethodSignatureOfAnonymousClass(diff2)) ||
                (classDiff != null && classDiff.getOriginalClass().containsAttributeWithName(
                    diff1) && !classDiff.getNextClass().containsAttributeWithName(
                    diff1) && !containsMethodSignatureOfAnonymousClass(diff2))) {
                List<UMLParameter> matchingAddedParameters = new ArrayList<>();
                for (UMLParameter addedParameter : operationDiff.getAddedParameters()) {
                    if (diff2.contains(addedParameter.getName())) {
                        matchingAddedParameters.add(addedParameter);
                    }
                }
                if (matchingAddedParameters.size() > 0) {
                    Replacement matchingReplacement = getMatchingReplacement(replacementInfo, operationDiff);
                    if (matchingReplacement != null) {
                        Set<String> splitVariables = new LinkedHashSet<>();
                        splitVariables.add(matchingReplacement.getAfter());
                        StringBuilder concat = new StringBuilder();
                        int counter = 0;
                        for (UMLParameter addedParameter : matchingAddedParameters) {
                            splitVariables.add(addedParameter.getName());
                            concat.append(addedParameter.getName());
                            if (counter < matchingAddedParameters.size() - 1) {
                                concat.append(",");
                            }
                            counter++;
                        }
                        SplitVariableReplacement split =
                            new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
                        if (!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
                            replacementInfo.getReplacements().remove(matchingReplacement);
                            replacementInfo.getReplacements().add(split);
                            return true;
                        }
                    } else if (diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
                        Set<String> addedVariables = new LinkedHashSet<>();
                        StringBuilder concat = new StringBuilder();
                        int counter = 0;
                        for (UMLParameter addedParameter : matchingAddedParameters) {
                            addedVariables.add(addedParameter.getName());
                            concat.append(addedParameter.getName());
                            if (counter < matchingAddedParameters.size() - 1) {
                                concat.append(",");
                            }
                            counter++;
                        }
                        if (concat.toString().equals(diff2)) {
                            AddVariableReplacement r = new AddVariableReplacement(addedVariables);
                            replacementInfo.getReplacements().add(r);
                            return true;
                        }
                    }
                    if (operation1.getParameterNameList().contains(diff1)) {
                        Set<String> splitVariables = new LinkedHashSet<>();
                        StringBuilder concat = new StringBuilder();
                        int counter = 0;
                        for (UMLParameter addedParameter : matchingAddedParameters) {
                            splitVariables.add(addedParameter.getName());
                            concat.append(addedParameter.getName());
                            if (counter < matchingAddedParameters.size() - 1) {
                                concat.append(",");
                            }
                            counter++;
                        }
                        SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
                        if (!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
                            replacementInfo.getReplacements().add(split);
                            return true;
                        }
                    }
                }
                if (classDiff != null) {
                    List<UMLAttribute> matchingAttributes = new ArrayList<>();
                    for (UMLAttribute attribute : classDiff.getNextClass().getAttributes()) {
                        if (diff2.contains(attribute.getName())) {
                            matchingAttributes.add(attribute);
                        }
                    }
                    if (matchingAttributes.size() > 0) {
                        Replacement matchingReplacement = null;
                        for (Replacement replacement : replacementInfo.getReplacements()) {
                            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                                if (classDiff.getOriginalClass().containsAttributeWithName(replacement.getBefore()) &&
                                    classDiff.getNextClass().containsAttributeWithName(replacement.getAfter())) {
                                    matchingReplacement = replacement;
                                    break;
                                }
                            }
                        }
                        if (matchingReplacement != null) {
                            Set<String> splitVariables = new LinkedHashSet<>();
                            splitVariables.add(matchingReplacement.getAfter());
                            StringBuilder concat = new StringBuilder();
                            int counter = 0;
                            for (UMLAttribute attribute : matchingAttributes) {
                                splitVariables.add(attribute.getName());
                                concat.append(attribute.getName());
                                if (counter < matchingAttributes.size() - 1) {
                                    concat.append(",");
                                }
                                counter++;
                            }
                            SplitVariableReplacement split =
                                new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
                            if (!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(
                                diff2)) {
                                replacementInfo.getReplacements().remove(matchingReplacement);
                                replacementInfo.getReplacements().add(split);
                                return true;
                            }
                        } else if (diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
                            Set<String> addedVariables = new LinkedHashSet<>();
                            StringBuilder concat = new StringBuilder();
                            int counter = 0;
                            for (UMLAttribute attribute : matchingAttributes) {
                                addedVariables.add(attribute.getName());
                                concat.append(attribute.getName());
                                if (counter < matchingAttributes.size() - 1) {
                                    concat.append(",");
                                }
                                counter++;
                            }
                            if (concat.toString().equals(diff2)) {
                                AddVariableReplacement r = new AddVariableReplacement(addedVariables);
                                replacementInfo.getReplacements().add(r);
                                return true;
                            }
                        }
                        if (classDiff.getOriginalClass().containsAttributeWithName(diff1)) {
                            Set<String> splitVariables = new LinkedHashSet<>();
                            StringBuilder concat = new StringBuilder();
                            int counter = 0;
                            for (UMLAttribute attribute : matchingAttributes) {
                                splitVariables.add(attribute.getName());
                                concat.append(attribute.getName());
                                if (counter < matchingAttributes.size() - 1) {
                                    concat.append(",");
                                }
                                counter++;
                            }
                            SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
                            if (!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(
                                diff2)) {
                                replacementInfo.getReplacements().add(split);
                                return true;
                            }
                        }
                    }
                }
                List<VariableDeclaration> matchingVariableDeclarations = new ArrayList<>();
                for (VariableDeclaration declaration : operation2.getAllVariableDeclarations()) {
                    if (diff2.contains(declaration.getVariableName())) {
                        matchingVariableDeclarations.add(declaration);
                    }
                }
                if (matchingVariableDeclarations.size() > 0) {
                    Replacement matchingReplacement = null;
                    for (Replacement replacement : replacementInfo.getReplacements()) {
                        if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                            int indexOf1 = s1.indexOf(replacement.getAfter());
                            int indexOf2 = s2.indexOf(replacement.getAfter());
                            int characterIndex1 = indexOf1 + replacement.getAfter().length();
                            int characterIndex2 = indexOf2 + replacement.getAfter().length();
                            boolean isVariableDeclarationReplacement =
                                characterIndex1 < s1.length() && s1.charAt(characterIndex1) == '=' &&
                                    characterIndex2 < s2.length() && s2.charAt(characterIndex2) == '=';
                            if (!isVariableDeclarationReplacement &&
                                operation1.getVariableDeclaration(replacement.getBefore()) != null &&
                                operation2.getVariableDeclaration(replacement.getAfter()) != null) {
                                matchingReplacement = replacement;
                                break;
                            }
                        }
                    }
                    if (matchingReplacement != null) {
                        Set<String> splitVariables = new LinkedHashSet<>();
                        splitVariables.add(matchingReplacement.getAfter());
                        StringBuilder concat = new StringBuilder();
                        int counter = 0;
                        for (VariableDeclaration declaration : matchingVariableDeclarations) {
                            splitVariables.add(declaration.getVariableName());
                            concat.append(declaration.getVariableName());
                            if (counter < matchingVariableDeclarations.size() - 1) {
                                concat.append(",");
                            }
                            counter++;
                        }
                        SplitVariableReplacement split =
                            new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
                        if (!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
                            replacementInfo.getReplacements().remove(matchingReplacement);
                            replacementInfo.getReplacements().add(split);
                            return true;
                        }
                    } else if (diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
                        Set<String> addedVariables = new LinkedHashSet<>();
                        StringBuilder concat = new StringBuilder();
                        int counter = 0;
                        for (VariableDeclaration declaration : matchingVariableDeclarations) {
                            addedVariables.add(declaration.getVariableName());
                            concat.append(declaration.getVariableName());
                            if (counter < matchingVariableDeclarations.size() - 1) {
                                concat.append(",");
                            }
                            counter++;
                        }
                        if (concat.toString().equals(diff2)) {
                            AddVariableReplacement r = new AddVariableReplacement(addedVariables);
                            replacementInfo.getReplacements().add(r);
                            return true;
                        }
                    }
                    if (operation1.getVariableDeclaration(diff1) != null) {
                        Set<String> splitVariables = new LinkedHashSet<>();
                        StringBuilder concat = new StringBuilder();
                        int counter = 0;
                        for (VariableDeclaration declaration : matchingVariableDeclarations) {
                            splitVariables.add(declaration.getVariableName());
                            concat.append(declaration.getVariableName());
                            if (counter < matchingVariableDeclarations.size() - 1) {
                                concat.append(",");
                            }
                            counter++;
                        }
                        SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
                        if (!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
                            replacementInfo.getReplacements().add(split);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Replacement getMatchingReplacement(ReplacementInfo replacementInfo,
                                               UMLOperationDiff operationDiff) {
        Replacement matchingReplacement = null;
        for (Replacement replacement : replacementInfo.getReplacements()) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                for (UMLParameterDiff parameterDiff : operationDiff.getParameterDiffList()) {
                    if (parameterDiff.isNameChanged() &&
                        replacement.getBefore().equals(parameterDiff.getRemovedParameter().getName()) &&
                        replacement.getAfter().equals(parameterDiff.getAddedParameter().getName())) {
                        matchingReplacement = replacement;
                        break;
                    }
                }
            }
            if (matchingReplacement != null) {
                break;
            }
        }
        return matchingReplacement;
    }

    private boolean classInstanceCreationWithEverythingReplaced(AbstractCodeFragment statement1,
                                                                AbstractCodeFragment statement2,
                                                                ReplacementInfo replacementInfo,
                                                                Map<String, String> parameterToArgumentMap) {
        String string1 = statement1.getString();
        String string2 = statement2.getString();
        if (containsMethodSignatureOfAnonymousClass(string1)) {
            string1 = string1.substring(0, string1.indexOf("\n"));
        }
        if (containsMethodSignatureOfAnonymousClass(string2)) {
            string2 = string2.substring(0, string2.indexOf("\n"));
        }
        if (string1.contains("=") && string1.endsWith("\n") && string2.startsWith("return ") && string2.endsWith(
            "\n")) {
            boolean typeReplacement = false, compatibleTypes = false, classInstanceCreationReplacement = false;
            String assignment1 = string1.substring(string1.indexOf("=") + 1, string1.lastIndexOf("\n"));
            String assignment2 = string2.substring(7, string2.lastIndexOf("\n"));
            UMLType type1 = null, type2 = null;
            ObjectCreation objectCreation1 = null, objectCreation2 = null;
            Map<String, String> argumentToParameterMap = new LinkedHashMap<>();
            Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
            for (String creation1 : creationMap1.keySet()) {
                if (creation1.equals(assignment1)) {
                    objectCreation1 = creationMap1.get(creation1).get(0);
                    type1 = objectCreation1.getType();
                }
            }
            Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
            for (String creation2 : creationMap2.keySet()) {
                if (creation2.equals(assignment2)) {
                    objectCreation2 = creationMap2.get(creation2).get(0);
                    type2 = objectCreation2.getType();
                    for (String argument : objectCreation2.getArguments()) {
                        if (parameterToArgumentMap.containsKey(argument)) {
                            argumentToParameterMap.put(parameterToArgumentMap.get(argument), argument);
                        }
                    }
                }
            }
            int minArguments = 0;
            if (type1 != null && type2 != null) {
                compatibleTypes = type1.compatibleTypes(type2);
                minArguments = Math.min(objectCreation1.getArguments().size(), objectCreation2.getArguments().size());
            }
            int replacedArguments = 0;
            for (Replacement replacement : replacementInfo.getReplacements()) {
                if (replacement.getType().equals(Replacement.ReplacementType.TYPE)) {
                    typeReplacement = true;
                    if (string1.contains(replacement.getBefore() + "(") &&
                        string2.contains(replacement.getAfter() + "("))
                        classInstanceCreationReplacement = true;
                } else if (objectCreation1 != null && objectCreation2 != null &&
                    objectCreation1.getArguments().contains(replacement.getBefore()) &&
                    (objectCreation2.getArguments().contains(
                        replacement.getAfter()) || objectCreation2.getArguments().contains(
                        argumentToParameterMap.get(replacement.getAfter())))) {
                    replacedArguments++;
                } else if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) &&
                    assignment1.equals(replacement.getBefore()) &&
                    assignment2.equals(replacement.getAfter()))
                    classInstanceCreationReplacement = true;
            }
            return typeReplacement && !compatibleTypes && replacedArguments == minArguments && classInstanceCreationReplacement;
        } else if (string1.startsWith("return ") && string1.endsWith("\n") && string2.contains(
            "=") && string2.endsWith("\n")) {
            boolean typeReplacement = false, compatibleTypes = false, classInstanceCreationReplacement = false;
            String assignment1 = string1.substring(7, string1.lastIndexOf("\n"));
            String assignment2 = string2.substring(string2.indexOf("=") + 1, string2.lastIndexOf("\n"));
            UMLType type1 = null, type2 = null;
            ObjectCreation objectCreation1 = null, objectCreation2 = null;
            Map<String, String> argumentToParameterMap = new LinkedHashMap<>();
            Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
            for (String creation1 : creationMap1.keySet()) {
                if (creation1.equals(assignment1)) {
                    objectCreation1 = creationMap1.get(creation1).get(0);
                    type1 = objectCreation1.getType();
                }
            }
            Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
            for (String creation2 : creationMap2.keySet()) {
                if (creation2.equals(assignment2)) {
                    objectCreation2 = creationMap2.get(creation2).get(0);
                    type2 = objectCreation2.getType();
                    for (String argument : objectCreation2.getArguments()) {
                        if (parameterToArgumentMap.containsKey(argument)) {
                            argumentToParameterMap.put(parameterToArgumentMap.get(argument), argument);
                        }
                    }
                }
            }
            int minArguments = 0;
            if (type1 != null && type2 != null) {
                compatibleTypes = type1.compatibleTypes(type2);
                minArguments = Math.min(objectCreation1.getArguments().size(), objectCreation2.getArguments().size());
            }
            int replacedArguments = 0;
            for (Replacement replacement : replacementInfo.getReplacements()) {
                if (replacement.getType().equals(Replacement.ReplacementType.TYPE)) {
                    typeReplacement = true;
                    if (string1.contains(replacement.getBefore() + "(")
                        && string2.contains(replacement.getAfter() + "("))
                        classInstanceCreationReplacement = true;
                } else if (objectCreation1 != null && objectCreation2 != null &&
                    objectCreation1.getArguments().contains(replacement.getBefore()) &&
                    (objectCreation2.getArguments().contains(
                        replacement.getAfter()) || objectCreation2.getArguments().contains(
                        argumentToParameterMap.get(replacement.getAfter())))) {
                    replacedArguments++;
                } else if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) &&
                    assignment1.equals(replacement.getBefore()) &&
                    assignment2.equals(replacement.getAfter()))
                    classInstanceCreationReplacement = true;
            }
            return typeReplacement && !compatibleTypes && replacedArguments == minArguments && classInstanceCreationReplacement;
        }
        return false;
    }


    private boolean variableAssignmentWithEverythingReplaced(AbstractCodeFragment statement1,
                                                             AbstractCodeFragment statement2,
                                                             ReplacementInfo replacementInfo) {
        String string1 = statement1.getString();
        String string2 = statement2.getString();
        if (containsMethodSignatureOfAnonymousClass(string1)) {
            string1 = string1.substring(0, string1.indexOf("\n"));
        }
        if (containsMethodSignatureOfAnonymousClass(string2)) {
            string2 = string2.substring(0, string2.indexOf("\n"));
        }
        if (string1.contains("=") && string1.endsWith("\n") && string2.contains("=") && string2.endsWith("\n")) {
            boolean typeReplacement = false, variableRename = false,
                classInstanceCreationReplacement = false;
            String variableName1 = string1.substring(0, string1.indexOf("="));
            String variableName2 = string2.substring(0, string2.indexOf("="));
            String assignment1 = string1.substring(string1.indexOf("=") + 1, string1.lastIndexOf("\n"));
            String assignment2 = string2.substring(string2.indexOf("=") + 1, string2.lastIndexOf("\n"));
            Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
            Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
            boolean compatibleTypes = isCompatibleTypes(assignment1, assignment2, creationMap1, creationMap2);
            OperationInvocation inv1 = null, inv2 = null;
            Map<String, List<OperationInvocation>> methodInvocationMap1 = statement1.getMethodInvocationMap();
            for (String invocation1 : methodInvocationMap1.keySet()) {
                if (invocation1.equals(assignment1)) {
                    inv1 = methodInvocationMap1.get(invocation1).get(0);
                }
            }
            Map<String, List<OperationInvocation>> methodInvocationMap2 = statement2.getMethodInvocationMap();
            for (String invocation2 : methodInvocationMap2.keySet()) {
                if (invocation2.equals(assignment2)) {
                    inv2 = methodInvocationMap2.get(invocation2).get(0);
                }
            }
            for (Replacement replacement : replacementInfo.getReplacements()) {
                if (replacement.getType().equals(Replacement.ReplacementType.TYPE)) {
                    typeReplacement = true;
                    if (string1.contains(replacement.getBefore() + "(") &&
                        string2.contains(replacement.getAfter() + "("))
                        classInstanceCreationReplacement = true;
                } else if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) &&
                    (variableName1.equals(replacement.getBefore()) || variableName1.endsWith(
                        " " + replacement.getBefore())) &&
                    (variableName2.equals(replacement.getAfter()) || variableName2.endsWith(
                        " " + replacement.getAfter())))
                    variableRename = true;
                else if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) &&
                    assignment1.equals(replacement.getBefore()) &&
                    assignment2.equals(replacement.getAfter()))
                    classInstanceCreationReplacement = true;
            }
            if (typeReplacement && !compatibleTypes && variableRename && classInstanceCreationReplacement) {
                return true;
            }
            if (variableRename && inv1 != null && inv2 != null && inv1.differentExpressionNameAndArguments(inv2)) {
                if (inv1.getArguments().size() > inv2.getArguments().size()) {
                    for (String argument : inv1.getArguments()) {
                        List<OperationInvocation> argumentInvocations = methodInvocationMap1.get(argument);
                        if (argumentInvocations != null) {
                            for (OperationInvocation argumentInvocation : argumentInvocations) {
                                if (!argumentInvocation.differentExpressionNameAndArguments(inv2)) {
                                    return false;
                                }
                            }
                        }
                    }
                } else if (inv1.getArguments().size() < inv2.getArguments().size()) {
                    for (String argument : inv2.getArguments()) {
                        List<OperationInvocation> argumentInvocations = methodInvocationMap2.get(argument);
                        if (argumentInvocations != null) {
                            for (OperationInvocation argumentInvocation : argumentInvocations) {
                                if (!inv1.differentExpressionNameAndArguments(argumentInvocation)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean isCompatibleTypes(String assignment1,
                                      String assignment2,
                                      Map<String, List<ObjectCreation>> creationMap1,
                                      Map<String, List<ObjectCreation>> creationMap2) {
        boolean compatibleTypes = false;
        UMLType type1 = null;
        UMLType type2 = null;
        for (String creation1 : creationMap1.keySet()) {
            if (creation1.equals(assignment1)) {
                type1 = creationMap1.get(creation1).get(0).getType();
            }
        }
        for (String creation2 : creationMap2.keySet()) {
            if (creation2.equals(assignment2)) {
                type2 = creationMap2.get(creation2).get(0).getType();
            }
        }
        if (type1 != null && type2 != null) {
            compatibleTypes = type1.compatibleTypes(type2);
        }
        return compatibleTypes;
    }

    private boolean isCallChain(Collection<List<? extends AbstractCall>> calls) {
        if (calls.size() > 1) {
            AbstractCall previous;
            AbstractCall current = null;
            int chainLength = 0;
            for (List<? extends AbstractCall> list : calls) {
                previous = current;
                current = list.get(0);
                if (current != null && previous != null) {
                    if (previous.getExpression() != null && previous.getExpression().equals(current.actualString())) {
                        chainLength++;
                    } else {
                        return false;
                    }
                }
            }
            return chainLength == calls.size() - 1;
        }
        return false;
    }

    private boolean variableDeclarationsWithEverythingReplaced(List<VariableDeclaration> variableDeclarations1,
                                                               List<VariableDeclaration> variableDeclarations2,
                                                               ReplacementInfo replacementInfo) {
        if (variableDeclarations1.size() == 1 && variableDeclarations2.size() == 1) {
            boolean typeReplacement = false, variableRename = false, methodInvocationReplacement = false,
                nullInitializer = false, zeroArgumentClassInstantiation = false,
                classInstantiationArgumentReplacement = false;
            UMLType type1 = variableDeclarations1.get(0).getType();
            UMLType type2 = variableDeclarations2.get(0).getType();
            AbstractExpression initializer1 = variableDeclarations1.get(0).getInitializer();
            AbstractExpression initializer2 = variableDeclarations2.get(0).getInitializer();
            if (initializer1 == null && initializer2 == null) {
                nullInitializer = true;
            } else if (initializer1 != null && initializer2 != null) {
                nullInitializer =
                    initializer1.getExpression().equals("Null") && initializer2.getExpression().equals("Null");
                if (initializer1.getCreationMap().size() == 1 && initializer2.getCreationMap().size() == 1) {
                    ObjectCreation creation1 = initializer1.getCreationMap().values().iterator().next().get(0);
                    ObjectCreation creation2 = initializer2.getCreationMap().values().iterator().next().get(0);
                    if (creation1.getArguments().size() == 0 && creation2.getArguments().size() == 0) {
                        zeroArgumentClassInstantiation = true;
                    } else if (creation1.getArguments().size() == 1 && creation2.getArguments().size() == 1) {
                        String argument1 = creation1.getArguments().get(0);
                        String argument2 = creation2.getArguments().get(0);
                        for (Replacement replacement : replacementInfo.getReplacements()) {
                            if (replacement.getBefore().equals(argument1) && replacement.getAfter().equals(argument2)) {
                                classInstantiationArgumentReplacement = true;
                                break;
                            }
                        }
                    }
                }
            }
            for (Replacement replacement : replacementInfo.getReplacements()) {
                if (replacement.getType().equals(Replacement.ReplacementType.TYPE))
                    typeReplacement = true;
                else if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) &&
                    variableDeclarations1.get(0).getVariableName().equals(replacement.getBefore()) &&
                    variableDeclarations2.get(0).getVariableName().equals(replacement.getAfter()))
                    variableRename = true;
                else if (replacement instanceof MethodInvocationReplacement) {
                    MethodInvocationReplacement invocationReplacement = (MethodInvocationReplacement) replacement;
                    if (initializer1 != null && invocationReplacement.getInvokedOperationBefore().actualString().equals(
                        initializer1.getString()) &&
                        initializer2 != null && invocationReplacement.getInvokedOperationAfter().actualString().equals(
                        initializer2.getString())) {
                        methodInvocationReplacement = true;
                    }
                    if (initializer1 != null && initializer1.getExpression().equals(replacement.getBefore()) &&
                        initializer2 != null && initializer2.getExpression().equals(replacement.getAfter())) {
                        methodInvocationReplacement = true;
                    }
                } else if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION)) {
                    if (initializer1 != null && initializer1.getExpression().equals(replacement.getBefore()) &&
                        initializer2 != null && initializer2.getExpression().equals(replacement.getAfter())) {
                        methodInvocationReplacement = true;
                    }
                }
            }
            return typeReplacement && !type1.compatibleTypes(type2) && variableRename
                && (methodInvocationReplacement || nullInitializer ||
                zeroArgumentClassInstantiation || classInstantiationArgumentReplacement);
        }
        return false;
    }

    private void replaceVariablesWithArguments(Map<String, List<? extends AbstractCall>> callMap,
                                               Set<String> calls, Map<String, String> parameterToArgumentMap) {
        if (isCallChain(callMap.values())) {
            for (String parameter : parameterToArgumentMap.keySet()) {
                String argument = parameterToArgumentMap.get(parameter);
                if (!parameter.equals(argument)) {
                    Set<String> toBeAdded = new LinkedHashSet<>();
                    for (String call : calls) {
                        String afterReplacement = ReplacementUtil.performArgumentReplacement(call, parameter, argument);
                        if (!call.equals(afterReplacement)) {
                            toBeAdded.add(afterReplacement);
                            List<? extends AbstractCall> oldCalls = callMap.get(call);
                            List<AbstractCall> newCalls = new ArrayList<>();
                            for (AbstractCall oldCall : oldCalls) {
                                AbstractCall newCall = oldCall.update(parameter, argument);
                                newCalls.add(newCall);
                            }
                            callMap.put(afterReplacement, newCalls);
                        }
                    }
                    calls.addAll(toBeAdded);
                }
            }
        } else {
            Set<String> finalNewCalls = new LinkedHashSet<>();
            for (String parameter : parameterToArgumentMap.keySet()) {
                String argument = parameterToArgumentMap.get(parameter);
                if (!parameter.equals(argument)) {
                    Set<String> toBeAdded = new LinkedHashSet<>();
                    for (String call : calls) {
                        String afterReplacement = ReplacementUtil.performArgumentReplacement(call, parameter, argument);
                        if (!call.equals(afterReplacement)) {
                            toBeAdded.add(afterReplacement);
                            List<? extends AbstractCall> oldCalls = callMap.get(call);
                            List<AbstractCall> newCalls = new ArrayList<>();
                            for (AbstractCall oldCall : oldCalls) {
                                AbstractCall newCall = oldCall.update(parameter, argument);
                                newCalls.add(newCall);
                            }
                            callMap.put(afterReplacement, newCalls);
                        }
                    }
                    finalNewCalls.addAll(toBeAdded);
                }
            }
            calls.addAll(finalNewCalls);
        }
    }

    private Set<Replacement> replacementsWithinMethodInvocations(String s1,
                                                                 String s2,
                                                                 Set<String> set1,
                                                                 Set<String> set2,
                                                                 Map<String, List<? extends AbstractCall>> methodInvocationMap,
                                                                 Direction direction) {
        Set<Replacement> replacements = new LinkedHashSet<>();
        for (String element1 : set1) {
            if (s1.contains(element1) && !s1.equals(element1) && !s1.equals("this." + element1) && !s1.equals(
                "_" + element1)) {
                int startIndex1 = s1.indexOf(element1);
                String substringBeforeIndex1 = s1.substring(0, startIndex1);
                String substringAfterIndex1 = s1.substring(startIndex1 + element1.length());
                for (String element2 : set2) {
                    if (element2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
                        element2 = element2.substring(0, element2.indexOf(substringAfterIndex1));
                    }
                    if (s2.contains(element2) && !s2.equals(element2)) {
                        int startIndex2 = s2.indexOf(element2);
                        String substringBeforeIndex2 = s2.substring(0, startIndex2);
                        String substringAfterIndex2 = s2.substring(startIndex2 + element2.length());
                        List<? extends AbstractCall> methodInvocationList = null;
                        if (direction.equals(Direction.VARIABLE_TO_INVOCATION))
                            methodInvocationList = methodInvocationMap.get(element2);
                        else if (direction.equals(Direction.INVOCATION_TO_VARIABLE))
                            methodInvocationList = methodInvocationMap.get(element1);
                        if (substringBeforeIndex1.equals(substringBeforeIndex2) && !substringAfterIndex1.isEmpty() &&
                            !substringAfterIndex2.isEmpty() && methodInvocationList != null) {
                            Replacement r = new VariableReplacementWithMethodInvocation(element1, element2,
                                (OperationInvocation) methodInvocationList.get(
                                    0), direction);
                            replacements.add(r);
                        } else if (substringAfterIndex1.equals(substringAfterIndex2) && !substringBeforeIndex1.isEmpty()
                            && !substringBeforeIndex2.isEmpty() && methodInvocationList != null) {
                            Replacement r = new VariableReplacementWithMethodInvocation(element1, element2,
                                (OperationInvocation) methodInvocationList.get(
                                    0), direction);
                            replacements.add(r);
                        }
                    }
                }
            }
        }
        return replacements;
    }

    private Replacement variableReplacementWithinMethodInvocations(String s1,
                                                                   String s2,
                                                                   Set<String> variables1,
                                                                   Set<String> variables2) {
        for (String variable1 : variables1) {
            if (s1.contains(variable1) && !s1.equals(variable1) && !s1.equals("this." + variable1) && !s1.equals(
                "_" + variable1)) {
                int startIndex1 = s1.indexOf(variable1);
                String substringBeforeIndex1 = s1.substring(0, startIndex1);
                String substringAfterIndex1 = s1.substring(startIndex1 + variable1.length());
                for (String variable2 : variables2) {
                    if (variable2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
                        variable2 = variable2.substring(0, variable2.indexOf(substringAfterIndex1));
                    }
                    if (s2.contains(variable2) && !s2.equals(variable2)) {
                        int startIndex2 = s2.indexOf(variable2);
                        String substringBeforeIndex2 = s2.substring(0, startIndex2);
                        String substringAfterIndex2 = s2.substring(startIndex2 + variable2.length());
                        if (substringBeforeIndex1.equals(substringBeforeIndex2) && substringAfterIndex1.equals(
                            substringAfterIndex2)) {
                            return new Replacement(variable1, variable2, Replacement.ReplacementType.VARIABLE_NAME);
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean equalAfterArgumentMerge(String s1, String s2, ReplacementInfo replacementInfo) {
        Map<String, Set<Replacement>> commonVariableReplacementMap = new LinkedHashMap<>();
        for (Replacement replacement : replacementInfo.getReplacements()) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                String key = replacement.getAfter();
                if (commonVariableReplacementMap.containsKey(key)) {
                    commonVariableReplacementMap.get(key).add(replacement);
                    int index = s1.indexOf(key);
                    if (index != -1) {
                        if (s1.charAt(index + key.length()) == ',') {
                            s1 = s1.substring(0, index) + s1.substring(index + key.length() + 1);
                        } else if (index > 0 && s1.charAt(index - 1) == ',') {
                            s1 = s1.substring(0, index - 1) + s1.substring(index + key.length());
                        }
                    }
                } else {
                    Set<Replacement> replacements = new LinkedHashSet<>();
                    replacements.add(replacement);
                    commonVariableReplacementMap.put(key, replacements);
                }
            }
        }
        if (s1.equals(s2)) {
            for (String key : commonVariableReplacementMap.keySet()) {
                Set<Replacement> replacements = commonVariableReplacementMap.get(key);
                if (replacements.size() > 1) {
                    replacementInfo.getReplacements().removeAll(replacements);
                    Set<String> mergedVariables = new LinkedHashSet<>();
                    for (Replacement replacement : replacements) {
                        mergedVariables.add(replacement.getBefore());
                    }
                    MergeVariableReplacement merge = new MergeVariableReplacement(mergedVariables, key);
                    replacementInfo.getReplacements().add(merge);
                }
            }
            return true;
        }
        return false;
    }

    private boolean variablesStartWithSameCase(String s1, String s2, Map<String, String> parameterToArgumentMap) {
        if (parameterToArgumentMap.containsValue(s2)) {
            return true;
        }
        if (s1.length() > 0 && s2.length() > 0) {
            if (Character.isUpperCase(s1.charAt(0)) && Character.isUpperCase(s2.charAt(0)))
                return true;
            if (Character.isLowerCase(s1.charAt(0)) && Character.isLowerCase(s2.charAt(0)))
                return true;
            if (s1.charAt(0) == '_' && s2.charAt(0) == '_')
                return true;
            return s1.charAt(0) == '(' || s2.charAt(0) == '(';
        }
        return false;
    }

    public static boolean containsMethodSignatureOfAnonymousClass(String s) {
        String[] lines = s.split("\\n");
        return s.contains(" -> ") && lines.length > 1;
/*  TODO: implement VariableReplacementAnalysis
           for(String line : lines) {
            line = VariableReplacementAnalysis.prepareLine(line);
            if(Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches()) {
                return true;
            }
        }*/
    }

    private void findReplacements(Set<String> strings1,
                                  Set<String> strings2,
                                  ReplacementInfo replacementInfo,
                                  Replacement.ReplacementType type) throws RefactoringMinerTimedOutException {
        TreeMap<Double, Set<Replacement>> globalReplacementMap = new TreeMap<>();
        TreeMap<Double, Set<Replacement>> replacementCache = new TreeMap<>();
        if (strings1.size() <= strings2.size()) {
            for (String s1 : strings1) {
                TreeMap<Double, Replacement> replacementMap = new TreeMap<>();
                for (String s2 : strings2) {
                    if (Thread.interrupted()) {
                        throw new RefactoringMinerTimedOutException();
                    }
                    boolean containsMethodSignatureOfAnonymousClass1 = containsMethodSignatureOfAnonymousClass(s1);
                    boolean containsMethodSignatureOfAnonymousClass2 = containsMethodSignatureOfAnonymousClass(s2);
                    if (containsMethodSignatureOfAnonymousClass1 != containsMethodSignatureOfAnonymousClass2 &&
                        operation1.getVariableDeclaration(s1) == null && operation2.getVariableDeclaration(
                        s2) == null) {
                        continue;
                    }
                    String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(),
                        replacementInfo.getArgumentizedString2(), s1, s2);
                    int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
                    if (distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
                        Replacement replacement = new Replacement(s1, s2, type);
                        double distancenormalized = (double) distanceRaw / (double) Math.max(temp.length(),
                            replacementInfo.getArgumentizedString2().length());
                        replacementMap.put(distancenormalized, replacement);
                        if (replacementCache.containsKey(distancenormalized)) {
                            replacementCache.get(distancenormalized).add(replacement);
                        } else {
                            Set<Replacement> r = new LinkedHashSet<>();
                            r.add(replacement);
                            replacementCache.put(distancenormalized, r);
                        }
                        if (distanceRaw == 0) {
                            break;
                        }
                    }
                }
                if (!replacementMap.isEmpty()) {
                    Double distancenormalized = replacementMap.firstEntry().getKey();
                    Replacement replacement = replacementMap.firstEntry().getValue();
                    if (globalReplacementMap.containsKey(distancenormalized)) {
                        globalReplacementMap.get(distancenormalized).add(replacement);
                    } else {
                        Set<Replacement> r = new LinkedHashSet<>();
                        r.add(replacement);
                        globalReplacementMap.put(distancenormalized, r);
                    }
                    if (distancenormalized == 0) {
                        break;
                    }
                }
            }
        } else {
            for (String s2 : strings2) {
                TreeMap<Double, Replacement> replacementMap = new TreeMap<>();
                for (String s1 : strings1) {
                    if (Thread.interrupted()) {
                        throw new RefactoringMinerTimedOutException();
                    }
                    boolean containsMethodSignatureOfAnonymousClass1 = containsMethodSignatureOfAnonymousClass(s1);
                    boolean containsMethodSignatureOfAnonymousClass2 = containsMethodSignatureOfAnonymousClass(s2);
                    if (containsMethodSignatureOfAnonymousClass1 != containsMethodSignatureOfAnonymousClass2 &&
                        operation1.getVariableDeclaration(s1) == null && operation2.getVariableDeclaration(
                        s2) == null) {
                        continue;
                    }
                    String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(),
                        replacementInfo.getArgumentizedString2(), s1, s2);
                    int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
                    if (distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
                        Replacement replacement = new Replacement(s1, s2, type);
                        double distancenormalized = (double) distanceRaw / (double) Math.max(temp.length(),
                            replacementInfo.getArgumentizedString2().length());
                        replacementMap.put(distancenormalized, replacement);
                        if (replacementCache.containsKey(distancenormalized)) {
                            replacementCache.get(distancenormalized).add(replacement);
                        } else {
                            Set<Replacement> r = new LinkedHashSet<>();
                            r.add(replacement);
                            replacementCache.put(distancenormalized, r);
                        }
                        if (distanceRaw == 0) {
                            break;
                        }
                    }
                }
                if (!replacementMap.isEmpty()) {
                    Double distancenormalized = replacementMap.firstEntry().getKey();
                    Replacement replacement = replacementMap.firstEntry().getValue();
                    if (globalReplacementMap.containsKey(distancenormalized)) {
                        globalReplacementMap.get(distancenormalized).add(replacement);
                    } else {
                        Set<Replacement> r = new LinkedHashSet<>();
                        r.add(replacement);
                        globalReplacementMap.put(distancenormalized, r);
                    }
                    if (replacementMap.firstEntry().getKey() == 0) {
                        break;
                    }
                }
            }
        }
        if (!globalReplacementMap.isEmpty()) {
            Double distancenormalized = globalReplacementMap.firstEntry().getKey();
            if (distancenormalized == 0) {
                Set<Replacement> replacements = globalReplacementMap.firstEntry().getValue();
                for (Replacement replacement : replacements) {
                    replacementInfo.addReplacement(replacement);
                    replacementInfo.setArgumentizedString1(
                        ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(),
                            replacementInfo.getArgumentizedString2(),
                            replacement.getBefore(), replacement.getAfter()));
                }
            } else {
                Set<String> processedBefores = new LinkedHashSet<>();
                for (Set<Replacement> replacements : globalReplacementMap.values()) {
                    for (Replacement replacement : replacements) {
                        if (!processedBefores.contains(replacement.getBefore())) {
                            replacementInfo.addReplacement(replacement);
                            replacementInfo.setArgumentizedString1(
                                ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(),
                                    replacementInfo.getArgumentizedString2(),
                                    replacement.getBefore(),
                                    replacement.getAfter()));
                            processedBefores.add(replacement.getBefore());
                        } else {
                            //find the next best match for replacement.getAfter() from the replacement cache
                            for (Set<Replacement> replacements2 : replacementCache.values()) {
                                for (Replacement replacement2 : replacements2) {
                                    if (replacement2.getAfter().equals(replacement.getAfter()) && !replacement2.equals(
                                        replacement)) {
                                        replacementInfo.addReplacement(replacement2);
                                        replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(
                                            replacementInfo.getArgumentizedString1(),
                                            replacementInfo.getArgumentizedString2(), replacement2.getBefore(),
                                            replacement2.getAfter()));
                                        processedBefores.add(replacement2.getBefore());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void inlinedVariableAssignment(StatementObject statement, List<StatementObject> nonMappedLeavesT2) {
        for (AbstractCodeMapping mapping : getMappings()) {
            mapping.inlinedVariableAssignment(statement, nonMappedLeavesT2, refactorings);
        }
    }

    private boolean isExpressionOfAnotherMethodInvocation(AbstractCall invocation,
                                                          Map<String, List<? extends AbstractCall>> invocationMap) {
        for (String key : invocationMap.keySet()) {
            List<? extends AbstractCall> invocations = invocationMap.get(key);
            for (AbstractCall call : invocations) {
                if (!call.equals(invocation) && call.getExpression() != null && call.getExpression().equals(
                    invocation.actualString())) {
                    for (String argument : call.getArguments()) {
                        if (invocationMap.containsKey(argument)) {
                            List<? extends AbstractCall> argumentInvocations = invocationMap.get(argument);
                            for (AbstractCall argumentCall : argumentInvocations) {
                                if (argumentCall.identicalName(invocation) && argumentCall.equalArguments(invocation)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean containsExtractOperationRefactoring(UMLOperation extractedOperation) {
        if (classDiff != null) {
            return classDiff.containsExtractOperationRefactoring(operation1, extractedOperation);
        }
        return false;
    }

    @Override
    public int compareTo(@NotNull UMLOperationBodyMapper operationBodyMapper) {
        int thisCallChainIntersectionSum = 0;
        for (AbstractCodeMapping mapping : this.mappings) {
            if (mapping instanceof LeafMapping) {
                thisCallChainIntersectionSum += ((LeafMapping) mapping).callChainIntersection().size();
            }
        }
        int otherCallChainIntersectionSum = 0;
        for (AbstractCodeMapping mapping : operationBodyMapper.mappings) {
            if (mapping instanceof LeafMapping) {
                otherCallChainIntersectionSum += ((LeafMapping) mapping).callChainIntersection().size();
            }
        }
        if (thisCallChainIntersectionSum != otherCallChainIntersectionSum) {
            return -Integer.compare(thisCallChainIntersectionSum, otherCallChainIntersectionSum);
        }
        int thisMappings = this.mappingsWithoutBlocks();
        for (AbstractCodeMapping mapping : this.getMappings()) {
            if (mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
                thisMappings++;
            }
        }
        int otherMappings = operationBodyMapper.mappingsWithoutBlocks();
        for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
            if (mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
                otherMappings++;
            }
        }
        if (thisMappings != otherMappings) {
            return -Integer.compare(thisMappings, otherMappings);
        } else {
            int thisExactMatches = this.exactMatches();
            int otherExactMatches = operationBodyMapper.exactMatches();
            if (thisExactMatches != otherExactMatches) {
                return -Integer.compare(thisExactMatches, otherExactMatches);
            } else {
                int thisEditDistance = this.editDistance();
                int otherEditDistance = operationBodyMapper.editDistance();
                if (thisEditDistance != otherEditDistance) {
                    return Integer.compare(thisEditDistance, otherEditDistance);
                } else {
                    int thisOperationNameEditDistance = this.operationNameEditDistance();
                    int otherOperationNameEditDistance = operationBodyMapper.operationNameEditDistance();
                    return Integer.compare(thisOperationNameEditDistance, otherOperationNameEditDistance);
                }
            }
        }
    }

    public int operationNameEditDistance() {
        return StringDistance.editDistance(this.operation1.getName(), this.operation2.getName());
    }

    public int exactMatches() {
        int count = 0;
        for (AbstractCodeMapping mapping : getMappings()) {
            if (mapping.isExact() && mapping.getFragment1().countableStatement() &&
                mapping.getFragment2().countableStatement() &&
                !mapping.getFragment1().getString().equals("try"))
                count++;
        }
        return count;
    }

    public int mappingsWithoutBlocks() {
        int count = 0;
        for (AbstractCodeMapping mapping : getMappings()) {
            if (mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement())
                count++;
        }
        return count;
    }

    private int editDistance() {
        int count = 0;
        for (AbstractCodeMapping mapping : getMappings()) {
            if (mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
                continue;
            }
            String s1 = preprocessInput1(mapping.getFragment1(), mapping.getFragment2());
            String s2 = preprocessInput2(mapping.getFragment1(), mapping.getFragment2());
            if (!s1.equals(s2)) {
                count += StringDistance.editDistance(s1, s2);
            }
        }
        return count;
    }

    private int inconsistentVariableMappingCount(AbstractCodeFragment statement1,
                                                 AbstractCodeFragment statement2,
                                                 VariableDeclaration v1,
                                                 VariableDeclaration v2) {
        int count = 0;
        if (v1 != null && v2 != null) {
            for (AbstractCodeMapping mapping : mappings) {
                List<VariableDeclaration> variableDeclarations1 = mapping.getFragment1().getVariableDeclarations();
                List<VariableDeclaration> variableDeclarations2 = mapping.getFragment2().getVariableDeclarations();
                if (variableDeclarations1.contains(v1) &&
                    variableDeclarations2.size() > 0 &&
                    !variableDeclarations2.contains(v2)) {
                    count++;
                }
                if (variableDeclarations2.contains(v2) &&
                    variableDeclarations1.size() > 0 &&
                    !variableDeclarations1.contains(v1)) {
                    count++;
                }
                if (mapping.isExact()) {
                    boolean containsMapping = true;
                    if (statement1 instanceof CompositeStatementObject && statement2 instanceof CompositeStatementObject &&
                        statement1.getLocationInfo().getCodeElementType().equals(
                            CodeElementType.ENHANCED_FOR_STATEMENT)) {
                        CompositeStatementObject comp1 = (CompositeStatementObject) statement1;
                        CompositeStatementObject comp2 = (CompositeStatementObject) statement2;
                        containsMapping =
                            comp1.contains(mapping.getFragment1()) && comp2.contains(mapping.getFragment2());
                    }
                }
            }
        }
        return count;
    }

    private String preprocessInput1(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
        return preprocessInput(leaf1, leaf2);
    }

    private String preprocessInput2(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
        return preprocessInput(leaf2, leaf1);
    }

    private String preprocessInput(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
        String argumentizedString = leaf1.getArgumentizedString();
        if (leaf1 instanceof StatementObject && leaf2 instanceof AbstractExpression) {
            if (argumentizedString.startsWith("return ") && argumentizedString.endsWith("\n")) {
                argumentizedString = argumentizedString.substring("return ".length(),
                    argumentizedString.lastIndexOf("\n"));
            }
        }
        return argumentizedString;
    }

    public Set<AbstractCodeMapping> getMappings() {
        return mappings;
    }

    public List<StatementObject> getNonMappedLeavesT1() {
        return nonMappedLeavesT1;
    }

    public List<StatementObject> getNonMappedLeavesT2() {
        return nonMappedLeavesT2;
    }

    private static class ReplacementInfo {
        private String argumentizedString1;
        private final String argumentizedString2;
        private int rawDistance;
        private final Set<Replacement> replacements;
        private final List<? extends AbstractCodeFragment> statements1;
        private final List<? extends AbstractCodeFragment> statements2;

        public ReplacementInfo(String argumentizedString1,
                               String argumentizedString2,
                               List<? extends AbstractCodeFragment> statements1,
                               List<? extends AbstractCodeFragment> statements2) {
            this.argumentizedString1 = argumentizedString1;
            this.argumentizedString2 = argumentizedString2;
            this.statements1 = statements1;
            this.statements2 = statements2;
            this.rawDistance = StringDistance.editDistance(argumentizedString1, argumentizedString2);
            this.replacements = new LinkedHashSet<>();
        }

        public String getArgumentizedString1() {
            return argumentizedString1;
        }

        public String getArgumentizedString2() {
            return argumentizedString2;
        }

        public void setArgumentizedString1(String string) {
            this.argumentizedString1 = string;
            this.rawDistance = StringDistance.editDistance(this.argumentizedString1, this.argumentizedString2);
        }

        public int getRawDistance() {
            return rawDistance;
        }

        public void addReplacement(Replacement r) {
            this.replacements.add(r);
        }

        public void addReplacements(Set<Replacement> replacementsToBeAdded) {
            this.replacements.addAll(replacementsToBeAdded);
        }

        public void removeReplacements(Set<Replacement> replacementsToBeRemoved) {
            this.replacements.removeAll(replacementsToBeRemoved);
        }

        public Set<Replacement> getReplacements() {
            return replacements;
        }

        public List<Replacement> getReplacements(Replacement.ReplacementType type) {
            List<Replacement> replacements = new ArrayList<>();
            for (Replacement replacement : this.replacements) {
                if (replacement.getType().equals(type)) {
                    replacements.add(replacement);
                }
            }
            return replacements;
        }
    }

    private boolean isTemporaryVariableAssignment(StatementObject statement) {
/*  TODO: Implement  ExtractVariableRefactoring

          for (Refactoring refactoring : refactorings) {
            if (refactoring instanceof ExtractVariableRefactoring) {
                ExtractVariableRefactoring extractVariable = (ExtractVariableRefactoring) refactoring;
                if (statement.getVariableDeclarations().contains(extractVariable.getVariableDeclaration())) {
                    return true;
                }
            }
        }*/
        return false;
    }

    public List<CompositeStatementObject> getNonMappedInnerNodesT1() {
        return nonMappedInnerNodesT1;
    }

    public int nonMappedElementsT1() {
        int nonMappedInnerNodeCount = 0;
        for (CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
            if (composite.countableStatement())
                nonMappedInnerNodeCount++;
        }
        int nonMappedLeafCount = 0;
        for (StatementObject statement : getNonMappedLeavesT1()) {
            if (statement.countableStatement())
                nonMappedLeafCount++;
        }
        return nonMappedLeafCount + nonMappedInnerNodeCount;
    }

    public List<CompositeStatementObject> getNonMappedInnerNodesT2() {
        return nonMappedInnerNodesT2;
    }

    public int nonMappedElementsT2() {
        int nonMappedInnerNodeCount = 0;
        for (CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
            if (composite.countableStatement())
                nonMappedInnerNodeCount++;
        }
        int nonMappedLeafCount = 0;
        for (StatementObject statement : getNonMappedLeavesT2()) {
            if (statement.countableStatement() && !isTemporaryVariableAssignment(statement))
                nonMappedLeafCount++;
        }
        return nonMappedLeafCount + nonMappedInnerNodeCount;
    }

    public int nonMappedLeafElementsT2() {
        int nonMappedLeafCount = 0;
        for (StatementObject statement : getNonMappedLeavesT2()) {
            if (statement.countableStatement() && !isTemporaryVariableAssignment(statement))
                nonMappedLeafCount++;
        }
        return nonMappedLeafCount;
    }


    public Set<Refactoring> getRefactorings() {
        VariableReplacementAnalysis analysis = new VariableReplacementAnalysis(this, refactorings, classDiff);
        refactorings.addAll(analysis.getVariableRenames());
        refactorings.addAll(analysis.getVariableMerges());
        refactorings.addAll(analysis.getVariableSplits());
        candidateAttributeRenames.addAll(analysis.getCandidateAttributeRenames());
        candidateAttributeMerges.addAll(analysis.getCandidateAttributeMerges());
        candidateAttributeSplits.addAll(analysis.getCandidateAttributeSplits());
        TypeReplacementAnalysis typeAnalysis = new TypeReplacementAnalysis(this.getMappings());
        refactorings.addAll(typeAnalysis.getChangedTypes());
        return refactorings;
    }

    public Set<Refactoring> getRefactoringsAfterPostProcessing() {
        return refactorings;
    }

    public int nonMappedElementsT2CallingAddedOperation(List<UMLOperation> addedOperations) {
        int nonMappedInnerNodeCount = 0;
        for (CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
            if (composite.countableStatement()) {
                Map<String, List<OperationInvocation>> methodInvocationMap = composite.getMethodInvocationMap();
                for (String key : methodInvocationMap.keySet()) {
                    for (OperationInvocation invocation : methodInvocationMap.get(key)) {
                        for (UMLOperation operation : addedOperations) {
                            if (invocation.matchesOperation(operation, operation2.variableTypeMap(), modelDiff)) {
                                nonMappedInnerNodeCount++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        int nonMappedLeafCount = 0;
        for (StatementObject statement : getNonMappedLeavesT2()) {
            if (statement.countableStatement()) {
                Map<String, List<OperationInvocation>> methodInvocationMap = statement.getMethodInvocationMap();
                for (String key : methodInvocationMap.keySet()) {
                    for (OperationInvocation invocation : methodInvocationMap.get(key)) {
                        for (UMLOperation operation : addedOperations) {
                            if (invocation.matchesOperation(operation, operation2.variableTypeMap(), modelDiff)) {
                                nonMappedLeafCount++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return nonMappedLeafCount + nonMappedInnerNodeCount;
    }


    public int nonMappedElementsT1CallingRemovedOperation(List<UMLOperation> removedOperations) {
        int nonMappedInnerNodeCount = 0;
        for (CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
            if (composite.countableStatement()) {
                Map<String, List<OperationInvocation>> methodInvocationMap = composite.getMethodInvocationMap();
                for (String key : methodInvocationMap.keySet()) {
                    for (OperationInvocation invocation : methodInvocationMap.get(key)) {
                        for (UMLOperation operation : removedOperations) {
                            if (invocation.matchesOperation(operation, operation1.variableTypeMap(), modelDiff)) {
                                nonMappedInnerNodeCount++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        int nonMappedLeafCount = 0;
        for (StatementObject statement : getNonMappedLeavesT1()) {
            if (statement.countableStatement()) {
                Map<String, List<OperationInvocation>> methodInvocationMap = statement.getMethodInvocationMap();
                for (String key : methodInvocationMap.keySet()) {
                    for (OperationInvocation invocation : methodInvocationMap.get(key)) {
                        for (UMLOperation operation : removedOperations) {
                            if (invocation.matchesOperation(operation, operation1.variableTypeMap(), modelDiff)) {
                                nonMappedLeafCount++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return nonMappedLeafCount + nonMappedInnerNodeCount;
    }

    public Set<MethodInvocationReplacement> getMethodInvocationRenameReplacements() {
        Set<MethodInvocationReplacement> replacements = new LinkedHashSet<>();
        for (AbstractCodeMapping mapping : getMappings()) {
            for (Replacement replacement : mapping.getReplacements()) {
                if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT)) {
                    replacements.add((MethodInvocationReplacement) replacement);
                }
            }
        }
        return replacements;
    }

    public Set<CandidateAttributeRefactoring> getCandidateAttributeRenames() {
        return candidateAttributeRenames;
    }

    public Set<CandidateMergeVariableRefactoring> getCandidateAttributeMerges() {
        return candidateAttributeMerges;
    }

    public Set<CandidateSplitVariableRefactoring> getCandidateAttributeSplits() {
        return candidateAttributeSplits;
    }

    public Set<Replacement> getReplacements() {
        Set<Replacement> replacements = new LinkedHashSet<>();
        for (AbstractCodeMapping mapping : getMappings()) {
            replacements.addAll(mapping.getReplacements());
        }
        return replacements;
    }

    public double normalizedEditDistance() {
        double editDistance = 0;
        double maxLength = 0;
        for (AbstractCodeMapping mapping : getMappings()) {
            if (mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
                continue;
            }
            String s1 = preprocessInput1(mapping.getFragment1(), mapping.getFragment2());
            String s2 = preprocessInput2(mapping.getFragment1(), mapping.getFragment2());
            if (!s1.equals(s2)) {
                editDistance += StringDistance.editDistance(s1, s2);
                maxLength += Math.max(s1.length(), s2.length());
            }
        }
        return editDistance / maxLength;
    }

    public List<AbstractCodeMapping> getExactMatches() {
        List<AbstractCodeMapping> exactMatches = new ArrayList<>();
        for (AbstractCodeMapping mapping : getMappings()) {
            if (mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
                !mapping.getFragment1().getString().equals("try"))
                exactMatches.add(mapping);
        }
        return exactMatches;
    }

    public Set<Replacement> getReplacementsInvolvingMethodInvocation() {
        Set<Replacement> replacements = new LinkedHashSet<>();
        for (AbstractCodeMapping mapping : getMappings()) {
            for (Replacement replacement : mapping.getReplacements()) {
                if (replacement instanceof MethodInvocationReplacement ||
                    replacement instanceof VariableReplacementWithMethodInvocation ||
                    //  replacement instanceof ClassInstanceCreationWithMethodInvocationReplacement ||
                    replacement.getType().equals(
                        Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION)) {
                    replacements.add(replacement);
                }
            }
        }
        return replacements;
    }

    public void addChildMapper(UMLOperationBodyMapper mapper) {
        this.childMappers.add(mapper);
        //TODO add logic to remove the mappings from "this" mapper,
        //which are less similar than the mappings of the mapper passed as parameter
    }

}
