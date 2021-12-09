package org.jetbrains.research.kotlinrminer.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangesUtil
import org.jetbrains.research.kotlinrminer.common.RefactoringType
import org.jetbrains.research.kotlinrminer.ide.uml.UMLModel

object KotlinRMiner {

    private val refactoringTypesToConsider = RefactoringType.ALL

    fun detectRefactorings(project: Project, changes: List<Change>): List<Refactoring> {
        val filePathsBefore = arrayListOf<String>()
        val filePathsCurrent = arrayListOf<String>()
        val renamedFilesHint = hashMapOf<String, String>()

        preprocessChanges(changes, filePathsBefore, filePathsCurrent, renamedFilesHint)

        val repositoryDirectoriesBefore = linkedSetOf<String>()
        val repositoryDirectoriesCurrent = linkedSetOf<String>()
        val fileContentsBefore = linkedMapOf<String, String>()
        val fileContentsCurrent = linkedMapOf<String, String>()

        // If no kt files changed, there is no refactoring. Also, if there are
        // only ADD's or only REMOVE's there is no refactoring
        if (filePathsBefore.isNotEmpty() && filePathsCurrent.isNotEmpty()) {
            populateFileContents(
                RevisionType.BEFORE, changes, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore
            )
            val parentUMLModel = createModelInKotlin(project, fileContentsBefore, repositoryDirectoriesBefore)
            populateFileContents(
                RevisionType.AFTER, changes, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent
            )
            val currentUMLModel = createModelInKotlin(project, fileContentsCurrent, repositoryDirectoriesCurrent)
            return parentUMLModel.diff(currentUMLModel, renamedFilesHint).refactorings.filter(::filter)
        }

        return emptyList()
    }

    private fun preprocessChanges(
        changes: List<Change>,
        filesBefore: MutableList<String>,
        filesCurrent: MutableList<String>,
        renamedFilesHint: MutableMap<String, String>
    ) {
        for (change in changes) {
            val changeType = change.type
            val oldPath = change.beforeRevision?.file?.path.orEmpty()
            val newPath = change.afterRevision?.file?.path.orEmpty()
            if (changeType != Change.Type.NEW) {
                if (isKotlinFile(oldPath)) {
                    filesBefore.add(oldPath)
                }
            }
            if (changeType != Change.Type.DELETED) {
                if (isKotlinFile(newPath)) {
                    filesCurrent.add(newPath)
                }
            }
            if (change.isRenamed) {
                if (isKotlinFile(oldPath) && isKotlinFile(newPath)) {
                    renamedFilesHint[oldPath] = newPath
                }
            }
        }
    }

    private fun populateFileContents(
        type: RevisionType,
        changes: List<Change>,
        filePaths: List<String>,
        fileContents: MutableMap<String, String>,
        repositoryDirectories: MutableSet<String>
    ) {
        for (change in changes) {
            val revision =
                (if (type == RevisionType.BEFORE) change.beforeRevision else change.afterRevision) ?: continue
            val pathString = ChangesUtil.getFilePath(change).path
            if (filePaths.contains(pathString)) {
                fileContents[pathString] = ChangesUtil.loadContentRevision(revision).toString(Charsets.UTF_8)
            }
            if (pathString.endsWith(".kt") && pathString.contains("/")) {
                val directory = pathString.substring(0, pathString.lastIndexOf("/"))
                repositoryDirectories.add(directory)
                //include sub-directories
                var subDirectory = directory
                while (subDirectory.contains("/")) {
                    subDirectory = subDirectory.substring(0, subDirectory.lastIndexOf("/"))
                    repositoryDirectories.add(subDirectory)
                }
            }
        }
    }

    private fun createModelInKotlin(
        project: Project,
        fileContents: Map<String, String>,
        repositoryDirectories: Set<String>
    ): UMLModel {
        val psiReader = UMLModelPsiReader(repositoryDirectories)
        psiReader.parseFiles(project, fileContents)
        return psiReader.umlModel
    }

    private fun filter(refactoringsAtRevision: Refactoring): Boolean {
        //TODO: perform filtration
        return refactoringsAtRevision.refactoringType in refactoringTypesToConsider
    }

    private fun isKotlinFile(path: String): Boolean {
        return path.endsWith(".kt")
    }

    enum class RevisionType {
        BEFORE, AFTER
    }
}
