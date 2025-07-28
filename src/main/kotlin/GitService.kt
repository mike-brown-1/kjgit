package org.mikeb.gstat

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.measureTime
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RemoteConfig
import org.eclipse.jgit.util.FS
//import org.mikeb.kgstat.GitRepoScanner.logger
import java.io.IOException
import kotlin.io.path.isDirectory

private const val PROCESS_TIMEOUT = 10L
private const val BRANCH_LINE_PREFIX = "## "
private const val BRANCH_SEPARATOR = "..."
private const val TIMING = false

fun File.isGitRepository(): Boolean {
    return RepositoryCache.FileKey.isGitRepository(this, FS.DETECTED)
}

fun getRepoData(startDir: File, config: Config): List<Repo> {
    var repoList: List<Repo> = listOf()
    val directories = findGitDirectories(startDir)
    if (config.verbose) {
        println("Found ${directories.size} repositories.")
    }
    if (directories.isNotEmpty()) {
        val duration: Duration = measureTime {
            repoList = lookForChanges(directories, config)
        }
        if (TIMING) {
            println("lookForChanges took: $duration")
        }
    }
    return repoList
}


/**
 * Finds git repositories.
 *
 * @param startDir starting point of the search
 * @return list of directories containing `.git` directory
 */
fun findGitDirectories(startDir: File): List<File> {
    require(startDir.exists() && startDir.isDirectory) { "Starting directory must be valid" }

    return Files.walk(startDir.toPath())
        .filter { it.isDirectory() }
        .map { it.toFile() }
        .filter { it.isGitRepository() }
        .toList()
}

//
//private fun searchDirectory(directory: File): List<File> {
//    val gitDirectories = mutableListOf<File>()
//    directory.listFiles { file -> file.isDirectory }?.forEach { dir ->
//        if (dir.name == ".git") {
//            val result = executeProcess(dir.parentFile, listOf("git", "rev-parse",
//                "--is-inside-work-tree"), PROCESS_TIMEOUT)
//            if (result.exitCode == 0) {
//                gitDirectories.add(dir.parentFile)
//            }
//        } else {
//            gitDirectories.addAll(searchDirectory(dir))
//        }
//    }
//    return gitDirectories
//}
//
/**
 * Given a list of git directories, collect information about changes in those repositories.
 * @param listOfGitRepos List of directories that are git repositories
 * @return List of Repo
 */
fun lookForChanges(listOfGitRepos: List<File>, config: Config): List<Repo> {
    val repoList = mutableListOf<Repo>()
    runBlocking {
        listOfGitRepos.forEach { repoDir ->
            val duration: Duration = measureTime {
                launch(Dispatchers.IO + CoroutineName(repoDir.name)) {
                    val repo = getRepo(repoDir, config)
                    if (repo != null) { // (!repo.invalidRepo) {
                        repoList.add(repo)
                    }
                }
            }
            if (TIMING) {
                println("getRepo for: $repoDir took: $duration")
            }
        }
    }
    return repoList
}

/**
 * If dir is a valid git repository, return a populated Repo object with its status.
 * Otherwise, return null
 * @param dir the directory to process
 * @param config a Config instance with options
 * @return A populated Repo or null if invalid directory
 */
