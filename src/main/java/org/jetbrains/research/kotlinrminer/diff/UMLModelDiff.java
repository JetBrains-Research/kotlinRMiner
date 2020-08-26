package org.jetbrains.research.kotlinrminer.diff;

import java.util.*;

import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringMinerTimedOutException;
import org.jetbrains.research.kotlinrminer.decomposition.UMLOperationBodyMapper;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.decomposition.replacement.MergeVariableReplacement;
import org.jetbrains.research.kotlinrminer.decomposition.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.diff.refactoring.*;
import org.jetbrains.research.kotlinrminer.uml.*;

public class UMLModelDiff {
    private List<UMLClassMoveDiff> classMoveDiffList;
    private List<UMLClass> addedClasses;
    private List<UMLClass> removedClasses;
    private Set<String> deletedFolderPaths;
    private List<UMLClassMoveDiff> innerClassMoveDiffList;
    private List<UMLClassRenameDiff> classRenameDiffList;
    private List<UMLClassDiff> commonClassDiffList;
    private List<Refactoring> refactorings;

    public UMLModelDiff() {
        this.addedClasses = new ArrayList<>();
        this.removedClasses = new ArrayList<>();
        this.classMoveDiffList = new ArrayList<>();
        this.deletedFolderPaths = new LinkedHashSet<>();
        this.innerClassMoveDiffList = new ArrayList<>();
        this.classRenameDiffList = new ArrayList<>();
        this.commonClassDiffList = new ArrayList<>();
        this.refactorings = new ArrayList<>();
    }

    public void reportAddedClass(UMLClass umlClass) {
        if (!addedClasses.contains(umlClass)) {
            this.addedClasses.add(umlClass);
        }
    }

    public void reportRemovedClass(UMLClass umlClass) {
        if (!removedClasses.contains(umlClass)) {
            this.removedClasses.add(umlClass);
        }
    }


    private Map<RenamePattern, Integer> typeRenamePatternMap(Set<Refactoring> refactorings) {
        Map<RenamePattern, Integer> typeRenamePatternMap = new LinkedHashMap<>();
        for (Refactoring ref : refactorings) {
            if (ref instanceof ChangeVariableTypeRefactoring) {
                ChangeVariableTypeRefactoring refactoring = (ChangeVariableTypeRefactoring) ref;
                RenamePattern pattern = new RenamePattern(refactoring.getOriginalVariable().getType().toString(),
                                                          refactoring.getChangedTypeVariable().getType().toString());
                if (typeRenamePatternMap.containsKey(pattern)) {
                    typeRenamePatternMap.put(pattern, typeRenamePatternMap.get(pattern) + 1);
                } else {
                    typeRenamePatternMap.put(pattern, 1);
                }
            }
/*            else if(ref instanceof ChangeAttributeTypeRefactoring) {
                ChangeAttributeTypeRefactoring refactoring = (ChangeAttributeTypeRefactoring)ref;
                RenamePattern pattern = new RenamePattern(refactoring.getOriginalAttribute().getType().toString(), refactoring.getChangedTypeAttribute().getType().toString());
                if(typeRenamePatternMap.containsKey(pattern)) {
                    typeRenamePatternMap.put(pattern, typeRenamePatternMap.get(pattern) + 1);
                }
                else {
                    typeRenamePatternMap.put(pattern, 1);
                }
            }
            else if(ref instanceof ChangeReturnTypeRefactoring) {
                ChangeReturnTypeRefactoring refactoring = (ChangeReturnTypeRefactoring)ref;
                RenamePattern pattern = new RenamePattern(refactoring.getOriginalType().toString(), refactoring.getChangedType().toString());
                if(typeRenamePatternMap.containsKey(pattern)) {
                    typeRenamePatternMap.put(pattern, typeRenamePatternMap.get(pattern) + 1);
                }
                else {
                    typeRenamePatternMap.put(pattern, 1);
                }
            }*/
        }
        return typeRenamePatternMap;
    }

    private UMLClass looksLikeAddedClass(UMLType type) {
        for (UMLClass umlClass : addedClasses) {
            if (umlClass.getName().endsWith("." + type.getClassType())) {
                return umlClass;
            }
        }
        return null;
    }

    private UMLClass looksLikeRemovedClass(UMLType type) {
        for (UMLClass umlClass : removedClasses) {
            if (umlClass.getName().endsWith("." + type.getClassType())) {
                return umlClass;
            }
        }
        return null;
    }


