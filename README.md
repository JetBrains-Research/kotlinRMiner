# kotlinRMiner
[![JB Research](https://jb.gg/badges/research-flat-square.svg)](https://research.jetbrains.org/)

A library that detects performed refactorings in changes in Kotlin code.

## About
The library is based on [RefactoringMiner](https://github.com/tsantalis/RefactoringMiner), a tool that detects performed refactorings in code changes written in Java.
The tool analyzes commit history of a Git project, parses code changes in each commit, uses detection rules and heuristics to detect performed refactorings, and returns a list of detected refactorings in each  commit with a description. 

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
19. Change Attribute Type
20. Change Variable Type
21. Change Parameter Type
22. Move Attribute
23. Pull Up Attribute
24. Push Down Attribute
25. Rename Attribute
26. Rename Variable

## Usage
Use a library as CLI:
1. Clone the repository 

    ```git clone https://github.com/JetBrains-Research/kotlinRMiner.git```
 
2. Run ```./gradlew jar``` in the project directory
 
3. Now you can use a library as CLI
 
    ```cd build/libs```
 
    ```java -jar kotlinRMiner-1.0.jar <option>```
 
The list of supported options:
 
```-h Usage: kotlinRMiner <args>
    -c   <git-repo-folder> <commit-sha1>                            Detect refactorings at the specific commit <commit-sha1> for project <git-repo-folder>.
    -bc  <git-repo-folder> <start-commit-sha1> <end-commit-sha1>    Detect refactorings between <start-commit-sha1> and <end-commit-sha1> for a project <git-repo-folder>.    
    -all <git-repo-folder> <branch>                                 Detect all refactorings at the <branch> for <git-repo-folder>. If <branch> is not specified, commits from master branch are analyzed.
```
## Contacts
If you have any questions or suggestions, don't hesitate to open an issue.
If you want to contribute, please create pull requests.
