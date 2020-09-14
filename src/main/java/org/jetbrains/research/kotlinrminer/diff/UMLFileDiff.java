package org.jetbrains.research.kotlinrminer.diff;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringMinerTimedOutException;
import org.jetbrains.research.kotlinrminer.decomposition.UMLOperationBodyMapper;
import org.jetbrains.research.kotlinrminer.diff.refactoring.RenameOperationRefactoring;
import org.jetbrains.research.kotlinrminer.uml.UMLFile;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UMLFileDiff implements Comparable<UMLFileDiff> {
    protected UMLFile originalFile;
    protected UMLFile nextFile;
    protected List<UMLOperation> addedOperations;
    protected List<UMLOperation> removedOperations;
    private final List<UMLOperationBodyMapper> operationBodyMapperList;
    private final List<UMLOperationDiff> operationDiffList;
    protected List<Refactoring> refactorings;
    private final UMLModelDiff modelDiff;

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
        /*checkForOperationSignatureChanges();*/
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
/*                    if (getModelDiff() != null) {
                        List<UMLOperationBodyMapper>
                            mappers = getModelDiff().findMappersWithMatchingSignature2(nextOperation);
                        if (mappers.size() > 0) {
                            UMLOperation operation1 = mappers.get(0).getOperation1();
                            if (!operation1.equalSignature(originalOperation) &&
                                getModelDiff().commonlyImplementedOperations(operation1, nextOperation, this)) {
                                if (!removedOperations.contains(originalOperation)) {
                                    removedOperations.add(originalOperation);
                                }
                                break;
                            }
                        }
                    }*/
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
