package org.jetbrains.research.kotlinrminer.uml;

import org.jetbrains.research.kotlinrminer.diff.UMLModelDiff;

import java.util.*;

public class UMLModel {
    private Set<String> repositoryDirectories;
    private List<UMLClass> classList;
    private List<UMLGeneralization> generalizationList;
    private List<UMLRealization> realizationList;

    public UMLModel(Set<String> repositoryDirectories) {
        this.repositoryDirectories = repositoryDirectories;
        classList = new ArrayList<>();
        generalizationList = new ArrayList<>();
        realizationList = new ArrayList<>();
    }

    public void addClass(UMLClass umlClass) {
        classList.add(umlClass);
    }

    public void addGeneralization(UMLGeneralization umlGeneralization) {
        generalizationList.add(umlGeneralization);
    }

    public void addRealization(UMLRealization umlRealization) {
        realizationList.add(umlRealization);
    }

    public UMLClass getClass(UMLClass umlClassFromOtherModel) {
        for (UMLClass umlClass : classList) {
            if (umlClass.equals(umlClassFromOtherModel))
                return umlClass;
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
                String thisParentComparedString = null;
                if (thisParent.contains("."))
                    thisParentComparedString = thisParent.substring(thisParent.lastIndexOf(".") + 1);
                else
                    thisParentComparedString = thisParent;
                String otherParentComparedString = null;
                if (otherParent.contains("."))
                    otherParentComparedString = otherParent.substring(otherParent.lastIndexOf(".") + 1);
                else
                    otherParentComparedString = otherParent;
                if (thisParentComparedString.equals(otherParentComparedString))
                    return generalization;
            }
        }
        return null;
    }

    public UMLModelDiff diff(UMLModel umlModel) {
        return this.diff(umlModel, Collections.emptyMap());
    }

    public UMLModelDiff diff(UMLModel umlModel, Map<String, String> renamedFileHints) {
        UMLModelDiff modelDiff = new UMLModelDiff();
        for (UMLClass umlClass : classList) {
            if (!umlModel.classList.contains(umlClass))
                modelDiff.reportRemovedClass(umlClass);
        }
        for (UMLClass umlClass : umlModel.classList) {
            if (!this.classList.contains(umlClass))
                modelDiff.reportAddedClass(umlClass);
        }

        modelDiff.checkForMovedClasses(renamedFileHints, umlModel.repositoryDirectories, new UMLClassMatcher.Move());
        modelDiff.checkForRenamedClasses(renamedFileHints, new UMLClassMatcher.Rename());
        modelDiff.checkForRenamedClasses(renamedFileHints, new UMLClassMatcher.RelaxedRename());

        return modelDiff;
    }
}