    private UMLClassBaseDiff getUMLClassDiffWithAttribute(Replacement pattern) {
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                return classDiff;
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                return classDiff;
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                return classDiff;
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                return classDiff;
        }
        return null;
    }


    private List<UMLClassBaseDiff> getUMLClassDiffWithExistingAttributeAfter(Replacement pattern) {
        List<UMLClassBaseDiff> classDiffs = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                classDiffs.add(classDiff);
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                classDiffs.add(classDiff);
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                classDiffs.add(classDiff);
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                classDiffs.add(classDiff);
        }
        return classDiffs;
    }

    private List<UMLClassBaseDiff> getUMLClassDiffWithNewAttributeAfter(Replacement pattern) {
        List<UMLClassBaseDiff> classDiffs = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                classDiffs.add(classDiff);
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                classDiffs.add(classDiff);
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                classDiffs.add(classDiff);
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
                    classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
                classDiffs.add(classDiff);
        }
        return classDiffs;
    }

    public boolean isSubclassOf(String subclass, String finalSuperclass) {
        return isSubclassOf(subclass, finalSuperclass, new LinkedHashSet<>());
    }

    private boolean checkInheritanceRelationship(UMLType superclass,
                                                 String finalSuperclass,
                                                 Set<String> visitedClasses) {
        if (looksLikeSameType(superclass.getClassType(), finalSuperclass))
            return true;
        else
            return isSubclassOf(superclass.getClassType(), finalSuperclass, visitedClasses);
    }

    private boolean isSubclassOf(String subclass, String finalSuperclass, Set<String> visitedClasses) {
        if (visitedClasses.contains(subclass)) {
            return false;
        } else {
            visitedClasses.add(subclass);
        }
        UMLClassBaseDiff subclassDiff = getUMLClassDiff(subclass);
        if (subclassDiff == null) {
            subclassDiff = getUMLClassDiff(UMLType.extractTypeObject(subclass));
        }
        if (subclassDiff != null) {
            UMLType superclass = subclassDiff.getSuperclass();
            if (superclass != null) {
                if (checkInheritanceRelationship(superclass, finalSuperclass, visitedClasses)) {
                    return true;
                }
            } else if (subclassDiff.getOldSuperclass() != null && subclassDiff.getNewSuperclass() != null &&
                    !subclassDiff.getOldSuperclass().equals(subclassDiff.getNewSuperclass()) && looksLikeAddedClass(
                    subclassDiff.getNewSuperclass()) != null) {
                UMLClass addedClass = looksLikeAddedClass(subclassDiff.getNewSuperclass());
                if (addedClass.getSuperclass() != null) {
                    return checkInheritanceRelationship(addedClass.getSuperclass(), finalSuperclass, visitedClasses);
                }
            } else if (subclassDiff.getOldSuperclass() == null && subclassDiff.getNewSuperclass() != null && looksLikeAddedClass(
                    subclassDiff.getNewSuperclass()) != null) {
                UMLClass addedClass = looksLikeAddedClass(subclassDiff.getNewSuperclass());
                return checkInheritanceRelationship(UMLType.extractTypeObject(addedClass.getName()), finalSuperclass,
                                                    visitedClasses);
            }
            for (UMLType implementedInterface : subclassDiff.getAddedImplementedInterfaces()) {
                if (checkInheritanceRelationship(implementedInterface, finalSuperclass, visitedClasses)) {
                    return true;
                }
            }
            for (UMLType implementedInterface : subclassDiff.getNextClass().getImplementedInterfaces()) {
                if (checkInheritanceRelationship(implementedInterface, finalSuperclass, visitedClasses)) {
                    return true;
                }
            }
        }
        UMLClass addedClass = getAddedClass(subclass);
        if (addedClass == null) {
            addedClass = looksLikeAddedClass(UMLType.extractTypeObject(subclass));
        }
        if (addedClass != null) {
            UMLType superclass = addedClass.getSuperclass();
            if (superclass != null) {
                return checkInheritanceRelationship(superclass, finalSuperclass, visitedClasses);
            }
            for (UMLType implementedInterface : addedClass.getImplementedInterfaces()) {
                if (checkInheritanceRelationship(implementedInterface, finalSuperclass, visitedClasses)) {
                    return true;
                }
            }
        }
        UMLClass removedClass = getRemovedClass(subclass);
        if (removedClass == null) {
            removedClass = looksLikeRemovedClass(UMLType.extractTypeObject(subclass));
        }
        if (removedClass != null) {
            UMLType superclass = removedClass.getSuperclass();
            if (superclass != null) {
                return checkInheritanceRelationship(superclass, finalSuperclass, visitedClasses);
            }
            for (UMLType implementedInterface : removedClass.getImplementedInterfaces()) {
                if (checkInheritanceRelationship(implementedInterface, finalSuperclass, visitedClasses)) {
                    return true;
                }
            }
        }
        return false;
    }


    public UMLClass getAddedClass(String className) {
        for (UMLClass umlClass : addedClasses) {
            if (umlClass.getName().equals(className))
                return umlClass;
        }
        return null;
    }

    public UMLClass getRemovedClass(String className) {
        for (UMLClass umlClass : removedClasses) {
            if (umlClass.getName().equals(className))
                return umlClass;
        }
        return null;
    }

    public List<Refactoring> getRefactorings() throws RefactoringMinerTimedOutException {
        Set<Refactoring> refactorings = new LinkedHashSet<>();
        refactorings.addAll(getMoveClassRefactorings());
        refactorings.addAll(getRenameClassRefactorings());
        // refactorings.addAll(identifyConvertAnonymousClassToTypeRefactorings());
        Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap =
                new LinkedHashMap<>();
        Map<MergeVariableReplacement, Set<CandidateMergeVariableRefactoring>> mergeMap =
                new LinkedHashMap<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            refactorings.addAll(classDiff.getRefactorings());
/*            extractMergePatterns(classDiff, mergeMap);
            extractRenamePatterns(classDiff, renameMap);*/
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            refactorings.addAll(classDiff.getRefactorings());
/*            extractMergePatterns(classDiff, mergeMap);
            extractRenamePatterns(classDiff, renameMap);*/
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            refactorings.addAll(classDiff.getRefactorings());
/*            extractMergePatterns(classDiff, mergeMap);
            extractRenamePatterns(classDiff, renameMap);*/
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            refactorings.addAll(classDiff.getRefactorings());
/*            extractMergePatterns(classDiff, mergeMap);
            extractRenamePatterns(classDiff, renameMap);*/
        }
        Map<RenamePattern, Integer> typeRenamePatternMap = typeRenamePatternMap(refactorings);
        for (RenamePattern pattern : typeRenamePatternMap.keySet()) {
            if (typeRenamePatternMap.get(pattern) > 1) {
                UMLClass removedClass = looksLikeRemovedClass(UMLType.extractTypeObject(pattern.getBefore()));
                UMLClass addedClass = looksLikeAddedClass(UMLType.extractTypeObject(pattern.getAfter()));
                if (removedClass != null && addedClass != null) {
                    UMLClassRenameDiff renameDiff = new UMLClassRenameDiff(removedClass, addedClass, this);
                    renameDiff.process();
                    refactorings.addAll(renameDiff.getRefactorings());
/*                    extractMergePatterns(renameDiff, mergeMap);
                    extractRenamePatterns(renameDiff, renameMap);*/
                    classRenameDiffList.add(renameDiff);
                    Refactoring refactoring = null;
                    if (renameDiff.samePackage())
                        refactoring =
                                new RenameClassRefactoring(renameDiff.getOriginalClass(), renameDiff.getRenamedClass());
                    else
                        refactoring = new MoveAndRenameClassRefactoring(renameDiff.getOriginalClass(),
                                                                        renameDiff.getRenamedClass());
                    refactorings.add(refactoring);
                }
            }
        }
        for (MergeVariableReplacement merge : mergeMap.keySet()) {
            UMLClassBaseDiff diff = null;
            for (String mergedVariable : merge.getMergedVariables()) {
                Replacement replacement =
                        new Replacement(mergedVariable, merge.getAfter(), Replacement.ReplacementType.VARIABLE_NAME);
                diff = getUMLClassDiffWithAttribute(replacement);
            }
            if (diff != null) {
                Set<UMLAttribute> mergedAttributes = new LinkedHashSet<>();
                Set<VariableDeclaration> mergedVariables = new LinkedHashSet<>();
                for (String mergedVariable : merge.getMergedVariables()) {
                    UMLAttribute a1 = diff.findAttributeInOriginalClass(mergedVariable);
                    if (a1 != null) {
                        mergedAttributes.add(a1);
                        mergedVariables.add(a1.getVariableDeclaration());
                    }
                }
                UMLAttribute a2 = diff.findAttributeInNextClass(merge.getAfter());
                Set<CandidateMergeVariableRefactoring> set = mergeMap.get(merge);
/*                if(mergedVariables.size() > 1 && mergedVariables.size() == merge.getMergedVariables().size() && a2 != null) {
                    MergeAttributeRefactoring ref = new MergeAttributeRefactoring(mergedAttributes, a2, diff.getOriginalClassName(), diff.getNextClassName(), set);
                    if(!refactorings.contains(ref)) {
                        refactorings.add(ref);
                        Refactoring conflictingRefactoring = attributeRenamed(mergedVariables, a2.getVariableDeclaration(), refactorings);
                        if(conflictingRefactoring != null) {
                            refactorings.remove(conflictingRefactoring);
                        }
                    }
                }*/
            }
        }
        for (Replacement pattern : renameMap.keySet()) {
            UMLClassBaseDiff diff = getUMLClassDiffWithAttribute(pattern);
            Set<CandidateAttributeRefactoring> set = renameMap.get(pattern);
            for (CandidateAttributeRefactoring candidate : set) {
                if (candidate.getOriginalVariableDeclaration() == null && candidate.getRenamedVariableDeclaration() == null) {
                    if (diff != null) {
                        UMLAttribute a1 = diff.findAttributeInOriginalClass(pattern.getBefore());
                        UMLAttribute a2 = diff.findAttributeInNextClass(pattern.getAfter());
                        if (!diff.getOriginalClass().containsAttributeWithName(pattern.getAfter()) &&
                                !diff.getNextClass().containsAttributeWithName(pattern.getBefore())
                            /* && !attributeMerged(a1, a2, refactorings)*/) {
/*                            UMLAttributeDiff attributeDiff = new UMLAttributeDiff(a1, a2, diff.getOperationBodyMapperList());
                            Set<Refactoring> attributeDiffRefactorings = attributeDiff.getRefactorings(set);
                            if(!refactorings.containsAll(attributeDiffRefactorings)) {
                                refactorings.addAll(attributeDiffRefactorings);
                                break;//it's not necessary to repeat the same process for all candidates in the set
                            }*/
                        }
                    }
                } else if (candidate.getOriginalVariableDeclaration() != null) {
                    List<UMLClassBaseDiff> diffs1 = getUMLClassDiffWithExistingAttributeAfter(pattern);
                    List<UMLClassBaseDiff> diffs2 = getUMLClassDiffWithNewAttributeAfter(pattern);
                    if (!diffs1.isEmpty()) {
                        UMLClassBaseDiff diff1 = diffs1.get(0);
                        UMLClassBaseDiff originalClassDiff = null;
                        if (candidate.getOriginalAttribute() != null) {
                            originalClassDiff = getUMLClassDiff(candidate.getOriginalAttribute().getClassName());
                        } else {
                            originalClassDiff = getUMLClassDiff(candidate.getOperationBefore().getClassName());
                        }
                        if (diffs1.size() > 1) {
                            for (UMLClassBaseDiff classDiff : diffs1) {
                                if (isSubclassOf(originalClassDiff.nextClass.getName(),
                                                 classDiff.nextClass.getName())) {
                                    diff1 = classDiff;
                                    break;
                                }
                            }
                        }
                        UMLAttribute a2 = diff1.findAttributeInNextClass(pattern.getAfter());
                        if (a2 != null) {
                            if (candidate.getOriginalVariableDeclaration().isAttribute()) {
                                if (originalClassDiff != null && originalClassDiff.removedAttributes.contains(
                                        candidate.getOriginalAttribute())) {
/*                                    ReplaceAttributeRefactoring ref = new ReplaceAttributeRefactoring(candidate.getOriginalAttribute(), a2, set);
                                    if(!refactorings.contains(ref)) {
                                        refactorings.add(ref);
                                        break;//it's not necessary to repeat the same process for all candidates in the set
                                    }*/
                                }
                            } else {
                                RenameVariableRefactoring ref =
                                        new RenameVariableRefactoring(candidate.getOriginalVariableDeclaration(),
                                                                      a2.getVariableDeclaration(),
                                                                      candidate.getOperationBefore(),
                                                                      candidate.getOperationAfter(),
                                                                      candidate.getAttributeReferences());
                                if (!refactorings.contains(ref)) {
                                    refactorings.add(ref);
                                    break;//it's not necessary to repeat the same process for all candidates in the set
                                }
                            }
                        }
                    } else if (!diffs2.isEmpty()) {
                        UMLClassBaseDiff diff2 = diffs2.get(0);
                        UMLClassBaseDiff originalClassDiff = null;
                        if (candidate.getOriginalAttribute() != null) {
                            originalClassDiff = getUMLClassDiff(candidate.getOriginalAttribute().getClassName());
                        } else {
                            originalClassDiff = getUMLClassDiff(candidate.getOperationBefore().getClassName());
                        }
                        if (diffs2.size() > 1) {
                            for (UMLClassBaseDiff classDiff : diffs2) {
                                if (isSubclassOf(originalClassDiff.nextClass.getName(),
                                                 classDiff.nextClass.getName())) {
                                    diff2 = classDiff;
                                    break;
                                }
                            }
                        }
                        UMLAttribute a2 = diff2.findAttributeInNextClass(pattern.getAfter());
                        if (a2 != null) {
                            if (candidate.getOriginalVariableDeclaration().isAttribute()) {
                                if (originalClassDiff != null && originalClassDiff.removedAttributes.contains(
                                        candidate.getOriginalAttribute())) {
/*                                    MoveAndRenameAttributeRefactoring ref = new MoveAndRenameAttributeRefactoring(candidate.getOriginalAttribute(), a2, set);
                                    if(!refactorings.contains(ref)) {
                                        refactorings.add(ref);
                                        break;//it's not necessary to repeat the same process for all candidates in the set
                                    }*/
                                }
                            } else {
                                RenameVariableRefactoring ref =
                                        new RenameVariableRefactoring(candidate.getOriginalVariableDeclaration(),
                                                                      a2.getVariableDeclaration(),
                                                                      candidate.getOperationBefore(),
                                                                      candidate.getOperationAfter(),
                                                                      candidate.getAttributeReferences());
                                if (!refactorings.contains(ref)) {
                                    refactorings.add(ref);
                                    break;//it's not necessary to repeat the same process for all candidates in the set
                                }
                            }
                        }
                    }
                }
            }
        }
       /* refactorings.addAll(identifyExtractSuperclassRefactorings());
        refactorings.addAll(identifyExtractClassRefactorings(commonClassDiffList));
        refactorings.addAll(identifyExtractClassRefactorings(classMoveDiffList));
        refactorings.addAll(identifyExtractClassRefactorings(innerClassMoveDiffList));
        refactorings.addAll(identifyExtractClassRefactorings(classRenameDiffList));
        checkForOperationMovesBetweenCommonClasses();
        checkForOperationMovesIncludingAddedClasses();
        checkForOperationMovesIncludingRemovedClasses();
        checkForExtractedAndMovedOperations(getOperationBodyMappersInCommonClasses(), getAddedAndExtractedOperationsInCommonClasses());
        checkForExtractedAndMovedOperations(getOperationBodyMappersInMovedAndRenamedClasses(), getAddedOperationsInMovedAndRenamedClasses());
        checkForMovedAndInlinedOperations(getOperationBodyMappersInCommonClasses(), getRemovedAndInlinedOperationsInCommonClasses());
*//*        refactorings.addAll(checkForAttributeMovesBetweenCommonClasses());
        refactorings.addAll(checkForAttributeMovesIncludingAddedClasses());
        refactorings.addAll(checkForAttributeMovesIncludingRemovedClasses());*/
        refactorings.addAll(this.refactorings);
        for (UMLClassDiff classDiff : commonClassDiffList) {
            inferMethodSignatureRelatedRefactorings(classDiff, refactorings);
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            inferMethodSignatureRelatedRefactorings(classDiff, refactorings);
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            inferMethodSignatureRelatedRefactorings(classDiff, refactorings);
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            inferMethodSignatureRelatedRefactorings(classDiff, refactorings);
        }
        return new ArrayList<>(refactorings);
    }

    private List<Refactoring> getRenameClassRefactorings() {
        List<Refactoring> refactorings = new ArrayList<>();
        for (UMLClassRenameDiff classRenameDiff : classRenameDiffList) {
            Refactoring refactoring;
            if (classRenameDiff.samePackage()) {
                refactoring = new RenameClassRefactoring(classRenameDiff.getOriginalClass(),
                                                         classRenameDiff.getRenamedClass());
            } else {
                refactoring = new MoveAndRenameClassRefactoring(classRenameDiff.getOriginalClass(),
                                                                classRenameDiff.getRenamedClass());
            }
            refactorings.add(refactoring);
        }
        return refactorings;
    }


    private UMLClassBaseDiff getUMLClassDiff(String className) {
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.matches(className))
                return classDiff;
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.matches(className))
                return classDiff;
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.matches(className))
                return classDiff;
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.matches(className))
                return classDiff;
        }
        return null;
    }

    private UMLClassBaseDiff getUMLClassDiff(UMLType type) {
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.matches(type))
                return classDiff;
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.matches(type))
                return classDiff;
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.matches(type))
                return classDiff;
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.matches(type))
                return classDiff;
        }
        return null;
    }

    public boolean commonlyImplementedOperations(UMLOperation operation1,
                                                 UMLOperation operation2,
                                                 UMLClassBaseDiff classDiff2) {
        UMLClassBaseDiff classDiff1 = getUMLClassDiff(operation1.getClassName());
        if (classDiff1 != null) {
            Set<UMLType> commonInterfaces = classDiff1.nextClassCommonInterfaces(classDiff2);
            for (UMLType commonInterface : commonInterfaces) {
                UMLClassBaseDiff interfaceDiff = getUMLClassDiff(commonInterface);
                if (interfaceDiff != null &&
                        interfaceDiff.containsOperationWithTheSameSignatureInOriginalClass(operation1) &&
                        interfaceDiff.containsOperationWithTheSameSignatureInNextClass(operation2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<UMLOperationBodyMapper> findMappersWithMatchingSignatures(UMLOperation operation1,
                                                                           UMLOperation operation2) {
        List<UMLOperationBodyMapper> mappers = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignatures(operation1, operation2);
            if (mapper != null) {
                mappers.add(mapper);
            }
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignatures(operation1, operation2);
            if (mapper != null) {
                mappers.add(mapper);
            }
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignatures(operation1, operation2);
            if (mapper != null) {
                mappers.add(mapper);
            }
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignatures(operation1, operation2);
            if (mapper != null) {
                mappers.add(mapper);
            }
        }
        return mappers;
    }

    private void inferMethodSignatureRelatedRefactorings(UMLClassBaseDiff classDiff, Set<Refactoring> refactorings) {
        if (classDiff.getOriginalClass().isInterface() && classDiff.getNextClass().isInterface()) {
            for (UMLOperation removedOperation : classDiff.getRemovedOperations()) {
                for (UMLOperation addedOperation : classDiff.getAddedOperations()) {
                    List<UMLOperationBodyMapper> mappers =
                            findMappersWithMatchingSignatures(removedOperation, addedOperation);
                    if (!mappers.isEmpty()) {
                        UMLOperationDiff operationSignatureDiff =
                                new UMLOperationDiff(removedOperation, addedOperation);
                        if (operationSignatureDiff.isOperationRenamed()) {
                            RenameOperationRefactoring refactoring =
                                    new RenameOperationRefactoring(removedOperation, addedOperation);
                            refactorings.add(refactoring);
                        }
                        Set<Refactoring> signatureRefactorings = operationSignatureDiff.getRefactorings();
                        refactorings.addAll(signatureRefactorings);
                        if (signatureRefactorings.isEmpty()) {
                            inferRefactoringsFromMatchingMappers(mappers, operationSignatureDiff, refactorings);
                        }
                    }
                }
            }
        } else if (classDiff.getOriginalClass().isAbstract() && classDiff.getNextClass().isAbstract()) {
            for (UMLOperation removedOperation : classDiff.getRemovedOperations()) {
                for (UMLOperation addedOperation : classDiff.getAddedOperations()) {
                    if (removedOperation.isAbstract() && addedOperation.isAbstract()) {
                        List<UMLOperationBodyMapper> mappers =
                                findMappersWithMatchingSignatures(removedOperation, addedOperation);
                        if (!mappers.isEmpty()) {
                            UMLOperationDiff operationSignatureDiff =
                                    new UMLOperationDiff(removedOperation, addedOperation);
                            if (operationSignatureDiff.isOperationRenamed()) {
                                RenameOperationRefactoring refactoring =
                                        new RenameOperationRefactoring(removedOperation, addedOperation);
                                refactorings.add(refactoring);
                            }
                            Set<Refactoring> signatureRefactorings = operationSignatureDiff.getRefactorings();
                            refactorings.addAll(signatureRefactorings);
                            if (signatureRefactorings.isEmpty()) {
                                inferRefactoringsFromMatchingMappers(mappers, operationSignatureDiff, refactorings);
                            }
                        }
                    }
                }
            }
        }
    }

    public List<UMLOperationBodyMapper> findMappersWithMatchingSignature2(UMLOperation operation2) {
        List<UMLOperationBodyMapper> mappers = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignature2(operation2);
            if (mapper != null) {
                mappers.add(mapper);
            }
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignature2(operation2);
            if (mapper != null) {
                mappers.add(mapper);
            }
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignature2(operation2);
            if (mapper != null) {
                mappers.add(mapper);
            }
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignature2(operation2);
            if (mapper != null) {
                mappers.add(mapper);
            }
        }
        return mappers;
    }

    private void inferRefactoringsFromMatchingMappers(List<UMLOperationBodyMapper> mappers,
                                                      UMLOperationDiff operationSignatureDiff,
                                                      Set<Refactoring> refactorings) {
        for (UMLOperationBodyMapper mapper : mappers) {
            for (Refactoring refactoring : mapper.getRefactoringsAfterPostProcessing()) {
                if (refactoring instanceof RenameVariableRefactoring) {
                    RenameVariableRefactoring rename = (RenameVariableRefactoring) refactoring;
                    UMLParameter matchingRemovedParameter = null;
                    for (UMLParameter parameter : operationSignatureDiff.getRemovedParameters()) {
                        if (parameter.getName().equals(rename.getOriginalVariable().getVariableName()) &&
                                parameter.getType().equals(rename.getOriginalVariable().getType())) {
                            matchingRemovedParameter = parameter;
                            break;
                        }
                    }
                    UMLParameter matchingAddedParameter = null;
                    for (UMLParameter parameter : operationSignatureDiff.getAddedParameters()) {
                        if (parameter.getName().equals(rename.getRenamedVariable().getVariableName()) &&
                                parameter.getType().equals(rename.getRenamedVariable().getType())) {
                            matchingAddedParameter = parameter;
                            break;
                        }
                    }
                    if (matchingRemovedParameter != null && matchingAddedParameter != null) {
                        RenameVariableRefactoring newRename =
                                new RenameVariableRefactoring(matchingRemovedParameter.getVariableDeclaration(),
                                                              matchingAddedParameter.getVariableDeclaration(),
                                                              operationSignatureDiff.getRemovedOperation(),
                                                              operationSignatureDiff.getAddedOperation(),
                                                              new LinkedHashSet<>());
                        refactorings.add(newRename);
                    }
                } else if (refactoring instanceof ChangeVariableTypeRefactoring) {
                    ChangeVariableTypeRefactoring changeType = (ChangeVariableTypeRefactoring) refactoring;
                    UMLParameter matchingRemovedParameter = null;
                    for (UMLParameter parameter : operationSignatureDiff.getRemovedParameters()) {
                        if (parameter.getName().equals(changeType.getOriginalVariable().getVariableName()) &&
                                parameter.getType().equals(changeType.getOriginalVariable().getType())) {
                            matchingRemovedParameter = parameter;
                            break;
                        }
                    }
                    UMLParameter matchingAddedParameter = null;
                    for (UMLParameter parameter : operationSignatureDiff.getAddedParameters()) {
                        if (parameter.getName().equals(changeType.getChangedTypeVariable().getVariableName()) &&
                                parameter.getType().equals(changeType.getChangedTypeVariable().getType())) {
                            matchingAddedParameter = parameter;
                            break;
                        }
                    }
                    if (matchingRemovedParameter != null && matchingAddedParameter != null) {
                        ChangeVariableTypeRefactoring newChangeType =
                                new ChangeVariableTypeRefactoring(matchingRemovedParameter.getVariableDeclaration(),
                                                                  matchingAddedParameter.getVariableDeclaration(),
                                                                  operationSignatureDiff.getRemovedOperation(),
                                                                  operationSignatureDiff.getAddedOperation(),
                                                                  new LinkedHashSet<>());
                        refactorings.add(newChangeType);
                    }
                }
            }
        }
    }


    private List<Refactoring> getMoveClassRefactorings() {
        List<Refactoring> refactorings = new ArrayList<>();
        List<RenamePackageRefactoring> renamePackageRefactorings = new ArrayList<>();
        List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = new ArrayList<>();
        for (UMLClassMoveDiff classMoveDiff : classMoveDiffList) {
            UMLClass originalClass = classMoveDiff.getOriginalClass();
            String originalName = originalClass.getName();
            UMLClass movedClass = classMoveDiff.getMovedClass();
            String movedName = movedClass.getName();

            String originalPath = originalClass.getSourceFile();
            String movedPath = movedClass.getSourceFile();
            String originalPathPrefix = "";
            if (originalPath.contains("/")) {
                originalPathPrefix = originalPath.substring(0, originalPath.lastIndexOf('/'));
            }
            String movedPathPrefix = "";
            if (movedPath.contains("/")) {
                movedPathPrefix = movedPath.substring(0, movedPath.lastIndexOf('/'));
            }

            if (!originalName.equals(movedName)) {
                MoveClassRefactoring refactoring = new MoveClassRefactoring(originalClass, movedClass);
                RenamePattern renamePattern = refactoring.getRenamePattern();
                //check if the the original path is a substring of the moved path and vice versa
                if (renamePattern.getBefore().contains(renamePattern.getAfter()) ||
                        renamePattern.getAfter().contains(renamePattern.getBefore()) ||
                        !originalClass.isTopLevel() || !movedClass.isTopLevel()) {
                    refactorings.add(refactoring);
                } else {
                    boolean foundInMatchingRenamePackageRefactoring = false;
                    for (RenamePackageRefactoring renamePackageRefactoring : renamePackageRefactorings) {
                        if (renamePackageRefactoring.getPattern().equals(renamePattern)) {
                            renamePackageRefactoring.addMoveClassRefactoring(refactoring);
                            foundInMatchingRenamePackageRefactoring = true;
                            break;
                        }
                    }
                    if (!foundInMatchingRenamePackageRefactoring) {
                        renamePackageRefactorings.add(new RenamePackageRefactoring(refactoring));
                    }
                }
            } else if (!originalPathPrefix.equals(movedPathPrefix)) {
                MovedClassToAnotherSourceFolder refactoring =
                        new MovedClassToAnotherSourceFolder(originalClass, movedClass, originalPathPrefix,
                                                            movedPathPrefix);
                RenamePattern renamePattern = refactoring.getRenamePattern();
                boolean foundInMatchingMoveSourceFolderRefactoring = false;
                for (MoveSourceFolderRefactoring moveSourceFolderRefactoring : moveSourceFolderRefactorings) {
                    if (moveSourceFolderRefactoring.getPattern().equals(renamePattern)) {
                        moveSourceFolderRefactoring.addMovedClassToAnotherSourceFolder(refactoring);
                        foundInMatchingMoveSourceFolderRefactoring = true;
                        break;
                    }
                }
                if (!foundInMatchingMoveSourceFolderRefactoring) {
                    moveSourceFolderRefactorings.add(new MoveSourceFolderRefactoring(refactoring));
                }
            }
        }
        for (RenamePackageRefactoring renamePackageRefactoring : renamePackageRefactorings) {
            List<MoveClassRefactoring> moveClassRefactorings =
                    renamePackageRefactoring.getMoveClassRefactorings();
            if (moveClassRefactorings.size() > 1 && isSourcePackageDeleted(renamePackageRefactoring)) {
                refactorings.add(renamePackageRefactoring);
            }
            refactorings.addAll(moveClassRefactorings);
        }
        refactorings.addAll(moveSourceFolderRefactorings);
        return refactorings;
    }

    public void checkForMovedClasses(Map<String, String> renamedFileHints,
                                     Set<String> repositoryDirectories, UMLClassMatcher matcher) throws
            RefactoringMinerTimedOutException {
        for (Iterator<UMLClass> removedClassIterator = removedClasses.iterator();
             removedClassIterator.hasNext(); ) {
            UMLClass removedClass = removedClassIterator.next();
            TreeSet<UMLClassMoveDiff> diffSet = new TreeSet<>(new ClassMoveComparator());
            for (UMLClass addedClass : addedClasses) {
                String removedClassSourceFile = removedClass.getSourceFile();
                String renamedFile = renamedFileHints.get(removedClassSourceFile);
                String removedClassSourceFolder = "";
                if (removedClassSourceFile.contains("/")) {
                    removedClassSourceFolder =
                            removedClassSourceFile.substring(0, removedClassSourceFile.lastIndexOf("/"));
                }
                if (!repositoryDirectories.contains(removedClassSourceFolder)) {
                    deletedFolderPaths.add(removedClassSourceFolder);
                    //add deleted sub-directories
                    String subDirectory = removedClassSourceFolder;
                    while (subDirectory.contains("/")) {
                        subDirectory = subDirectory.substring(0, subDirectory.lastIndexOf("/"));
                        if (!repositoryDirectories.contains(subDirectory)) {
                            deletedFolderPaths.add(subDirectory);
                        }
                    }
                }
                if (matcher.match(removedClass, addedClass, renamedFile)) {
                    if (!conflictingMoveOfTopLevelClass(removedClass, addedClass)) {
                        UMLClassMoveDiff classMoveDiff = new UMLClassMoveDiff(removedClass, addedClass, this);
                        diffSet.add(classMoveDiff);
                    }
                }
            }
            if (!diffSet.isEmpty()) {
                UMLClassMoveDiff minClassMoveDiff = diffSet.first();
                minClassMoveDiff.process();
                classMoveDiffList.add(minClassMoveDiff);
                addedClasses.remove(minClassMoveDiff.getMovedClass());
                removedClassIterator.remove();
            }
        }

        List<UMLClassMoveDiff> allClassMoves = new ArrayList<>(this.classMoveDiffList);
        Collections.sort(allClassMoves);

        for (int i = 0; i < allClassMoves.size(); i++) {
            UMLClassMoveDiff classMoveI = allClassMoves.get(i);
            for (int j = i + 1; j < allClassMoves.size(); j++) {
                UMLClassMoveDiff classMoveJ = allClassMoves.get(j);
                if (classMoveI.isInnerClassMove(classMoveJ)) {
                    innerClassMoveDiffList.add(classMoveJ);
                }
            }
        }
        this.classMoveDiffList.removeAll(innerClassMoveDiffList);
    }

    public void checkForRenamedClasses(Map<String, String> renamedFileHints,
                                       UMLClassMatcher matcher) throws RefactoringMinerTimedOutException {
        for (Iterator<UMLClass> removedClassIterator = removedClasses.iterator();
             removedClassIterator.hasNext(); ) {
            UMLClass removedClass = removedClassIterator.next();
            TreeSet<UMLClassRenameDiff> diffSet = new TreeSet<>(new ClassRenameComparator());
            for (UMLClass addedClass : addedClasses) {
                String renamedFile = renamedFileHints.get(removedClass.getSourceFile());
                if (matcher.match(removedClass, addedClass, renamedFile)) {
                    if (!conflictingMoveOfTopLevelClass(removedClass, addedClass) &&
                            !innerClassWithTheSameName(removedClass, addedClass)) {
                        UMLClassRenameDiff classRenameDiff =
                                new UMLClassRenameDiff(removedClass, addedClass, this);
                        diffSet.add(classRenameDiff);
                    }
                }
            }
            if (!diffSet.isEmpty()) {
                UMLClassRenameDiff minClassRenameDiff = diffSet.first();
                minClassRenameDiff.process();
                classRenameDiffList.add(minClassRenameDiff);
                addedClasses.remove(minClassRenameDiff.getRenamedClass());
                removedClassIterator.remove();
            }
        }

        List<UMLClassMoveDiff> allClassMoves = new ArrayList<>(this.classMoveDiffList);
        Collections.sort(allClassMoves);

        for (UMLClassRenameDiff classRename : classRenameDiffList) {
            for (UMLClassMoveDiff classMove : allClassMoves) {
                if (classRename.isInnerClassMove(classMove)) {
                    innerClassMoveDiffList.add(classMove);
                }
            }
        }
        this.classMoveDiffList.removeAll(innerClassMoveDiffList);
    }

    private boolean conflictingMoveOfTopLevelClass(UMLClass removedClass, UMLClass addedClass) {
        if (!removedClass.isTopLevel() && !addedClass.isTopLevel()) {
            //check if classMoveDiffList contains already a move for the outer class to a different target
            for (UMLClassMoveDiff diff : classMoveDiffList) {
                if ((diff.getOriginalClass().getName().startsWith(removedClass.getPackageName()) &&
                        !diff.getMovedClass().getName().startsWith(addedClass.getPackageName())) ||
                        (!diff.getOriginalClass().getName().startsWith(removedClass.getPackageName()) &&
                                diff.getMovedClass().getName().startsWith(addedClass.getPackageName()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSourcePackageDeleted(RenamePackageRefactoring renamePackageRefactoring) {
        for (String deletedFolderPath : deletedFolderPaths) {
            String originalPath = renamePackageRefactoring.getPattern().getBefore();
            //remove last .
            String trimmedOriginalPath =
                    originalPath.endsWith(".") ? originalPath.substring(0, originalPath.length() - 1) :
                            originalPath;
            String convertedPackageToFilePath = trimmedOriginalPath.replaceAll("\\.", "/");
            if (deletedFolderPath.endsWith(convertedPackageToFilePath)) {
                return true;
            }
        }
        return false;
    }

    private boolean innerClassWithTheSameName(UMLClass removedClass, UMLClass addedClass) {
        if (!removedClass.isTopLevel() && !addedClass.isTopLevel()) {
            String removedClassName = removedClass.getName();
            String removedName = removedClassName.substring(removedClassName.lastIndexOf(".") + 1);
            String addedClassName = addedClass.getName();
            String addedName = addedClassName.substring(addedClassName.lastIndexOf(".") + 1);
            return removedName.equals(addedName);
        }
        return false;
    }

    public static boolean looksLikeSameType(String parent, String addedClassName) {
        if (addedClassName.contains(".") && !parent.contains(".")) {
            return parent.equals(addedClassName.substring(addedClassName.lastIndexOf(".") + 1));
        }
        if (parent.contains(".") && !addedClassName.contains(".")) {
            return addedClassName.equals(parent.substring(parent.lastIndexOf(".") + 1));
        }
        if (parent.contains(".") && addedClassName.contains(".")) {
            return UMLType.extractTypeObject(parent).equalClassType(UMLType.extractTypeObject(addedClassName));
        }
        return parent.equals(addedClassName);
    }

    public void addUMLClassDiff(UMLClassDiff classDiff) {
        this.commonClassDiffList.add(classDiff);
    }
}
