package org.jetbrains.research.kotlinrminer.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.uml.UMLClass;
import org.jetbrains.research.kotlinrminer.uml.UMLClassMatcher;
import org.jetbrains.research.kotlinrminer.uml.UMLType;

public class UMLModelDiff {
  private List<UMLClassMoveDiff> classMoveDiffList;
  private List<UMLClass> addedClasses;
  private List<UMLClass> removedClasses;
  private Set<String> deletedFolderPaths;
  private List<UMLClassMoveDiff> innerClassMoveDiffList;
  private List<UMLClassRenameDiff> classRenameDiffList;

  public UMLModelDiff() {
    this.addedClasses = new ArrayList<>();
    this.removedClasses = new ArrayList<>();
    this.classMoveDiffList = new ArrayList<>();
    this.deletedFolderPaths = new LinkedHashSet<>();
    this.innerClassMoveDiffList = new ArrayList<>();
    this.classRenameDiffList = new ArrayList<>();
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

  public List<Refactoring> getRefactorings() {
    Set<Refactoring> refactorings = new LinkedHashSet<>();
    refactorings.addAll(getMoveClassRefactorings());
    refactorings.addAll(getRenameClassRefactorings());
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
                                   Set<String> repositoryDirectories, UMLClassMatcher matcher) {
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
          String subDirectory = new String(removedClassSourceFolder);
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
        //TODO: minClassMoveDiff.process();
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
                                     UMLClassMatcher matcher) {
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
        //TODO: minClassRenameDiff.process();
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

}
