package org.jetbrains.research.kotlinrminer.core.uml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.research.kotlinrminer.core.RefactoringMinerTimedOutException;
import org.jetbrains.research.kotlinrminer.core.diff.UMLClassDiff;
import org.jetbrains.research.kotlinrminer.core.diff.UMLFileDiff;
import org.jetbrains.research.kotlinrminer.core.diff.UMLModelDiff;

public class UMLModel {
    private final Set<String> repositoryDirectories;
    private final List<UMLClass> classList;
    private final List<UMLObject> objectList;
    private final List<UMLGeneralization> generalizationList;
    private final List<UMLRealization> realizationList;
    private final List<UMLFile> fileList;

    public UMLModel(Set<String> repositoryDirectories) {
        this.repositoryDirectories = repositoryDirectories;
        classList = new ArrayList<>();
        objectList = new ArrayList<>();
        generalizationList = new ArrayList<>();
        realizationList = new ArrayList<>();
        fileList = new ArrayList<>();
    }

    public void addClass(UMLClass umlClass) {
        classList.add(umlClass);
    }

    public void addObject(UMLObject umlObject) {
        objectList.add(umlObject);
    }

    public void addGeneralization(UMLGeneralization umlGeneralization) {
        generalizationList.add(umlGeneralization);
    }

    public void addRealization(UMLRealization umlRealization) {
        realizationList.add(umlRealization);
    }

    public void addFile(UMLFile umlFile) {
        fileList.add(umlFile);
    }

    public UMLClass getClass(UMLClass umlClassFromOtherModel) {
        for (UMLClass umlClass : classList) {
            if (umlClass.equals(umlClassFromOtherModel)) {
                return umlClass;
            }
        }
        return null;
    }

    public UMLFile getFile(UMLFile umlFileFromOtherModel) {
        for (UMLFile umlFile : fileList) {
            if (umlFile.equals(umlFileFromOtherModel)) {
                return umlFile;
            }
        }
        return null;
    }

    public List<UMLClass> getClassList() {
        return this.classList;
    }

    public List<UMLGeneralization> getGeneralizationList() {
        return this.generalizationList;
    }

    public UMLGeneralization matchGeneralization(UMLGeneralization otherGeneralization) {
        for (UMLGeneralization generalization : generalizationList) {
            if (generalization.getChild().equals(otherGeneralization.getChild())) {
                String thisParent = generalization.getParent();
                String otherParent = otherGeneralization.getParent();
                String thisParentComparedString;
                if (thisParent.contains(".")) {
                    thisParentComparedString = thisParent.substring(thisParent.lastIndexOf(".") + 1);
                } else {
                    thisParentComparedString = thisParent;
                }
                String otherParentComparedString;
                if (otherParent.contains(".")) {
                    otherParentComparedString = otherParent.substring(otherParent.lastIndexOf(".") + 1);
                } else {
                    otherParentComparedString = otherParent;
                }
                if (thisParentComparedString.equals(otherParentComparedString)) {
                    return generalization;
                }
            }
        }
        return null;
    }

    public UMLModelDiff diff(UMLModel umlModel) throws RefactoringMinerTimedOutException {
        return this.diff(umlModel, Collections.emptyMap());
    }

    public UMLModelDiff diff(UMLModel umlModel, Map<String, String> renamedFileHints) throws
        RefactoringMinerTimedOutException {
        UMLModelDiff modelDiff = new UMLModelDiff();
        for (UMLClass umlClass : classList) {
            if (!umlModel.classList.contains(umlClass)) {
                modelDiff.reportRemovedClass(umlClass);
            }
        }
        for (UMLClass umlClass : umlModel.classList) {
            if (!this.classList.contains(umlClass)) {
                modelDiff.reportAddedClass(umlClass);
            }
        }

        for (UMLClass umlClass : classList) {
            if (umlModel.classList.contains(umlClass)) {
                UMLClassDiff classDiff = new UMLClassDiff(umlClass, umlModel.getClass(umlClass), modelDiff);
                classDiff.process();
                if (!classDiff.isEmpty()) {
                    modelDiff.addUMLClassDiff(classDiff);
                }
            }
        }

        for (UMLGeneralization umlGeneralization : generalizationList) {
            if (!umlModel.generalizationList.contains(umlGeneralization))
                modelDiff.reportRemovedGeneralization(umlGeneralization);
        }
        for (UMLGeneralization umlGeneralization : umlModel.generalizationList) {
            if (!this.generalizationList.contains(umlGeneralization))
                modelDiff.reportAddedGeneralization(umlGeneralization);
        }

        modelDiff.checkForGeneralizationChanges();
        for (UMLRealization umlRealization : realizationList) {
            if (!umlModel.realizationList.contains(umlRealization))
                modelDiff.reportRemovedRealization(umlRealization);
        }

        for (UMLFile umlFile : fileList) {
            if (umlModel.fileList.contains(umlFile)) {
                UMLFileDiff fileDiff = new UMLFileDiff(umlFile, umlModel.getFile(umlFile), modelDiff);
                fileDiff.process();
                if (!fileDiff.isEmpty()) {
                    modelDiff.addUmlFileDiff(fileDiff);
                }
            }
        }

        modelDiff.checkForMovedClasses(renamedFileHints, umlModel.repositoryDirectories, new UMLClassMatcher.Move());
        modelDiff.checkForRenamedClasses(renamedFileHints, new UMLClassMatcher.Rename());
        modelDiff.checkForRenamedClasses(renamedFileHints, new UMLClassMatcher.RelaxedRename());

        return modelDiff;
    }

}
