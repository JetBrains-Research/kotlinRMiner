package org.jetbrains.research.kotlinrminer.diff;

import java.util.*;

import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringMinerTimedOutException;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.decomposition.*;
import org.jetbrains.research.kotlinrminer.decomposition.replacement.MergeVariableReplacement;
import org.jetbrains.research.kotlinrminer.decomposition.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.diff.refactoring.*;
import org.jetbrains.research.kotlinrminer.uml.*;

public class UMLModelDiff {
    private static final int MAXIMUM_NUMBER_OF_COMPARED_METHODS = 100;
    private final List<UMLClassMoveDiff> classMoveDiffList;
    private final List<UMLClass> addedClasses;
    private final List<UMLClass> removedClasses;
    private final Set<String> deletedFolderPaths;
    private final List<UMLGeneralization> addedGeneralizations;
    private final List<UMLGeneralization> removedGeneralizations;
    private final List<UMLGeneralizationDiff> generalizationDiffList;
    private final List<UMLRealization> addedRealizations;
    private List<UMLRealization> removedRealizations;
    //private List<UMLRealizationDiff> realizationDiffList;
    private final List<UMLClassMoveDiff> innerClassMoveDiffList;
    private final List<UMLClassRenameDiff> classRenameDiffList;
    private final List<UMLClassDiff> commonClassDiffList;
    private final List<UMLFileDiff> umlFileDiff;
    private final List<Refactoring> refactorings;

