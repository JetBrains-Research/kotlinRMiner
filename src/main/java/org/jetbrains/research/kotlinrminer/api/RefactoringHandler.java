package org.jetbrains.research.kotlinrminer.api;

import java.util.List;

public abstract class RefactoringHandler {
    public void handle(String commitId, List<Refactoring> refactorings) {
    }
}
