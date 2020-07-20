package org.jetbrains.research.kotlinrminer.uml;

public interface UMLClassMatcher {
    boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile);

    class Move implements UMLClassMatcher {
        public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
            return removedClass.hasSameNameAndKind(addedClass)
                    && (removedClass.hasSameAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile));
        }
    }

/*    TODO: public static class RelaxedMove implements UMLClassMatcher {
        public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
            return removedClass.hasSameNameAndKind(addedClass)
                    && (removedClass.hasCommonAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile));
        }
    }*/

    class ExtremelyRelaxedMove implements UMLClassMatcher {
        public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
            return removedClass.hasSameNameAndKind(addedClass)
                    && (removedClass.hasAttributesAndOperationsWithCommonNames(addedClass) || addedClass.getSourceFile().equals(renamedFile));
        }
    }

    class Rename implements UMLClassMatcher {
        public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
            return removedClass.hasSameKind(addedClass)
                    && (removedClass.hasSameAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile));
        }
    }

/*    TODO: public static class RelaxedRename implements UMLClassMatcher {
        public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
            return removedClass.hasSameKind(addedClass)
                    && (removedClass.hasCommonAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile));
        }
    }*/

    class ExtremelyRelaxedRename implements UMLClassMatcher {
        public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
            return removedClass.hasSameKind(addedClass)
                    && (removedClass.hasAttributesAndOperationsWithCommonNames(addedClass) || addedClass.getSourceFile().equals(renamedFile));
        }
    }
}