    public UMLModelDiff() {
        this.addedClasses = new ArrayList<>();
        this.removedClasses = new ArrayList<>();
        this.classMoveDiffList = new ArrayList<>();
        this.deletedFolderPaths = new LinkedHashSet<>();
        this.innerClassMoveDiffList = new ArrayList<>();
        this.classRenameDiffList = new ArrayList<>();
        this.commonClassDiffList = new ArrayList<>();
        this.umlFileDiff = new ArrayList<>();
        this.refactorings = new ArrayList<>();
        this.addedGeneralizations = new ArrayList<>();
        this.removedGeneralizations = new ArrayList<>();
        this.generalizationDiffList = new ArrayList<>();
        /* this.realizationDiffList = new ArrayList<>(); */
        this.addedRealizations = new ArrayList<>();
        this.removedRealizations = new ArrayList<>();
        this.removedRealizations = new ArrayList<>();
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
        }
        return typeRenamePatternMap;
    }

    private UMLClass looksLikeAddedClass(UMLType type) {
        for (UMLClass umlClass : addedClasses) {
            if (umlClass.getQualifiedName().endsWith("." + type.getClassType())) {
                return umlClass;
            }
        }
        return null;
    }

    private UMLClass looksLikeRemovedClass(UMLType type) {
        for (UMLClass umlClass : removedClasses) {
            if (umlClass.getQualifiedName().endsWith("." + type.getClassType())) {
                return umlClass;
            }
        }
        return null;
    }

    private int computeCompatibility(MoveAttributeRefactoring candidate) {
        int count = 0;
        for (Refactoring ref : refactorings) {
            if (ref instanceof MoveOperationRefactoring) {
                MoveOperationRefactoring moveRef = (MoveOperationRefactoring) ref;
                if (moveRef.compatibleWith(candidate)) {
                    count++;
                }
            }
        }
        UMLClassBaseDiff sourceClassDiff = getUMLClassDiff(candidate.getSourceClassName());
        UMLClassBaseDiff targetClassDiff = getUMLClassDiff(candidate.getTargetClassName());
        if (sourceClassDiff != null) {
            UMLType targetSuperclass = null;
            if (targetClassDiff != null) {
                targetSuperclass = targetClassDiff.getSuperclass();
            }
            List<UMLAttribute> addedAttributes = sourceClassDiff.getAddedAttributes();
            for (UMLAttribute addedAttribute : addedAttributes) {
                if (looksLikeSameType(addedAttribute.getType().getClassType(), candidate.getTargetClassName())) {
                    count++;
                }
                if (targetSuperclass != null && looksLikeSameType(addedAttribute.getType().getClassType(),
                                                                  targetSuperclass.getClassType())) {
                    count++;
                }
            }
            List<UMLAttribute> originalAttributes =
                sourceClassDiff.originalClassAttributesOfType(candidate.getTargetClassName());
            List<UMLAttribute> nextAttributes =
                sourceClassDiff.nextClassAttributesOfType(candidate.getTargetClassName());
            if (targetSuperclass != null) {
                originalAttributes.addAll(
                    sourceClassDiff.originalClassAttributesOfType(targetSuperclass.getClassType()));
                nextAttributes.addAll(sourceClassDiff.nextClassAttributesOfType(targetSuperclass.getClassType()));
            }
            Set<UMLAttribute> intersection = new LinkedHashSet<>(originalAttributes);
            intersection.retainAll(nextAttributes);
            if (!intersection.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void processCandidates(List<MoveAttributeRefactoring> candidates,
                                   List<MoveAttributeRefactoring> refactorings) {
        if (candidates.size() > 1) {
            TreeMap<Integer, List<MoveAttributeRefactoring>> map =
                new TreeMap<>();
            for (MoveAttributeRefactoring candidate : candidates) {
                int compatibility = computeCompatibility(candidate);
                if (map.containsKey(compatibility)) {
                    map.get(compatibility).add(candidate);
                } else {
                    List<MoveAttributeRefactoring> refs = new ArrayList<>();
                    refs.add(candidate);
                    map.put(compatibility, refs);
                }
            }
            int maxCompatibility = map.lastKey();
            refactorings.addAll(map.get(maxCompatibility));
        } else if (candidates.size() == 1) {
            refactorings.addAll(candidates);
        }
    }

    private List<MoveAttributeRefactoring> checkForAttributeMoves(List<UMLAttribute> addedAttributes,
                                                                  List<UMLAttribute> removedAttributes) {
        List<MoveAttributeRefactoring> refactorings = new ArrayList<>();
        if (addedAttributes.size() <= removedAttributes.size()) {
            for (UMLAttribute addedAttribute : addedAttributes) {
                List<MoveAttributeRefactoring> candidates = new ArrayList<>();
                for (UMLAttribute removedAttribute : removedAttributes) {
                    MoveAttributeRefactoring candidate = processPairOfAttributes(addedAttribute, removedAttribute);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
                processCandidates(candidates, refactorings);
            }
        } else {
            for (UMLAttribute removedAttribute : removedAttributes) {
                List<MoveAttributeRefactoring> candidates = new ArrayList<>();
                for (UMLAttribute addedAttribute : addedAttributes) {
                    MoveAttributeRefactoring candidate = processPairOfAttributes(addedAttribute, removedAttribute);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
                processCandidates(candidates, refactorings);
            }
        }
        return refactorings;
    }

    private MoveAttributeRefactoring processPairOfAttributes(UMLAttribute addedAttribute,
                                                             UMLAttribute removedAttribute) {
        if (addedAttribute.getName().equals(removedAttribute.getName()) &&
            addedAttribute.getType().equals(removedAttribute.getType())) {
            if (isSubclassOf(removedAttribute.getClassName(), addedAttribute.getClassName())) {
                return new PullUpAttributeRefactoring(removedAttribute, addedAttribute);
            } else if (isSubclassOf(addedAttribute.getClassName(), removedAttribute.getClassName())) {
                return new PushDownAttributeRefactoring(removedAttribute, addedAttribute);
            } else if (sourceClassImportsTargetClass(removedAttribute.getClassName(), addedAttribute.getClassName()) ||
                targetClassImportsSourceClass(removedAttribute.getClassName(), addedAttribute.getClassName())) {
                if (!initializerContainsTypeLiteral(addedAttribute, removedAttribute)) {
                    return new MoveAttributeRefactoring(removedAttribute, addedAttribute);
                }
            }
        }
        return null;
    }

    private boolean initializerContainsTypeLiteral(UMLAttribute addedAttribute, UMLAttribute removedAttribute) {
        VariableDeclaration v1 = addedAttribute.getVariableDeclaration();
        VariableDeclaration v2 = removedAttribute.getVariableDeclaration();
        if (v1.getInitializer() != null && v2.getInitializer() != null) {
            List<String> typeLiterals1 = v1.getInitializer().getTypeLiterals();
            List<String> typeLiterals2 = v2.getInitializer().getTypeLiterals();
            String className1 = addedAttribute.getNonQualifiedClassName();
            String className2 = removedAttribute.getNonQualifiedClassName();
            return typeLiterals1.contains(className1 + ".class") && typeLiterals2.contains(className2 + ".class") &&
                addedAttribute.getType().getClassType().endsWith(
                    "Logger") && removedAttribute.getType().getClassType().endsWith("Logger");
        }
        return false;
    }

    private UMLClassBaseDiff getUMLClassDiffWithAttribute(Replacement pattern) {
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                return classDiff;
            }
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                return classDiff;
            }
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                return classDiff;
            }
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                return classDiff;
            }
        }
        return null;
    }

    private List<UMLClassBaseDiff> getUMLClassDiffWithExistingAttributeAfter(Replacement pattern) {
        List<UMLClassBaseDiff> classDiffs = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                classDiffs.add(classDiff);
            }
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                classDiffs.add(classDiff);
            }
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                classDiffs.add(classDiff);
            }
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                classDiffs.add(classDiff);
            }
        }
        return classDiffs;
    }

    private List<UMLClassBaseDiff> getUMLClassDiffWithNewAttributeAfter(Replacement pattern) {
        List<UMLClassBaseDiff> classDiffs = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                classDiffs.add(classDiff);
            }
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                classDiffs.add(classDiff);
            }
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                classDiffs.add(classDiff);
            }
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
                classDiff.findAttributeInNextClass(pattern.getAfter()) != null) {
                classDiffs.add(classDiff);
            }
        }
        return classDiffs;
    }

    public boolean isSubclassOf(String subclass, String finalSuperclass) {
        return isSubclassOf(subclass, finalSuperclass, new LinkedHashSet<>());
    }

    private boolean checkInheritanceRelationship(UMLType superclass,
                                                 String finalSuperclass,
                                                 Set<String> visitedClasses) {
        if (looksLikeSameType(superclass.getClassType(), finalSuperclass)) {
            return true;
        } else {
            return isSubclassOf(superclass.getClassType(), finalSuperclass, visitedClasses);
        }
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
                if (addedClass != null && addedClass.getSuperclass() != null) {
                    return checkInheritanceRelationship(addedClass.getSuperclass(), finalSuperclass, visitedClasses);
                }
            } else if (subclassDiff.getOldSuperclass() == null && subclassDiff.getNewSuperclass() != null &&
                looksLikeAddedClass(
                    subclassDiff.getNewSuperclass()) != null) {
                UMLClass addedClass = looksLikeAddedClass(subclassDiff.getNewSuperclass());
                return checkInheritanceRelationship(UMLType.extractTypeObject(addedClass.getQualifiedName()),
                                                    finalSuperclass,
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
            if (umlClass.getQualifiedName().equals(className)) {
                return umlClass;
            }
        }
        return null;
    }

    public UMLClass getRemovedClass(String className) {
        for (UMLClass umlClass : removedClasses) {
            if (umlClass.getQualifiedName().equals(className)) {
                return umlClass;
            }
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
        for (UMLFileDiff fileDiff : umlFileDiff) {
            refactorings.addAll(fileDiff.getRefactorings());
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
                    Refactoring refactoring;
                    //if (!removedClass.getName().equals(addedClass.getName())) {
                    if (renameDiff.samePackage()) {
                        refactoring =
                            new RenameClassRefactoring(renameDiff.getOriginalClass(), renameDiff.getRenamedClass());
                    } else {
                        refactoring = new MoveAndRenameClassRefactoring(renameDiff.getOriginalClass(),
                                                                        renameDiff.getRenamedClass());
                    }
                    refactorings.add(refactoring);
                    //}
                }
            }
        }
        for (Replacement pattern : renameMap.keySet()) {
            Set<CandidateAttributeRefactoring> set = renameMap.get(pattern);
            for (CandidateAttributeRefactoring candidate : set) {
                if (candidate.getOriginalVariableDeclaration() != null) {
                    List<UMLClassBaseDiff> diffs1 = getUMLClassDiffWithExistingAttributeAfter(pattern);
                    List<UMLClassBaseDiff> diffs2 = getUMLClassDiffWithNewAttributeAfter(pattern);
                    if (!diffs1.isEmpty()) {
                        UMLClassBaseDiff diff1 = diffs1.get(0);
                        UMLClassBaseDiff originalClassDiff;
                        if (candidate.getOriginalAttribute() != null) {
                            originalClassDiff = getUMLClassDiff(candidate.getOriginalAttribute().getClassName());
                        } else {
                            originalClassDiff = getUMLClassDiff(candidate.getOperationBefore().getClassName());
                        }
                        if (diffs1.size() > 1) {
                            for (UMLClassBaseDiff classDiff : diffs1) {
                                if (isSubclassOf(originalClassDiff.nextClass.getQualifiedName(),
                                                 classDiff.nextClass.getQualifiedName())) {
                                    diff1 = classDiff;
                                    break;
                                }
                            }
                        }
                        UMLAttribute a2 = diff1.findAttributeInNextClass(pattern.getAfter());
                        if (a2 != null) {
                            if (!candidate.getOriginalVariableDeclaration().isAttribute()) {
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
                        UMLClassBaseDiff originalClassDiff;
                        if (candidate.getOriginalAttribute() != null) {
                            originalClassDiff = getUMLClassDiff(candidate.getOriginalAttribute().getClassName());
                        } else {
                            originalClassDiff = getUMLClassDiff(candidate.getOperationBefore().getClassName());
                        }
                        if (diffs2.size() > 1) {
                            for (UMLClassBaseDiff classDiff : diffs2) {
                                if (isSubclassOf(originalClassDiff.nextClass.getQualifiedName(),
                                                 classDiff.nextClass.getQualifiedName())) {
                                    diff2 = classDiff;
                                    break;
                                }
                            }
                        }
                        UMLAttribute a2 = diff2.findAttributeInNextClass(pattern.getAfter());
                        if (a2 != null) {
                            if (!candidate.getOriginalVariableDeclaration().isAttribute()) {
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
        checkForOperationMovesBetweenCommonClasses();
        checkForOperationMovesIncludingAddedClasses();
        checkForOperationMovesIncludingRemovedClasses();
        refactorings.addAll(identifyExtractSuperclassRefactorings());
        refactorings.addAll(identifyExtractClassRefactorings(commonClassDiffList));
        refactorings.addAll(identifyExtractClassRefactorings(classMoveDiffList));
        refactorings.addAll(identifyExtractClassRefactorings(innerClassMoveDiffList));
        refactorings.addAll(identifyExtractClassRefactorings(classRenameDiffList));
        checkForExtractedAndMovedOperations(getOperationBodyMappersInCommonClasses(),
                                            getAddedAndExtractedOperationsInCommonClasses());
        checkForExtractedAndMovedOperations(getOperationBodyMappersInMovedAndRenamedClasses(),
                                            getAddedOperationsInMovedAndRenamedClasses());
        checkForMovedAndInlinedOperations(getOperationBodyMappersInCommonClasses(),
                                          getRemovedAndInlinedOperationsInCommonClasses());

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

    private List<UMLOperation> getAddedOperationsInCommonClasses() {
        List<UMLOperation> addedOperations = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            addedOperations.addAll(classDiff.getAddedOperations());
        }
        return addedOperations;
    }

    private List<UMLOperation> getAddedAndExtractedOperationsInCommonClasses() {
        List<UMLOperation> addedOperations = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            addedOperations.addAll(classDiff.getAddedOperations());
            for (Refactoring ref : classDiff.getRefactorings()) {
                if (ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring extractRef = (ExtractOperationRefactoring) ref;
                    addedOperations.add(extractRef.getExtractedOperation());
                }
            }
        }
        return addedOperations;
    }

    private List<UMLOperation> getAddedOperationsInMovedAndRenamedClasses() {
        List<UMLOperation> addedOperations = new ArrayList<>();
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            addedOperations.addAll(classDiff.getAddedOperations());
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            addedOperations.addAll(classDiff.getAddedOperations());
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            addedOperations.addAll(classDiff.getAddedOperations());
        }
        return addedOperations;
    }

    private List<UMLOperation> getRemovedOperationsInCommonClasses() {
        List<UMLOperation> removedOperations = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            removedOperations.addAll(classDiff.getRemovedOperations());
        }
        return removedOperations;
    }

    private void checkForOperationMovesIncludingRemovedClasses() throws RefactoringMinerTimedOutException {
        List<UMLOperation> addedOperations = getAddedAndExtractedOperationsInCommonClasses();
        List<UMLOperation> removedOperations = getRemovedOperationsInCommonClasses();
        for (UMLClass removedClass : removedClasses) {
            removedOperations.addAll(removedClass.getOperations());
        }
        if (removedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS || addedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS) {
            checkForOperationMoves(addedOperations, removedOperations);
        }
    }

    private void checkForOperationMovesIncludingAddedClasses() throws RefactoringMinerTimedOutException {
        List<UMLOperation> addedOperations = getAddedOperationsInCommonClasses();
        for (UMLClass addedClass : addedClasses) {
            addedOperations.addAll(addedClass.getOperations());
        }
        List<UMLOperation> removedOperations = getRemovedOperationsInCommonClasses();
        if (removedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS || addedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS) {
            checkForOperationMoves(addedOperations, removedOperations);
        }
    }


    private void checkForMovedAndInlinedOperations(List<UMLOperationBodyMapper> mappers,
                                                   List<UMLOperation> removedOperations) throws
        RefactoringMinerTimedOutException {
        for (UMLOperation removedOperation : removedOperations) {
            for (UMLOperationBodyMapper mapper : mappers) {
                if (!mapper.getNonMappedLeavesT2().isEmpty() || !mapper.getNonMappedInnerNodesT2().isEmpty() ||
                    !mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
                    List<OperationInvocation> operationInvocations =
                        mapper.getOperation1().getAllOperationInvocations();
                    List<OperationInvocation> removedOperationInvocations = new ArrayList<>();
                    for (OperationInvocation invocation : operationInvocations) {
                        if (invocation.matchesOperation(removedOperation, mapper.getOperation1().variableTypeMap(),
                                                        this)) {
                            removedOperationInvocations.add(invocation);
                        }
                    }
                    if (removedOperationInvocations.size() > 0 && !invocationMatchesWithAddedOperation(
                        removedOperationInvocations.get(0), mapper.getOperation1().variableTypeMap(),
                        mapper.getOperation2().getAllOperationInvocations())) {
                        OperationInvocation removedOperationInvocation = removedOperationInvocations.get(0);
                        List<String> arguments = removedOperationInvocation.getArguments();
                        List<String> parameters = removedOperation.getParameterNameList();
                        Map<String, String> parameterToArgumentMap = new LinkedHashMap<>();
                        //special handling for methods with varargs parameter for which no argument is passed in the matching invocation
                        int size = Math.min(arguments.size(), parameters.size());
                        for (int i = 0; i < size; i++) {
                            parameterToArgumentMap.put(parameters.get(i), arguments.get(i));
                        }
                        UMLOperationBodyMapper operationBodyMapper =
                            new UMLOperationBodyMapper(removedOperation, mapper, parameterToArgumentMap,
                                                       getUMLClassDiff(removedOperation.getClassName()));
                        if (moveAndInlineMatchCondition(operationBodyMapper, mapper)) {
                            InlineOperationRefactoring inlineOperationRefactoring =
                                new InlineOperationRefactoring(operationBodyMapper, mapper.getOperation1(),
                                                               removedOperationInvocations);
                            refactorings.add(inlineOperationRefactoring);
                            deleteRemovedOperation(removedOperation);
                        }
                    }
                }
            }
        }
    }

    private boolean invocationMatchesWithAddedOperation(OperationInvocation removedOperationInvocation,
                                                        Map<String, UMLType> variableTypeMap,
                                                        List<OperationInvocation> operationInvocationsInNewMethod) {
        if (operationInvocationsInNewMethod.contains(removedOperationInvocation)) {
            for (UMLOperation addedOperation : getAddedOperationsInCommonClasses()) {
                if (removedOperationInvocation.matchesOperation(addedOperation, variableTypeMap, this)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<UMLOperationBodyMapper> getOperationBodyMappersInCommonClasses() {
        List<UMLOperationBodyMapper> mappers = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            mappers.addAll(classDiff.getOperationBodyMapperList());
        }
        return mappers;
    }

    private void checkForExtractedAndMovedOperations(List<UMLOperationBodyMapper> mappers,
                                                     List<UMLOperation> addedOperations) throws
        RefactoringMinerTimedOutException {
        for (UMLOperation addedOperation : addedOperations) {
            for (UMLOperationBodyMapper mapper : mappers) {
                if ((mapper.nonMappedElementsT1() > 0 || !mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) &&
                    !mapper.containsExtractOperationRefactoring(addedOperation)) {
                    List<OperationInvocation> operationInvocations =
                        ExtractOperationDetection.getInvocationsInSourceOperationAfterExtraction(mapper);
                    List<OperationInvocation> addedOperationInvocations = new ArrayList<>();
                    for (OperationInvocation invocation : operationInvocations) {
                        if (invocation.matchesOperation(addedOperation, mapper.getOperation2().variableTypeMap(),
                                                        this)) {
                            addedOperationInvocations.add(invocation);
                        }
                    }
                    if (addedOperationInvocations.size() > 0) {
                        OperationInvocation addedOperationInvocation = addedOperationInvocations.get(0);
                        List<String> arguments = addedOperationInvocation.getArguments();
                        List<String> parameters = addedOperation.getParameterNameList();
                        Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<>();
                        //special handling for methods with varargs parameter for which no argument is passed in the matching invocation
                        int size = Math.min(arguments.size(), parameters.size());
                        for (int i = 0; i < size; i++) {
                            parameterToArgumentMap2.put(parameters.get(i), arguments.get(i));
                        }
                        String className = mapper.getOperation2().getClassName();
                        List<UMLAttribute> attributes = new ArrayList<>();
                        if (className.contains(".") && isNumeric(
                            className.substring(className.lastIndexOf(".") + 1))) {
                            //add enclosing class fields + anonymous class fields
                            UMLClassBaseDiff umlClassDiff =
                                getUMLClassDiff(className.substring(0, className.lastIndexOf(".")));
                            attributes.addAll(
                                umlClassDiff.originalClassAttributesOfType(addedOperation.getClassName()));
/*                         TODO:   for (UMLAnonymousClass anonymous : umlClassDiff.getOriginalClass()
.getAnonymousClassList()) {
                                if (anonymous.getName().equals(className)) {
                                    attributes.addAll(anonymous.attributesOfType(addedOperation.getClassName()));
                                    break;
                                }
                            }*/
                        } else {
                            UMLClassBaseDiff umlClassDiff = getUMLClassDiff(className);
                            if (umlClassDiff == null) {
                                for (UMLClassDiff classDiff : commonClassDiffList) {
/*                     TODO:               for (UMLAnonymousClass anonymousClass : classDiff.getAddedAnonymousClasses()) {
                                        if (className.equals(anonymousClass.getCodePath())) {
                                            umlClassDiff = classDiff;
                                            attributes.addAll(
                                                anonymousClass.attributesOfType(addedOperation.getClassName()));
                                            break;
                                        }
                                    }*/
                                    if (umlClassDiff != null) {
                                        break;
                                    }
                                }
                            }
                            attributes.addAll(
                                umlClassDiff.originalClassAttributesOfType(addedOperation.getClassName()));
                        }
                        Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<>();
                        for (UMLAttribute attribute : attributes) {
                            parameterToArgumentMap1.put(attribute.getName() + ".", "");
                            parameterToArgumentMap2.put("this.", "");
                        }
                        if (addedOperationInvocation.getExpression() != null) {
                            parameterToArgumentMap1.put(addedOperationInvocation.getExpression() + ".", "");
                            parameterToArgumentMap2.put("this.", "");
                        }
                        UMLOperationBodyMapper operationBodyMapper =
                            new UMLOperationBodyMapper(mapper, addedOperation, parameterToArgumentMap1,
                                                       parameterToArgumentMap2,
                                                       getUMLClassDiff(addedOperation.getClassName()));
                        if (!anotherAddedMethodExistsWithBetterMatchingInvocationExpression(addedOperationInvocation,
                                                                                            addedOperation,
                                                                                            addedOperations) &&
                            !conflictingExpression(addedOperationInvocation, addedOperation,
                                                   mapper.getOperation2().variableTypeMap()) &&
                            extractAndMoveMatchCondition(operationBodyMapper, mapper)) {
                            if (className.equals(addedOperation.getClassName())) {
                                //extract inside moved or renamed class
                                ExtractOperationRefactoring extractOperationRefactoring =
                                    new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(),
                                                                    addedOperationInvocations);
                                refactorings.add(extractOperationRefactoring);
                                deleteAddedOperation(addedOperation);
                            } else if (isSubclassOf(className, addedOperation.getClassName())) {
                                //extract and pull up method
                                ExtractOperationRefactoring extractOperationRefactoring =
                                    new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(),
                                                                    addedOperationInvocations);
                                refactorings.add(extractOperationRefactoring);
                                deleteAddedOperation(addedOperation);
                            } else if (isSubclassOf(addedOperation.getClassName(), className)) {
                                //extract and push down method
                                ExtractOperationRefactoring extractOperationRefactoring =
                                    new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(),
                                                                    addedOperationInvocations);
                                refactorings.add(extractOperationRefactoring);
                                deleteAddedOperation(addedOperation);
                            } else if (addedOperation.getClassName().startsWith(className + ".")) {
                                //extract and move to inner class
                                ExtractOperationRefactoring extractOperationRefactoring =
                                    new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(),
                                                                    addedOperationInvocations);
                                refactorings.add(extractOperationRefactoring);
                                deleteAddedOperation(addedOperation);
                            } else if (className.startsWith(addedOperation.getClassName() + ".")) {
                                //extract and move to outer class
                                ExtractOperationRefactoring extractOperationRefactoring =
                                    new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(),
                                                                    addedOperationInvocations);
                                refactorings.add(extractOperationRefactoring);
                                deleteAddedOperation(addedOperation);
                            } else if (sourceClassImportsTargetClass(className, addedOperation.getClassName()) ||
                                sourceClassImportsSuperclassOfTargetClass(className, addedOperation.getClassName()) ||
                                targetClassImportsSourceClass(className, addedOperation.getClassName())) {
                                //extract and move
                                ExtractOperationRefactoring extractOperationRefactoring =
                                    new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(),
                                                                    addedOperationInvocations);
                                refactorings.add(extractOperationRefactoring);
                                deleteAddedOperation(addedOperation);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<UMLOperationBodyMapper> getOperationBodyMappersInMovedAndRenamedClasses() {
        List<UMLOperationBodyMapper> mappers = new ArrayList<>();
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            mappers.addAll(classDiff.getOperationBodyMapperList());
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            mappers.addAll(classDiff.getOperationBodyMapperList());
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            mappers.addAll(classDiff.getOperationBodyMapperList());
        }
        return mappers;
    }

    private boolean moveAndInlineMatchCondition(UMLOperationBodyMapper operationBodyMapper,
                                                UMLOperationBodyMapper parentMapper) {
        List<AbstractCodeMapping> mappingList = new ArrayList<>(operationBodyMapper.getMappings());
        if ((operationBodyMapper.getOperation1().isGetter() || operationBodyMapper.getOperation1().isDelegate() != null) && mappingList.size() == 1) {
            List<AbstractCodeMapping> parentMappingList =
                new ArrayList<>(parentMapper.getMappings());
            for (AbstractCodeMapping mapping : parentMappingList) {
                if (mapping.getFragment2().equals(mappingList.get(0).getFragment2())) {
                    return false;
                }
                if (mapping instanceof CompositeStatementObjectMapping) {
                    CompositeStatementObjectMapping compositeMapping = (CompositeStatementObjectMapping) mapping;
                    CompositeStatementObject fragment2 = (CompositeStatementObject) compositeMapping.getFragment2();
                    for (AbstractExpression expression : fragment2.getExpressions()) {
                        if (expression.equals(mappingList.get(0).getFragment2())) {
                            return false;
                        }
                    }
                }
            }
        }
        int delegateStatements = 0;
        for (StatementObject statement : operationBodyMapper.getNonMappedLeavesT1()) {
            OperationInvocation invocation = statement.invocationCoveringEntireFragment();
            if (invocation != null && invocation.matchesOperation(operationBodyMapper.getOperation1())) {
                delegateStatements++;
            }
        }
        int mappings = operationBodyMapper.mappingsWithoutBlocks();
        int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1() - delegateStatements;
        List<AbstractCodeMapping> exactMatchList = operationBodyMapper.getExactMatches();
        int exactMatches = exactMatchList.size();
        return mappings > 0 && (mappings > nonMappedElementsT1 ||
            (exactMatches == 1 && !exactMatchList.get(
                0).getFragment1().throwsNewException() && nonMappedElementsT1 - exactMatches < 10) ||
            (exactMatches > 1 && nonMappedElementsT1 - exactMatches < 20));
    }


    private List<UMLOperation> getRemovedAndInlinedOperationsInCommonClasses() {
        List<UMLOperation> removedOperations = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            removedOperations.addAll(classDiff.getRemovedOperations());
            for (Refactoring ref : classDiff.getRefactorings()) {
                if (ref instanceof InlineOperationRefactoring) {
                    InlineOperationRefactoring extractRef = (InlineOperationRefactoring) ref;
                    removedOperations.add(extractRef.getInlinedOperation());
                }
            }
        }
        return removedOperations;
    }

    private void checkForOperationMovesBetweenCommonClasses() throws RefactoringMinerTimedOutException {
        List<UMLOperation> addedOperations = getAddedAndExtractedOperationsInCommonClasses();
        List<UMLOperation> removedOperations = getRemovedOperationsInCommonMovedRenamedClasses();
        if (removedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS || addedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS) {
            checkForOperationMoves(addedOperations, removedOperations);
        }
    }

    private List<UMLOperation> getRemovedOperationsInCommonMovedRenamedClasses() {
        List<UMLOperation> removedOperations = new ArrayList<>();
        for (UMLClassDiff classDiff : commonClassDiffList) {
            removedOperations.addAll(classDiff.getRemovedOperations());
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            removedOperations.addAll(classDiff.getRemovedOperations());
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            removedOperations.addAll(classDiff.getRemovedOperations());
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            removedOperations.addAll(classDiff.getRemovedOperations());
        }
        return removedOperations;
    }

    private List<Refactoring> getRenameClassRefactorings() {
        List<Refactoring> refactorings = new ArrayList<>();
        for (UMLClassRenameDiff classRenameDiff : classRenameDiffList) {
            Refactoring refactoring;
            if (!classRenameDiff.getOriginalClass().getName().equals(classRenameDiff.getRenamedClass().getName())) {
                if (classRenameDiff.samePackage()) {
                    refactoring = new RenameClassRefactoring(classRenameDiff.getOriginalClass(),
                                                             classRenameDiff.getRenamedClass());
                } else {
                    refactoring = new MoveAndRenameClassRefactoring(classRenameDiff.getOriginalClass(),
                                                                    classRenameDiff.getRenamedClass());
                }
                refactorings.add(refactoring);
            }
        }
        return refactorings;
    }

    private UMLClassBaseDiff getUMLClassDiff(String className) {
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.matches(className)) {
                return classDiff;
            }
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.matches(className)) {
                return classDiff;
            }
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.matches(className)) {
                return classDiff;
            }
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.matches(className)) {
                return classDiff;
            }
        }
        return null;
    }

    private UMLClassBaseDiff getUMLClassDiff(UMLType type) {
        for (UMLClassDiff classDiff : commonClassDiffList) {
            if (classDiff.matches(type)) {
                return classDiff;
            }
        }
        for (UMLClassMoveDiff classDiff : classMoveDiffList) {
            if (classDiff.matches(type)) {
                return classDiff;
            }
        }
        for (UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
            if (classDiff.matches(type)) {
                return classDiff;
            }
        }
        for (UMLClassRenameDiff classDiff : classRenameDiffList) {
            if (classDiff.matches(type)) {
                return classDiff;
            }
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
            String originalName = originalClass.getQualifiedName();
            UMLClass movedClass = classMoveDiff.getMovedClass();
            String movedName = movedClass.getQualifiedName();

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
                if (!removedClass.getQualifiedName().equals(addedClass.getQualifiedName()) &&
                    matcher.match(removedClass, addedClass, renamedFile)) {
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

    private void checkForOperationMoves(List<UMLOperation> addedOperations, List<UMLOperation> removedOperations) throws
        RefactoringMinerTimedOutException {
        if (addedOperations.size() <= removedOperations.size()) {
            for (Iterator<UMLOperation> addedOperationIterator =
                 addedOperations.iterator(); addedOperationIterator.hasNext(); ) {
                UMLOperation addedOperation = addedOperationIterator.next();
                TreeMap<Integer, List<UMLOperationBodyMapper>> operationBodyMapperMap = new TreeMap<>();
                for (UMLOperation removedOperation : removedOperations) {
                    UMLOperationBodyMapper operationBodyMapper =
                        new UMLOperationBodyMapper(removedOperation, addedOperation,
                                                   getUMLClassDiff(removedOperation.getClassName()));
                    int mappings = operationBodyMapper.mappingsWithoutBlocks();
                    if (mappings > 0 && mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper)) {
                        int exactMatches = operationBodyMapper.exactMatches();
                        if (operationBodyMapperMap.containsKey(exactMatches)) {
                            List<UMLOperationBodyMapper> mapperList = operationBodyMapperMap.get(exactMatches);
                            mapperList.add(operationBodyMapper);
                        } else {
                            List<UMLOperationBodyMapper> mapperList = new ArrayList<>();
                            mapperList.add(operationBodyMapper);
                            operationBodyMapperMap.put(exactMatches, mapperList);
                        }
                    }
                }
                if (!operationBodyMapperMap.isEmpty()) {
                    List<UMLOperationBodyMapper> firstMappers = firstMappers(operationBodyMapperMap);
                    firstMappers.sort(new UMLOperationBodyMapperComparator());
                    addedOperationIterator.remove();
                    boolean sameSourceAndTargetClass = sameSourceAndTargetClass(firstMappers);
                    if (sameSourceAndTargetClass) {
                        TreeSet<UMLOperationBodyMapper> set;
                        if (allRenamedOperations(firstMappers)) {
                            set = new TreeSet<>();
                        } else {
                            set = new TreeSet<>(new UMLOperationBodyMapperComparator());
                        }
                        set.addAll(firstMappers);
                        UMLOperationBodyMapper bestMapper = set.first();
                        firstMappers.clear();
                        firstMappers.add(bestMapper);
                    }
                    for (UMLOperationBodyMapper firstMapper : firstMappers) {
                        UMLOperation removedOperation = firstMapper.getOperation1();
                        if (sameSourceAndTargetClass) {
                            removedOperations.remove(removedOperation);
                        }

                        Refactoring refactoring = null;
                        if (removedOperation.isConstructor() == addedOperation.isConstructor() &&
                            isSubclassOf(removedOperation.getClassName(),
                                         addedOperation.getClassName()) && addedOperation.compatibleSignature(
                            removedOperation)) {
                            refactoring = new PullUpOperationRefactoring(firstMapper);
                        } else if (removedOperation.isConstructor() == addedOperation.isConstructor() &&
                            isSubclassOf(addedOperation.getClassName(),
                                         removedOperation.getClassName()) && addedOperation.compatibleSignature(
                            removedOperation)) {
                            refactoring = new PushDownOperationRefactoring(firstMapper);
                        } else if (removedOperation.isConstructor() == addedOperation.isConstructor() &&
                            !addedOperation.getClassName().equals(removedOperation.getClassName()) &&
                            movedMethodSignature(removedOperation, addedOperation) &&
                            !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation)) {
                            refactoring = new MoveOperationRefactoring(firstMapper);
                        } else if (removedOperation.isConstructor() == addedOperation.isConstructor() &&
                            !addedOperation.getClassName().equals(removedOperation.getClassName()) &&
                            movedAndRenamedMethodSignature(removedOperation, addedOperation, firstMapper) &&
                            !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation)) {
                            refactoring = new MoveOperationRefactoring(firstMapper);
                        }
                        if (refactoring != null) {
                            deleteRemovedOperation(removedOperation);
                            deleteAddedOperation(addedOperation);
                            UMLOperationDiff operationSignatureDiff =
                                new UMLOperationDiff(removedOperation, addedOperation, firstMapper.getMappings());
                            refactorings.addAll(operationSignatureDiff.getRefactorings());
                            refactorings.add(refactoring);
                            UMLClass addedClass = getAddedClass(addedOperation.getClassName());
                            if (addedClass != null) {
                                checkForExtractedOperationsWithinMovedMethod(firstMapper, addedClass);
                            }
                        }
                    }
                }
            }
        } else {
            for (Iterator<UMLOperation> removedOperationIterator =
                 removedOperations.iterator(); removedOperationIterator.hasNext(); ) {
                UMLOperation removedOperation = removedOperationIterator.next();
                TreeMap<Integer, List<UMLOperationBodyMapper>> operationBodyMapperMap =
                    new TreeMap<>();
                for (UMLOperation addedOperation : addedOperations) {
                    UMLOperationBodyMapper operationBodyMapper =
                        new UMLOperationBodyMapper(removedOperation, addedOperation,
                                                   getUMLClassDiff(removedOperation.getClassName()));
                    int mappings = operationBodyMapper.mappingsWithoutBlocks();
                    if (mappings > 0 && mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper)) {
                        int exactMatches = operationBodyMapper.exactMatches();
                        if (operationBodyMapperMap.containsKey(exactMatches)) {
                            List<UMLOperationBodyMapper> mapperList = operationBodyMapperMap.get(exactMatches);
                            mapperList.add(operationBodyMapper);
                        } else {
                            List<UMLOperationBodyMapper> mapperList = new ArrayList<>();
                            mapperList.add(operationBodyMapper);
                            operationBodyMapperMap.put(exactMatches, mapperList);
                        }
                    }
                }
                if (!operationBodyMapperMap.isEmpty()) {
                    List<UMLOperationBodyMapper> firstMappers = firstMappers(operationBodyMapperMap);
                    firstMappers.sort(new UMLOperationBodyMapperComparator());
                    removedOperationIterator.remove();
                    boolean sameSourceAndTargetClass = sameSourceAndTargetClass(firstMappers);
                    if (sameSourceAndTargetClass) {
                        TreeSet<UMLOperationBodyMapper> set;
                        if (allRenamedOperations(firstMappers)) {
                            set = new TreeSet<>();
                        } else {
                            set = new TreeSet<>(new UMLOperationBodyMapperComparator());
                        }
                        set.addAll(firstMappers);
                        UMLOperationBodyMapper bestMapper = set.first();
                        firstMappers.clear();
                        firstMappers.add(bestMapper);
                    }
                    for (UMLOperationBodyMapper firstMapper : firstMappers) {
                        UMLOperation addedOperation = firstMapper.getOperation2();
                        if (sameSourceAndTargetClass) {
                            addedOperations.remove(addedOperation);
                        }

                        Refactoring refactoring = null;
                        if (removedOperation.isConstructor() == addedOperation.isConstructor() &&
                            isSubclassOf(removedOperation.getClassName(),
                                         addedOperation.getClassName()) && addedOperation.compatibleSignature(
                            removedOperation)) {
                            refactoring = new PullUpOperationRefactoring(firstMapper);
                        } else if (removedOperation.isConstructor() == addedOperation.isConstructor() &&
                            isSubclassOf(addedOperation.getClassName(),
                                         removedOperation.getClassName()) && addedOperation.compatibleSignature(
                            removedOperation)) {
                            refactoring = new PushDownOperationRefactoring(firstMapper);
                        } else if (removedOperation.isConstructor() == addedOperation.isConstructor() &&
                            !addedOperation.getClassName().equals(removedOperation.getClassName()) &&
                            movedMethodSignature(removedOperation, addedOperation) &&
                            !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation)) {
                            refactoring = new MoveOperationRefactoring(firstMapper);
                        } else if (removedOperation.isConstructor() == addedOperation.isConstructor() &&
                            !addedOperation.getClassName().equals(removedOperation.getClassName()) &&
                            movedAndRenamedMethodSignature(removedOperation, addedOperation, firstMapper) &&
                            !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation)) {
                            refactoring = new MoveOperationRefactoring(firstMapper);
                        }
                        if (refactoring != null) {
                            deleteRemovedOperation(removedOperation);
                            deleteAddedOperation(addedOperation);
                            UMLOperationDiff operationSignatureDiff =
                                new UMLOperationDiff(removedOperation, addedOperation, firstMapper.getMappings());
                            refactorings.addAll(operationSignatureDiff.getRefactorings());
                            refactorings.add(refactoring);
                        }
                    }
                }
            }
        }
    }

    private void checkForExtractedOperationsWithinMovedMethod(UMLOperationBodyMapper movedMethodMapper,
                                                              UMLClass addedClass) throws
        RefactoringMinerTimedOutException {
        UMLOperation removedOperation = movedMethodMapper.getOperation1();
        UMLOperation addedOperation = movedMethodMapper.getOperation2();
        List<OperationInvocation> removedInvocations = removedOperation.getAllOperationInvocations();
        List<OperationInvocation> addedInvocations = addedOperation.getAllOperationInvocations();
        Set<OperationInvocation> intersection = new LinkedHashSet<>(removedInvocations);
        intersection.retainAll(addedInvocations);
        Set<OperationInvocation> newInvocations = new LinkedHashSet<>(addedInvocations);
        newInvocations.removeAll(intersection);
        for (OperationInvocation newInvocation : newInvocations) {
            for (UMLOperation operation : addedClass.getOperations()) {
                if (!operation.isAbstract() && !operation.hasEmptyBody() &&
                    newInvocation.matchesOperation(operation, addedOperation.variableTypeMap(), this)) {
                    ExtractOperationDetection detection = new ExtractOperationDetection(movedMethodMapper,
                                                                                        addedClass.getOperations(),
                                                                                        getUMLClassDiff(
                                                                                            operation.getClassName()),
                                                                                        this);
                    List<ExtractOperationRefactoring> refs = detection.check(operation);
                    this.refactorings.addAll(refs);
                }
            }
        }
    }

    private boolean movedAndRenamedMethodSignature(UMLOperation removedOperation,
                                                   UMLOperation addedOperation,
                                                   UMLOperationBodyMapper mapper) {
        UMLClassBaseDiff removedOperationClassDiff = getUMLClassDiff(removedOperation.getClassName());
        if (removedOperationClassDiff != null && removedOperationClassDiff.containsOperationWithTheSameSignatureInNextClass(
            removedOperation)) {
            return false;
        }
/*      TODO:  if ((removedOperation.isGetter() || removedOperation.isSetter() || addedOperation.isGetter() ||
addedOperation.isSetter()) &&
            mapper.mappingsWithoutBlocks() == 1 && mapper.getMappings().size() == 1) {
            if (!mapper.getMappings().iterator().next().isExact()) {
                return false;
            }
        }*/
        if ((removedOperation.isConstructor() || addedOperation.isConstructor()) && mapper.mappingsWithoutBlocks() > 0) {
            if (!(UMLClassBaseDiff.allMappingsAreExactMatches(
                mapper) && mapper.nonMappedElementsT1() == 0 && mapper.nonMappedElementsT2() == 0)) {
                return false;
            }
        }
        int exactLeafMappings = 0;
        for (AbstractCodeMapping mapping : mapper.getMappings()) {
            if (mapping instanceof LeafMapping && mapping.isExact() && !mapping.getFragment1().getString().startsWith(
                "return ")) {
                exactLeafMappings++;
            }
        }
        double normalizedEditDistance = mapper.normalizedEditDistance();
        if (exactLeafMappings == 0 && normalizedEditDistance > 0.24) {
            return false;
        }
        if (exactLeafMappings == 1 && normalizedEditDistance > 0.5 && (mapper.nonMappedElementsT1() > 0 || mapper.nonMappedElementsT2() > 0)) {
            return false;
        }
        if (mapper.mappingsWithoutBlocks() == 1) {
            for (AbstractCodeMapping mapping : mapper.getMappings()) {
                String fragment1 = mapping.getFragment1().getString();
                String fragment2 = mapping.getFragment2().getString();
                if (fragment1.startsWith("return True") || fragment1.startsWith(
                    "return False") || fragment1.startsWith("return this") || fragment1.startsWith(
                    "return null") || fragment1.startsWith("return") ||
                    fragment2.startsWith("return True") || fragment2.startsWith(
                    "return False") || fragment2.startsWith("return this") || fragment2.startsWith(
                    "return null") || fragment2.startsWith("return")) {
                    return false;
                }
            }
        }
        if (addedOperation.isAbstract() == removedOperation.isAbstract() &&
            addedOperation.getTypeParameters().equals(removedOperation.getTypeParameters())) {
            List<UMLType> addedOperationParameterTypeList = addedOperation.getParameterTypeList();
            List<UMLType> removedOperationParameterTypeList = removedOperation.getParameterTypeList();
            if (addedOperationParameterTypeList.equals(
                removedOperationParameterTypeList) && addedOperationParameterTypeList.size() > 0) {
                return true;
            } else {
                // ignore parameters of types sourceClass and targetClass
                List<UMLParameter> oldParameters = new ArrayList<>();
                Set<String> oldParameterNames = new LinkedHashSet<>();
                for (UMLParameter oldParameter : removedOperation.getParameters()) {
                    if (!oldParameter.getKind().equals("return")
                        && !looksLikeSameType(oldParameter.getType().getClassType(), addedOperation.getClassName())
                        && !looksLikeSameType(oldParameter.getType().getClassType(), removedOperation.getClassName())) {
                        oldParameters.add(oldParameter);
                        oldParameterNames.add(oldParameter.getName());
                    }
                }
                List<UMLParameter> newParameters = new ArrayList<>();
                Set<String> newParameterNames = new LinkedHashSet<>();
                for (UMLParameter newParameter : addedOperation.getParameters()) {
                    if (!newParameter.getKind().equals("return") &&
                        !looksLikeSameType(newParameter.getType().getClassType(), addedOperation.getClassName()) &&
                        !looksLikeSameType(newParameter.getType().getClassType(), removedOperation.getClassName())) {
                        newParameters.add(newParameter);
                        newParameterNames.add(newParameter.getName());
                    }
                }
                Set<String> intersection = new LinkedHashSet<>(oldParameterNames);
                intersection.retainAll(newParameterNames);
                boolean parameterMatch = oldParameters.equals(newParameters) || oldParameters.containsAll(
                    newParameters) || newParameters.containsAll(oldParameters) || intersection.size() > 0 ||
                    removedOperation.isStatic() || addedOperation.isStatic();
                return (parameterMatch && oldParameters.size() > 0 && newParameters.size() > 0) ||
                    (parameterMatch && addedOperation.equalReturnParameter(
                        removedOperation) && (oldParameters.size() == 0 || newParameters.size() == 0));
            }
        }
        return false;
    }

    private String isRenamedClass(UMLClass umlClass) {
        for (UMLClassRenameDiff renameDiff : classRenameDiffList) {
            if (renameDiff.getOriginalClass().equals(umlClass))
                return renameDiff.getRenamedClass().getQualifiedName();
        }
        return null;
    }

    private String isMovedClass(UMLClass umlClass) {
        for (UMLClassMoveDiff moveDiff : classMoveDiffList) {
            if (moveDiff.getOriginalClass().equals(umlClass))
                return moveDiff.getMovedClass().getQualifiedName();
        }
        return null;
    }

    public void checkForGeneralizationChanges() {
        for (Iterator<UMLGeneralization> removedGeneralizationIterator =
             removedGeneralizations.iterator(); removedGeneralizationIterator.hasNext(); ) {
            UMLGeneralization removedGeneralization = removedGeneralizationIterator.next();
            for (Iterator<UMLGeneralization> addedGeneralizationIterator =
                 addedGeneralizations.iterator(); addedGeneralizationIterator.hasNext(); ) {
                UMLGeneralization addedGeneralization = addedGeneralizationIterator.next();
                String renamedChild = isRenamedClass(removedGeneralization.getChild());
                String movedChild = isMovedClass(removedGeneralization.getChild());
                if (removedGeneralization.getChild().equals(addedGeneralization.getChild())) {
                    UMLGeneralizationDiff generalizationDiff =
                        new UMLGeneralizationDiff(removedGeneralization, addedGeneralization);
                    addedGeneralizationIterator.remove();
                    removedGeneralizationIterator.remove();
                    generalizationDiffList.add(generalizationDiff);
                    break;
                }
                if ((renamedChild != null && renamedChild.equals(addedGeneralization.getChild().getQualifiedName())) ||
                    (movedChild != null && movedChild.equals(addedGeneralization.getChild().getQualifiedName()))) {
                    UMLGeneralizationDiff generalizationDiff =
                        new UMLGeneralizationDiff(removedGeneralization, addedGeneralization);
                    addedGeneralizationIterator.remove();
                    removedGeneralizationIterator.remove();
                    generalizationDiffList.add(generalizationDiff);
                    break;
                }
            }
        }
    }

    private ExtractClassRefactoring atLeastOneCommonAttributeOrOperation(UMLClass umlClass,
                                                                         UMLClassBaseDiff classDiff,
                                                                         UMLAttribute attributeOfExtractedClassType) {
        Set<UMLOperation> commonOperations = new LinkedHashSet<>();
        for (UMLOperation operation : classDiff.getRemovedOperations()) {
            if (!operation.isConstructor() && !operation.overridesObject()) {
                if (umlClass.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
                    commonOperations.add(operation);
                }
            }
        }
        Set<UMLAttribute> commonAttributes = new LinkedHashSet<>();
        for (UMLAttribute attribute : classDiff.getRemovedAttributes()) {
            if (umlClass.containsAttributeWithTheSameNameIgnoringChangedType(attribute)) {
                commonAttributes.add(attribute);
            }
        }
        int threshold = 1;
        if (attributeOfExtractedClassType != null)
            threshold = 0;
        if (commonOperations.size() > threshold || commonAttributes.size() > threshold) {
            return new ExtractClassRefactoring(umlClass, classDiff, commonOperations, commonAttributes,
                                               attributeOfExtractedClassType);
        }
        return null;
    }

    private List<ExtractClassRefactoring> identifyExtractClassRefactorings(List<? extends UMLClassBaseDiff> classDiffs) throws
        RefactoringMinerTimedOutException {
        List<ExtractClassRefactoring> refactorings = new ArrayList<>();
        for (UMLClass addedClass : addedClasses) {
            TreeSet<CandidateExtractClassRefactoring> candidates = new TreeSet<>();
            UMLType addedClassSuperType = addedClass.getSuperclass();
            if (!addedClass.isInterface()) {
                for (UMLClassBaseDiff classDiff : classDiffs) {
                    UMLType classDiffSuperType = classDiff.getNewSuperclass();
                    boolean commonSuperType = addedClassSuperType != null && classDiffSuperType != null &&
                        addedClassSuperType.getClassType().equals(classDiffSuperType.getClassType());
                    boolean commonInterface = false;
                    for (UMLType addedClassInterface : addedClass.getImplementedInterfaces()) {
                        for (UMLType classDiffInterface : classDiff.getNextClass().getImplementedInterfaces()) {
                            if (addedClassInterface.getClassType().equals(classDiffInterface.getClassType())) {
                                commonInterface = true;
                                break;
                            }
                        }
                        if (commonInterface)
                            break;
                    }
                    boolean extendsAddedClass = classDiff.getNewSuperclass() != null &&
                        addedClass.getQualifiedName().endsWith("." + classDiff.getNewSuperclass().getClassType());
                    UMLAttribute attributeOfExtractedClassType = attributeOfExtractedClassType(addedClass, classDiff);
                    boolean isTestClass = addedClass.isTestClass() && classDiff.getOriginalClass().isTestClass();
                    if ((!commonSuperType && !commonInterface && !extendsAddedClass) || attributeOfExtractedClassType != null || isTestClass) {
                        ExtractClassRefactoring refactoring =
                            atLeastOneCommonAttributeOrOperation(addedClass, classDiff, attributeOfExtractedClassType);
                        if (refactoring != null) {
                            CandidateExtractClassRefactoring candidate =
                                new CandidateExtractClassRefactoring(classDiff, refactoring);
                            candidates.add(candidate);
                        }
                    }
                }
            }
            if (!candidates.isEmpty()) {
                CandidateExtractClassRefactoring firstCandidate = candidates.first();
                if (firstCandidate.innerClassExtract() || firstCandidate.subclassExtract()) {
                    detectSubRefactorings(firstCandidate.getClassDiff(),
                                          firstCandidate.getRefactoring().getExtractedClass(),
                                          firstCandidate.getRefactoring().getRefactoringType());
                    refactorings.add(firstCandidate.getRefactoring());
                } else {
                    for (CandidateExtractClassRefactoring candidate : candidates) {
                        detectSubRefactorings(candidate.getClassDiff(),
                                              candidate.getRefactoring().getExtractedClass(),
                                              candidate.getRefactoring().getRefactoringType());
                        refactorings.add(candidate.getRefactoring());
                    }
                }
            }
        }
        return refactorings;
    }

    private UMLAttribute attributeOfExtractedClassType(UMLClass umlClass, UMLClassBaseDiff classDiff) {
        List<UMLAttribute> addedAttributes = classDiff.getAddedAttributes();
        for (UMLAttribute addedAttribute : addedAttributes) {
            if (umlClass.getQualifiedName().endsWith("." + addedAttribute.getType().getClassType())) {
                return addedAttribute;
            }
        }
        return null;
    }

    private void detectSubRefactorings(UMLClassBaseDiff classDiff,
                                       UMLClass addedClass,
                                       RefactoringType parentType) throws RefactoringMinerTimedOutException {
        for (UMLOperation addedOperation : addedClass.getOperations()) {
            UMLOperation removedOperation = classDiff.containsRemovedOperationWithTheSameSignature(addedOperation);
            if (removedOperation != null) {
                classDiff.getRemovedOperations().remove(removedOperation);
                Refactoring ref = null;
                if (parentType.equals(RefactoringType.EXTRACT_SUPERCLASS)) {
                    ref = new PullUpOperationRefactoring(removedOperation, addedOperation);
                } else if (parentType.equals(RefactoringType.EXTRACT_CLASS)) {
                    ref = new MoveOperationRefactoring(removedOperation, addedOperation);
                } else if (parentType.equals(RefactoringType.EXTRACT_SUBCLASS)) {
                    ref = new PushDownOperationRefactoring(removedOperation, addedOperation);
                }
                this.refactorings.add(ref);
                UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(removedOperation, addedOperation, classDiff);
                UMLOperationDiff operationSignatureDiff =
                    new UMLOperationDiff(removedOperation, addedOperation, mapper.getMappings());
                refactorings.addAll(operationSignatureDiff.getRefactorings());
                checkForExtractedOperationsWithinMovedMethod(mapper, addedClass);
            }
        }

        for (UMLAttribute addedAttribute : addedClass.getAttributes()) {
            UMLAttribute removedAttribute = classDiff.containsRemovedAttributeWithTheSameSignature(addedAttribute);
            if (removedAttribute != null) {
                classDiff.getRemovedAttributes().remove(removedAttribute);
                Refactoring ref = null;
                if (parentType.equals(RefactoringType.EXTRACT_SUPERCLASS)) {
                    ref = new PullUpAttributeRefactoring(removedAttribute, addedAttribute);
                } else if (parentType.equals(RefactoringType.EXTRACT_CLASS)) {
                    ref = new MoveAttributeRefactoring(removedAttribute, addedAttribute);
                } else if (parentType.equals(RefactoringType.EXTRACT_SUBCLASS)) {
                    ref = new PushDownAttributeRefactoring(removedAttribute, addedAttribute);
                }
                this.refactorings.add(ref);
            }
        }
    }

    public void reportAddedGeneralization(UMLGeneralization umlGeneralization) {
        this.addedGeneralizations.add(umlGeneralization);
    }

    public void reportRemovedGeneralization(UMLGeneralization umlGeneralization) {
        this.removedGeneralizations.add(umlGeneralization);
    }

    public void reportAddedRealization(UMLRealization umlRealization) {
        this.addedRealizations.add(umlRealization);
    }

    public void reportRemovedRealization(UMLRealization umlRealization) {
        this.removedRealizations.add(umlRealization);
    }

    private List<ExtractSuperClassRefactoring> identifyExtractSuperclassRefactorings() throws
        RefactoringMinerTimedOutException {
        List<ExtractSuperClassRefactoring> refactorings = new ArrayList<>();
        for (UMLClass addedClass : addedClasses) {
            Set<UMLClass> subclassSet = new LinkedHashSet<>();
            String addedClassName = addedClass.getQualifiedName();
            for (UMLGeneralization addedGeneralization : addedGeneralizations) {
                processAddedGeneralization(addedClass, subclassSet, addedGeneralization);
            }
            for (UMLGeneralizationDiff generalizationDiff : generalizationDiffList) {
                UMLGeneralization addedGeneralization = generalizationDiff.getAddedGeneralization();
                UMLGeneralization removedGeneralization = generalizationDiff.getRemovedGeneralization();
                if (!addedGeneralization.getParent().equals(removedGeneralization.getParent())) {
                    processAddedGeneralization(addedClass, subclassSet, addedGeneralization);
                }
            }
            for (UMLRealization addedRealization : addedRealizations) {
                String supplier = addedRealization.getSupplier();
                if (looksLikeSameType(supplier, addedClassName) && topLevelOrSameOuterClass(addedClass,
                                                                                            addedRealization.getClient()) && getAddedClass(
                    addedRealization.getClient().getQualifiedName()) == null) {
                    UMLClassBaseDiff clientClassDiff = getUMLClassDiff(addedRealization.getClient().getQualifiedName());
                    int implementedInterfaceOperations = 0;
                    boolean clientImplementsSupplier = false;
                    if (clientClassDiff != null) {
                        for (UMLOperation interfaceOperation : addedClass.getOperations()) {
                            if (clientClassDiff.containsOperationWithTheSameSignatureInOriginalClass(
                                interfaceOperation)) {
                                implementedInterfaceOperations++;
                            }
                        }
                        clientImplementsSupplier =
                            clientClassDiff.getOriginalClass().getImplementedInterfaces().contains(
                                UMLType.extractTypeObject(supplier));
                    }
                    if ((implementedInterfaceOperations > 0 || addedClass.getOperations().size() == 0) && !clientImplementsSupplier)
                        subclassSet.add(addedRealization.getClient());
                }
            }
            if (subclassSet.size() > 0) {
                ExtractSuperClassRefactoring extractSuperclassRefactoring =
                    new ExtractSuperClassRefactoring(addedClass, subclassSet);
                refactorings.add(extractSuperclassRefactoring);
            }
        }
        return refactorings;
    }

    private void processAddedGeneralization(UMLClass addedClass,
                                            Set<UMLClass> subclassSet,
                                            UMLGeneralization addedGeneralization) throws
        RefactoringMinerTimedOutException {
        String parent = addedGeneralization.getParent();
        UMLClass subclass = addedGeneralization.getChild();
        if (looksLikeSameType(parent, addedClass.getQualifiedName()) && topLevelOrSameOuterClass(addedClass,
                                                                                                 subclass) && getAddedClass(
            subclass.getQualifiedName()) == null) {
            UMLClassBaseDiff subclassDiff = getUMLClassDiff(subclass.getQualifiedName());
            if (subclassDiff != null) {
                detectSubRefactorings(subclassDiff, addedClass, RefactoringType.EXTRACT_SUPERCLASS);
            }
            subclassSet.add(subclass);
        }
    }

    private boolean topLevelOrSameOuterClass(UMLClass class1, UMLClass class2) {
        if (!class1.isTopLevel() && !class2.isTopLevel()) {
            return class1.getPackageName().equals(class2.getPackageName());
        }
        return true;
    }

    private boolean anotherAddedMethodExistsWithBetterMatchingInvocationExpression(OperationInvocation invocation,
                                                                                   UMLOperation addedOperation,
                                                                                   List<UMLOperation> addedOperations) {
        String expression = invocation.getExpression();
        if (expression != null) {
            int originalDistance = StringDistance.editDistance(expression, addedOperation.getNonQualifiedClassName());
            for (UMLOperation operation : addedOperations) {
                UMLClassBaseDiff classDiff = getUMLClassDiff(operation.getClassName());
                boolean isInterface = classDiff != null && classDiff.nextClass.isInterface();
                if (!operation.equals(addedOperation) && addedOperation.equalSignature(
                    operation) && !operation.isAbstract() && !isInterface) {
                    int newDistance = StringDistance.editDistance(expression, operation.getNonQualifiedClassName());
                    if (newDistance < originalDistance) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean sourceClassImportsSuperclassOfTargetClass(String sourceClassName, String targetClassName) {
        UMLClassBaseDiff targetClassDiff = getUMLClassDiff(targetClassName);
        if (targetClassDiff != null && targetClassDiff.getSuperclass() != null) {
            UMLClassBaseDiff superclassOfTargetClassDiff = getUMLClassDiff(targetClassDiff.getSuperclass());
            if (superclassOfTargetClassDiff != null) {
                return sourceClassImportsTargetClass(sourceClassName, superclassOfTargetClassDiff.getNextClassName());
            }
        }
        return false;
    }

    private boolean sourceClassImportsTargetClass(String sourceClassName, String targetClassName) {
        UMLClassBaseDiff classDiff = getUMLClassDiff(sourceClassName);
        if (classDiff == null) {
            classDiff = getUMLClassDiff(UMLType.extractTypeObject(sourceClassName));
        }
        if (classDiff != null) {
            return classDiff.nextClassImportsType(targetClassName) || classDiff.originalClassImportsType(
                targetClassName);
        }
        UMLClass removedClass = getRemovedClass(sourceClassName);
        if (removedClass == null) {
            removedClass = looksLikeRemovedClass(UMLType.extractTypeObject(sourceClassName));
        }
        if (removedClass != null) {
            return removedClass.importsType(targetClassName);
        }
        return false;
    }

    private boolean targetClassImportsSourceClass(String sourceClassName, String targetClassName) {
        UMLClassBaseDiff classDiff = getUMLClassDiff(targetClassName);
        if (classDiff == null) {
            classDiff = getUMLClassDiff(UMLType.extractTypeObject(targetClassName));
        }
        if (classDiff != null) {
            return classDiff.originalClassImportsType(sourceClassName) || classDiff.nextClassImportsType(
                sourceClassName);
        }
        UMLClass addedClass = getAddedClass(targetClassName);
        if (addedClass == null) {
            addedClass = looksLikeAddedClass(UMLType.extractTypeObject(targetClassName));
        }
        if (addedClass != null) {
            return addedClass.importsType(sourceClassName);
        }
        return false;
    }

    private boolean conflictingExpression(OperationInvocation invocation,
                                          UMLOperation addedOperation,
                                          Map<String, UMLType> variableTypeMap) {
        String expression = invocation.getExpression();
        if (expression != null && variableTypeMap.containsKey(expression)) {
            UMLType type = variableTypeMap.get(expression);
            UMLClassBaseDiff classDiff = getUMLClassDiff(addedOperation.getClassName());
            boolean superclassRelationship = false;
            if (classDiff != null && classDiff.getNewSuperclass() != null &&
                classDiff.getNewSuperclass().equals(type)) {
                superclassRelationship = true;
            }
            return !addedOperation.getNonQualifiedClassName().equals(type.getClassType()) && !superclassRelationship;
        }
        return false;
    }

    private boolean extractAndMoveMatchCondition(UMLOperationBodyMapper operationBodyMapper,
                                                 UMLOperationBodyMapper parentMapper) {
        List<AbstractCodeMapping> mappingList = new ArrayList<>(operationBodyMapper.getMappings());
        if (operationBodyMapper.getOperation2().isGetter() && mappingList.size() == 1) {
            List<AbstractCodeMapping> parentMappingList =
                new ArrayList<>(parentMapper.getMappings());
            for (AbstractCodeMapping mapping : parentMappingList) {
                if (mapping.getFragment1().equals(mappingList.get(0).getFragment1())) {
                    return false;
                }
                if (mapping instanceof CompositeStatementObjectMapping) {
                    CompositeStatementObjectMapping compositeMapping = (CompositeStatementObjectMapping) mapping;
                    CompositeStatementObject fragment1 = (CompositeStatementObject) compositeMapping.getFragment1();
                    for (AbstractExpression expression : fragment1.getExpressions()) {
                        if (expression.equals(mappingList.get(0).getFragment1())) {
                            return false;
                        }
                    }
                }
            }
        }
        int mappings = operationBodyMapper.mappingsWithoutBlocks();
        int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
        int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
        List<AbstractCodeMapping> exactMatchList = operationBodyMapper.getExactMatches();
        int exactMatches = exactMatchList.size();
        return mappings > 0 && (mappings > nonMappedElementsT2 || (mappings > 1 && mappings >= nonMappedElementsT2) ||
            (exactMatches == mappings && nonMappedElementsT1 == 0) ||
            (exactMatches == 1 && !exactMatchList.get(
                0).getFragment1().throwsNewException() && nonMappedElementsT2 - exactMatches <= 10) ||
            (exactMatches > 1 && nonMappedElementsT2 - exactMatches < 20) ||
            (mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2()));
    }

    private boolean movedMethodSignature(UMLOperation removedOperation, UMLOperation addedOperation) {
        if (addedOperation.getName().equals(removedOperation.getName()) &&
            addedOperation.equalReturnParameter(removedOperation) &&
            addedOperation.isAbstract() == removedOperation.isAbstract() &&
            addedOperation.getTypeParameters().equals(removedOperation.getTypeParameters())) {
            if (addedOperation.getParameters().equals(removedOperation.getParameters())) {
                return true;
            } else {
                // ignore parameters of types sourceClass and targetClass
                List<UMLParameter> oldParameters = new ArrayList<>();
                Set<String> oldParameterNames = new LinkedHashSet<>();
                for (UMLParameter oldParameter : removedOperation.getParameters()) {
                    if (!oldParameter.getKind().equals("return")
                        && !looksLikeSameType(oldParameter.getType().getClassType(), addedOperation.getClassName())
                        && !looksLikeSameType(oldParameter.getType().getClassType(), removedOperation.getClassName())) {
                        oldParameters.add(oldParameter);
                        oldParameterNames.add(oldParameter.getName());
                    }
                }
                List<UMLParameter> newParameters = new ArrayList<>();
                Set<String> newParameterNames = new LinkedHashSet<>();
                for (UMLParameter newParameter : addedOperation.getParameters()) {
                    if (!newParameter.getKind().equals("return") &&
                        !looksLikeSameType(newParameter.getType().getClassType(), addedOperation.getClassName()) &&
                        !looksLikeSameType(newParameter.getType().getClassType(), removedOperation.getClassName())) {
                        newParameters.add(newParameter);
                        newParameterNames.add(newParameter.getName());
                    }
                }
                Set<String> intersection = new LinkedHashSet<>(oldParameterNames);
                intersection.retainAll(newParameterNames);
                return oldParameters.equals(newParameters) || oldParameters.containsAll(
                    newParameters) || newParameters.containsAll(oldParameters) || intersection.size() > 0 ||
                    removedOperation.isStatic() || addedOperation.isStatic();
            }
        }
        return false;
    }

    private boolean allRenamedOperations(List<UMLOperationBodyMapper> mappers) {
        for (UMLOperationBodyMapper mapper : mappers) {
            if (mapper.getOperation1().getName().equals(mapper.getOperation2().getName())) {
                return false;
            }
        }
        return true;
    }

    private boolean refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(UMLOperation removedOperation) {
        for (Refactoring refactoring : refactorings) {
            if (refactoring instanceof MoveOperationRefactoring) {
                MoveOperationRefactoring moveRefactoring = (MoveOperationRefactoring) refactoring;
                if (moveRefactoring.getOriginalOperation().equals(removedOperation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean sameSourceAndTargetClass(List<UMLOperationBodyMapper> mappers) {
        if (mappers.size() == 1) {
            return false;
        }
        String sourceClassName = null;
        String targetClassName = null;
        for (UMLOperationBodyMapper mapper : mappers) {
            String mapperSourceClassName = mapper.getOperation1().getClassName();
            if (sourceClassName == null) {
                sourceClassName = mapperSourceClassName;
            } else if (!mapperSourceClassName.equals(sourceClassName)) {
                return false;
            }
            String mapperTargetClassName = mapper.getOperation2().getClassName();
            if (targetClassName == null) {
                targetClassName = mapperTargetClassName;
            } else if (!mapperTargetClassName.equals(targetClassName)) {
                return false;
            }
        }
        return true;
    }

    private List<UMLOperationBodyMapper> firstMappers(TreeMap<Integer, List<UMLOperationBodyMapper>> operationBodyMapperMap) {
        List<UMLOperationBodyMapper> firstMappers =
            new ArrayList<>(operationBodyMapperMap.get(operationBodyMapperMap.lastKey()));
        List<UMLOperationBodyMapper> extraMappers = operationBodyMapperMap.get(0);
        if (extraMappers != null && operationBodyMapperMap.lastKey() != 0) {
            for (UMLOperationBodyMapper extraMapper : extraMappers) {
                UMLOperation operation1 = extraMapper.getOperation1();
                UMLOperation operation2 = extraMapper.getOperation2();
                if (operation1.equalSignature(operation2)) {
                    List<AbstractCodeMapping> mappings = new ArrayList<>(extraMapper.getMappings());
                    if (mappings.size() == 1) {
                        Set<Replacement> replacements = mappings.get(0).getReplacements();
                        if (replacements.size() == 1) {
                            Replacement replacement = replacements.iterator().next();
                            List<String> parameterNames1 = operation1.getParameterNameList();
                            List<String> parameterNames2 = operation2.getParameterNameList();
                            for (int i = 0; i < parameterNames1.size(); i++) {
                                String parameterName1 = parameterNames1.get(i);
                                String parameterName2 = parameterNames2.get(i);
                                if (replacement.getBefore().equals(parameterName1) &&
                                    replacement.getAfter().equals(parameterName2)) {
                                    firstMappers.add(extraMapper);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return firstMappers;
    }

    private boolean mappedElementsMoreThanNonMappedT1AndT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
        int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
        int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
        UMLClass addedClass = getAddedClass(operationBodyMapper.getOperation2().getClassName());
        int nonMappedStatementsDeclaringSameVariable = 0;
        for (ListIterator<StatementObject> leafIterator1 =
             operationBodyMapper.getNonMappedLeavesT1().listIterator(); leafIterator1.hasNext(); ) {
            StatementObject s1 = leafIterator1.next();
            for (StatementObject s2 : operationBodyMapper.getNonMappedLeavesT2()) {
                if (s1.getVariableDeclarations().size() == 1 && s2.getVariableDeclarations().size() == 1) {
                    VariableDeclaration v1 = s1.getVariableDeclarations().get(0);
                    VariableDeclaration v2 = s2.getVariableDeclarations().get(0);
                    if (v1.getVariableName().equals(v2.getVariableName()) && v1.getType().equals(v2.getType())) {
                        nonMappedStatementsDeclaringSameVariable++;
                    }
                }
            }
            if (addedClass != null && s1.getVariableDeclarations().size() == 1) {
                VariableDeclaration v1 = s1.getVariableDeclarations().get(0);
                for (UMLAttribute attribute : addedClass.getAttributes()) {
                    VariableDeclaration attributeDeclaration = attribute.getVariableDeclaration();
                    if (attributeDeclaration.getInitializer() != null && v1.getInitializer() != null) {
                        String attributeInitializer = attributeDeclaration.getInitializer().getString();
                        String variableInitializer = v1.getInitializer().getString();
                        if (attributeInitializer.equals(variableInitializer) && attribute.getType().equals(
                            v1.getType()) &&
                            (attribute.getName().equals(v1.getVariableName()) ||
                                attribute.getName().toLowerCase().contains(v1.getVariableName().toLowerCase()) ||
                                v1.getVariableName().toLowerCase().contains(attribute.getName().toLowerCase()))) {
                            nonMappedStatementsDeclaringSameVariable++;
                            leafIterator1.remove();
                            LeafMapping mapping =
                                new LeafMapping(v1.getInitializer(), attributeDeclaration.getInitializer(),
                                                operationBodyMapper.getOperation1(),
                                                operationBodyMapper.getOperation2());
                            operationBodyMapper.getMappings().add(mapping);
                            break;
                        }
                    }
                }
            }
        }
        int nonMappedLoopsIteratingOverSameVariable = 0;
        for (CompositeStatementObject c1 : operationBodyMapper.getNonMappedInnerNodesT1()) {
            if (c1.isLoop()) {
                for (CompositeStatementObject c2 : operationBodyMapper.getNonMappedInnerNodesT2()) {
                    if (c2.isLoop()) {
                        Set<String> intersection = new LinkedHashSet<>(c1.getVariables());
                        intersection.retainAll(c2.getVariables());
                        if (!intersection.isEmpty()) {
                            nonMappedLoopsIteratingOverSameVariable++;
                        }
                    }
                }
            }
        }
        return (mappings > nonMappedElementsT1 - nonMappedStatementsDeclaringSameVariable - nonMappedLoopsIteratingOverSameVariable &&
            mappings > nonMappedElementsT2 - nonMappedStatementsDeclaringSameVariable - nonMappedLoopsIteratingOverSameVariable) ||
            (nonMappedElementsT1 - nonMappedStatementsDeclaringSameVariable - nonMappedLoopsIteratingOverSameVariable == 0 && mappings > Math.floor(
                nonMappedElementsT2 / 2.0)) ||
            (nonMappedElementsT2 - nonMappedStatementsDeclaringSameVariable - nonMappedLoopsIteratingOverSameVariable == 0 && mappings > Math.floor(
                nonMappedElementsT1 / 2.0));
    }

    private boolean conflictingMoveOfTopLevelClass(UMLClass removedClass, UMLClass addedClass) {
        if (!removedClass.isTopLevel() && !addedClass.isTopLevel()) {
            //check if classMoveDiffList contains already a move for the outer class to a different target
            for (UMLClassMoveDiff diff : classMoveDiffList) {
                if ((diff.getOriginalClass().getQualifiedName().startsWith(removedClass.getPackageName()) &&
                    !diff.getMovedClass().getQualifiedName().startsWith(addedClass.getPackageName())) ||
                    (!diff.getOriginalClass().getQualifiedName().startsWith(removedClass.getPackageName()) &&
                        diff.getMovedClass().getQualifiedName().startsWith(addedClass.getPackageName()))) {
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

    private void deleteRemovedOperation(UMLOperation operation) {
        UMLClassBaseDiff classDiff = getUMLClassDiff(operation.getClassName());
        if (classDiff != null)
            classDiff.getRemovedOperations().remove(operation);
    }

    private void deleteAddedOperation(UMLOperation operation) {
        UMLClassBaseDiff classDiff = getUMLClassDiff(operation.getClassName());
        if (classDiff != null)
            classDiff.getAddedOperations().remove(operation);
    }

    private boolean innerClassWithTheSameName(UMLClass removedClass, UMLClass addedClass) {
        if (!removedClass.isTopLevel() && !addedClass.isTopLevel()) {
            String removedClassName = removedClass.getQualifiedName();
            String removedName = removedClassName.substring(removedClassName.lastIndexOf(".") + 1);
            String addedClassName = addedClass.getQualifiedName();
            String addedName = addedClassName.substring(addedClassName.lastIndexOf(".") + 1);
            return removedName.equals(addedName);
        }
        return false;
    }

    public void addUMLClassDiff(UMLClassDiff classDiff) {
        this.commonClassDiffList.add(classDiff);
    }

    public void addUmlFileDiff(UMLFileDiff fileDiff) {
        this.umlFileDiff.add(fileDiff);
    }

    private static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}