fun getRepo(dir: File, config: Config): Repo? {
    val repoName = dir.parent
//    if (dir.absolutePath == config.directoryRoot) {
//        dir.name
//    } else {
//        dir.absolutePath.removePrefix(config.directoryRoot + File.separator)
//    }
    var repo: Repo? = Repo(repoName)
    if (config.verbose) {
        println("Searching: $repoName")
    }

    var repository: Repository? = null
    var git: Git? = null

    try {
        // Build the repository object. This finds the .git directory.
        repository = FileRepositoryBuilder()
            .setGitDir(File(repoName, ".git")) // Explicitly point to the .git directory
            .readEnvironment() // Read GIT_* environment variables
            .findGitDir() // Scan up the hierarchy to find the .git directory
            .build()

        // Create a Git object from the repository
        git = Git(repository)
        repo?.repository = repository

        // Get the status of the repository
        val status: Status = git.status().call()

        // Print the status details
//        println("  Current Branch: ${repository.branch}")
        repo?.branch = repository.branch
        repo?.isClean = status.isClean

        // Check if clean
        if (!status.isClean) {
            // Untracked files and folders
            repo?.untracked = status.untracked.isNotEmpty()
//            val untrackedFiles = status.untracked.size
//            val untrackedFolders = status.untrackedFolders.size
//            if (untrackedFiles > 0 || untrackedFolders > 0) {
//                println("  Untracked: $untrackedFiles files, $untrackedFolders folders")
//                status.untracked.forEach { println("    - Untracked: $it") }
//                status.untrackedFolders.forEach { println("    - Untracked Folder: $it") }
//            } else {
//                println("  Untracked: 0")
//            }

            // Unstaged changes (modified, missing/deleted)
            repo?.modified = status.modified.isNotEmpty()
            repo?.deleted = status.missing.isNotEmpty()
//            val modifiedFiles = status.modified.size
//            val missingFiles = status.missing.size
//            if (modifiedFiles > 0 || missingFiles > 0) {
//                println("  Unstaged Changes:")
//                println("    Modified: $modifiedFiles")
//                status.modified.forEach { println("      - Modified: $it") }
//                println("    Deleted (Missing): $missingFiles")
//                status.missing.forEach { println("      - Deleted: $it") }
//            } else {
//                println("  Unstaged Changes: None")
//            }

            // Staged changes (added, changed, removed)
            repo?.added = status.added.isNotEmpty()
            repo?.changed = status.changed.isNotEmpty()
            repo?.removed = status.removed.isNotEmpty()

//            val addedFiles = status.added.size
//            val changedFiles = status.changed.size // Modified files that have been staged
//            val removedFiles = status.removed.size // Deleted files that have been staged
//            if (addedFiles > 0 || changedFiles > 0 || removedFiles > 0) {
//                println("  Staged Changes:")
//                println("    Added: $addedFiles")
//                status.added.forEach { println("      - Added: $it") }
//                println("    Modified (Staged): $changedFiles")
//                status.changed.forEach { println("      - Modified (Staged): $it") }
//                println("    Deleted (Staged): $removedFiles")
//                status.removed.forEach { println("      - Deleted (Staged): $it") }
//            } else {
//                println("  Staged Changes: None")
//            }

            // Conflicting files (during a merge/rebase)
            repo?.conflicting = status.conflicting.isNotEmpty()
//            val conflictingFiles = status.conflicting.size
//            if (conflictingFiles > 0) {
//                println("  Conflicts: $conflictingFiles")
//                status.conflicting.forEach { println("    - Conflict: $it") }
//            } else {
//                println("  Conflicts: None")
//            }
        }

        val remotes = RemoteConfig.getAllRemoteConfigs(repository.config)
        repo?.hasRemote = remotes.isNotEmpty()
//        if (remotes.isNotEmpty()) {
//            println("  ${remotes.size} remote(s) found:")
//            remotes.forEach { remote ->
//                println("    Remote: ${remote.name}, URL: ${remote.urIs}")
//            }
//        } else {
//            println("  No remotes configured")
//        }

    } catch (e: IOException) {
//        logger.error("Error opening Git repository at ${repoDir.absolutePath}: ${e.message}")
        println("  Error: Could not open repository. ${e.message}")
        repo = null
    } catch (e: Exception) {
//        logger.error("Error processing Git repository at ${repoDir.absolutePath}: ${e.message}", e)
        println("  Error: An unexpected error occurred. ${e.message}")
        repo = null
    } finally {
        // Ensure resources are closed
        git?.close()
        repository?.close()
    }

//    if (config.checkRemote) {
//        val result = executeProcess(dir, listOf("git", "fetch"), PROCESS_TIMEOUT)
//        if (result.exitCode == SERVICE_ERROR || result.exitCode > 0) {
//            println("ERROR: git fetch failed for: $repoName: ${result.outputString}")
//            repo.invalidRemote = true
//        }
//        repo.remoteChecked = true
//    }
//    val result = executeProcess(dir, listOf("git", "status", "-s", "-b"), PROCESS_TIMEOUT)
//    if (result.exitCode == SERVICE_ERROR || result.exitCode > 0) {
//        println("ERROR: git status failed: $repoName: ${result.outputString}")
//        repo.invalidRepo = true
//    } else {
//        val branchLine = result.outputLines[0]
//        if (config.checkRemote) {
//            val hasRemote = branchLine.contains(BRANCH_SEPARATOR)
//            if (hasRemote) {
//                repo.branch = branchLine.substringAfter(BRANCH_LINE_PREFIX).substringBefore(BRANCH_SEPARATOR)
//                setRemoteInfo(repo, branchLine, dir, config)
//            } else {
//                repo.branch = branchLine.substring(BRANCH_LINE_PREFIX.length)
//            }
//        } else {
//            repo.branch = branchLine.substring(BRANCH_LINE_PREFIX.length)
//            if (repo.branch.contains(BRANCH_SEPARATOR)) {
//                repo.branch = repo.branch.split(BRANCH_SEPARATOR)[0]
//            }
//        }
//
//        setModificationInfo(repo, result.outputLines, config)
//    }
    return repo
}
//
//private fun setRemoteInfo(repo: Repo, branchLine: String, repoDir: File, config: Config) {
//    repo.hasRemote = !repo.invalidRemote
//    val remoteDiffers = branchLine.contains("[")
//
//    if (remoteDiffers && !repo.invalidRemote) {
//        repo.ahead = extractNumber("ahead", branchLine) ?: 0
//        repo.behind = extractNumber("behind", branchLine) ?: 0
//    }
//    if (config.checkRemote && !repo.invalidRemote) {
//        val mainBranch = getMainBranch(repoDir)
//        repo.onMainBranch = mainBranch == repo.branch
//    }
//}
//
//private fun setModificationInfo(repo: Repo, outputLines: List<String>, config: Config) {
//    for (i in 1..<outputLines.size) {
//        val field = outputLines[i].substring(0, 2)
//        repo.added = field.contains("A") || repo.added
//        repo.modified = field.contains("M") || repo.modified
//        repo.typeChange = field.contains("T") || repo.typeChange
//        repo.deleted = field.contains("D") || repo.deleted
//        repo.renamed = field.contains("R") || repo.renamed
//        repo.copied = field.contains("C") || repo.copied
//        repo.updated = field.contains("U") || repo.updated
//        if (!config.ignoreUntracked) {
//            repo.untracked = field.contains("?") || repo.untracked
//        }
//    }
//
//}
//
//private fun extractNumber(match: String, input: String): Int? {
//    val regex = Regex("${match}\\s+(\\d+)")
//    val matchResult = regex.find(input)
//    return matchResult?.groupValues?.get(1)?.toIntOrNull()
//}
//
//private fun getMainBranch(repoDir: File): String {
//    val result = executeProcess(repoDir, listOf("git", "remote", "show", "origin"), PROCESS_TIMEOUT)
//    val regex = Regex("HEAD branch: (\\w+)")
//    val matchResult = regex.find(result.outputString)
//    return matchResult?.groupValues?.get(1) ?: ""
//}
