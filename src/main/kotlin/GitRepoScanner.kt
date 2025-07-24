package org.mikeb.kgstat

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.revwalk.RevWalk // Added import for RevWalk
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

/**
 * A Kotlin application to scan a directory hierarchy for Git repositories
 * and report their status (untracked, modified, added, deleted, staged, etc.).
 */
object GitRepoScanner {

    private val logger = LoggerFactory.getLogger(GitRepoScanner::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        // Determine the starting directory for the scan.
        // If no argument is provided, use the current working directory.
        val startDir = if (args.isNotEmpty()) {
            File(args[0])
        } else {
            File(System.getProperty("user.dir"))
        }

        if (!startDir.exists() || !startDir.isDirectory) {
            logger.error("Error: Starting directory '${startDir.absolutePath}' does not exist or is not a directory.")
            return
        }

        logger.info("Scanning for Git repositories in: ${startDir.absolutePath}")
        println("-------------------------------------------------------")

        // Start the recursive scan
        scanDirectory(startDir)

        println("-------------------------------------------------------")
        logger.info("Scan complete.")
    }

    /**
     * Recursively scans a given directory for Git repositories.
     *
     * @param directory The directory to scan.
     */
    private fun scanDirectory(directory: File) {
        // Iterate over files and subdirectories
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Check if the current directory is a Git repository
                if (isGitRepository(file)) {
                    processGitRepository(file)
                } else {
                    // If not a Git repository, continue scanning its subdirectories
                    scanDirectory(file)
                }
            }
        }
    }

    /**
     * Checks if a given directory is a Git repository by looking for the .git folder.
     *
     * @param directory The directory to check.
     * @return True if it's a Git repository, false otherwise.
     */
    private fun isGitRepository(directory: File): Boolean {
        // A Git repository typically contains a .git directory or a .git file (for worktrees)
        return File(directory, ".git").exists()
    }

    /**
     * Processes a found Git repository, calculates its status, and prints the results.
     *
     * @param repoDir The root directory of the Git repository.
     */
    private fun processGitRepository(repoDir: File) {
        println("\nRepository: ${repoDir.absolutePath}")
        logger.info("Processing Git repository: ${repoDir.absolutePath}")

        var repository: Repository? = null
        var git: Git? = null

        try {
            // Build the repository object. This finds the .git directory.
            repository = FileRepositoryBuilder()
                .setGitDir(File(repoDir, ".git")) // Explicitly point to the .git directory
                .readEnvironment() // Read GIT_* environment variables
                .findGitDir() // Scan up the hierarchy to find the .git directory
                .build()

            // Create a Git object from the repository
            git = Git(repository)

            // Get the status of the repository
            val status: Status = git.status().call()

            // Print the status details
            println("  Current Branch: ${repository.branch}")

            // Untracked files and folders
            val untrackedFiles = status.untracked.size
            val untrackedFolders = status.untrackedFolders.size
            if (untrackedFiles > 0 || untrackedFolders > 0) {
                println("  Untracked: $untrackedFiles files, $untrackedFolders folders")
                status.untracked.forEach { println("    - Untracked: $it") }
                status.untrackedFolders.forEach { println("    - Untracked Folder: $it") }
            } else {
                println("  Untracked: 0")
            }

            // Unstaged changes (modified, missing/deleted)
            val modifiedFiles = status.modified.size
            val missingFiles = status.missing.size
            if (modifiedFiles > 0 || missingFiles > 0) {
                println("  Unstaged Changes:")
                println("    Modified: $modifiedFiles")
                status.modified.forEach { println("      - Modified: $it") }
                println("    Deleted (Missing): $missingFiles")
                status.missing.forEach { println("      - Deleted: $it") }
            } else {
                println("  Unstaged Changes: None")
            }

            // Staged changes (added, changed, removed)
            val addedFiles = status.added.size
            val changedFiles = status.changed.size // Modified files that have been staged
            val removedFiles = status.removed.size // Deleted files that have been staged
            if (addedFiles > 0 || changedFiles > 0 || removedFiles > 0) {
                println("  Staged Changes:")
                println("    Added: $addedFiles")
                status.added.forEach { println("      - Added: $it") }
                println("    Modified (Staged): $changedFiles")
                status.changed.forEach { println("      - Modified (Staged): $it") }
                println("    Deleted (Staged): $removedFiles")
                status.removed.forEach { println("      - Deleted (Staged): $it") }
            } else {
                println("  Staged Changes: None")
            }

            // Conflicting files (during a merge/rebase)
            val conflictingFiles = status.conflicting.size
            if (conflictingFiles > 0) {
                println("  Conflicts: $conflictingFiles")
                status.conflicting.forEach { println("    - Conflict: $it") }
            } else {
                println("  Conflicts: None")
            }

            // Check if clean
            if (status.isClean) {
                println("  Repository is clean (no untracked, unstaged, or staged changes).")
            }

//            // --- Ahead/Behind Status ---
//            // To get ahead/behind, we need to fetch and then compare
//            // Note: This part can be slow if done for many repos, as it involves network operations.
//            // For a high-level overview, you might skip fetching unless explicitly needed.
//            println("  Checking ahead/behind status (requires network fetch)...")
//            try {
//                // Fetch from the remote to ensure local tracking branches are up-to-date
//                git.fetch().setRemote("origin").call()
//
//                val localBranch = repository.branch // e.g., "main" or "master"
//                // Corrected: Use findRef() instead of ref()
//                val trackingBranch = repository.findRef("refs/remotes/origin/$localBranch")
//
//                if (trackingBranch != null) {
//                    val localCommit = repository.parseCommit(repository.resolve("HEAD"))
//                    val remoteCommit = repository.parseCommit(trackingBranch.objectId)
//
//                    val walk = RevWalk(repository)
//                    // Corrected: Call mergeBase as a static method of RevWalk
//                    val commonAncestor = walk.parseCommit(RevWalk.mergeBase(walk, localCommit, remoteCommit))
//
//                    var aheadCount = 0
//                    walk.reset() // Reset walk for counting ahead commits
//                    walk.markStart(localCommit)
//                    walk.markUninteresting(commonAncestor)
//                    for (commit in walk) {
//                        aheadCount++
//                    }
//
//                    var behindCount = 0
//                    walk.reset() // Reset walk for counting behind commits
//                    walk.markStart(remoteCommit)
//                    walk.markUninteresting(commonAncestor)
//                    for (commit in walk) {
//                        behindCount++
//                    }
//                    walk.close() // Close the RevWalk
//
//                    println("  Ahead of origin/$localBranch: $aheadCount commits")
//                    println("  Behind origin/$localBranch: $behindCount commits")
//                } else {
//                    println("  No tracking branch found for 'origin/$localBranch'. Cannot determine ahead/behind status.")
//                }
//            } catch (e: Exception) {
//                logger.warn("  Could not fetch or determine ahead/behind status for ${repoDir.name}: ${e.message}")
//                println("  Could not determine ahead/behind status (error during fetch or comparison).")
//            }

        } catch (e: IOException) {
            logger.error("Error opening Git repository at ${repoDir.absolutePath}: ${e.message}")
            println("  Error: Could not open repository. ${e.message}")
        } catch (e: Exception) {
            logger.error("Error processing Git repository at ${repoDir.absolutePath}: ${e.message}", e)
            println("  Error: An unexpected error occurred. ${e.message}")
        } finally {
            // Ensure resources are closed
            git?.close()
            repository?.close()
        }
    }
}
