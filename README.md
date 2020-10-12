# kotlinRMiner
A library that detects performed refactorings in code changes written in Kotlin.
The library is based on [RefactoringMiner](https://github.com/tsantalis/RefactoringMiner), a tool that detects performed refactorings in code changes written in Java.

A tool analyzes commit history of a Git project, parses code changes in each commit, uses detection rules and heuristics to detect performed refactorings, and returns a list of detected refactorings in each  commit with a description. 

Currently, kotlinRMiner supports the detection of the following refactorings:
1. Move Class
2. Rename Class
3. Rename Method
4. Add Parameter
5. Remove Parameter
6. Reorder Parameter
7. Move Method
8. Move And Rename Class
9. Move And Rename Method
10. Extract Method
11. Inline Method
12. Pull Up Method
13. Push Down Method
14. Extract Superclass
15. Extract Class
16. Extract Interface
17. Extract And Move Method
18. Move And Inline Method
