package org.mikeb.kgstat

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

fun checkRepositoryStatus(repositoryPath: String, fetchFromRemote: Boolean = false) {
    try {
        // Open the repository
        val repository: Repository = FileRepositoryBuilder()
            .setGitDir(File(repositoryPath, ".git"))
            .readEnvironment()
            .findGitDir()
            .build()

        val git = Git(repository)

        // Fetch latest information from remote if requested
        if (fetchFromRemote) {
            println("Fetching latest information from remote...")
            try {
                git.fetch().call()
                println("Fetch completed successfully")
            } catch (e: Exception) {
                println("Warning: Failed to fetch from remote: ${e.message}")
                println("Proceeding with local tracking information...")
            }
        } else {
            println("Using local tracking information (no fetch performed)")
        }

        // Get current branch name
        val currentBranch = repository.branch
        if (currentBranch == null) {
            println("No current branch found")
            return
        }

        println("Current branch: $currentBranch")

        // Get tracking status for current branch
        val trackingStatus = BranchTrackingStatus.of(repository, currentBranch)

        if (trackingStatus == null) {
            println("Branch '$currentBranch' is not tracking any remote branch")
            return
        }

        val remoteBranch = trackingStatus.remoteTrackingBranch
        println("Tracking remote branch: $remoteBranch")

        val aheadCount = trackingStatus.aheadCount
        val behindCount = trackingStatus.behindCount

        when {
            aheadCount == 0 && behindCount == 0 -> {
                println("✅ Branch is up to date with remote")
            }
            aheadCount > 0 && behindCount == 0 -> {
                println("⬆️  Branch is ahead of remote by $aheadCount commit(s)")
            }
            aheadCount == 0 && behindCount > 0 -> {
                println("⬇️  Branch is behind remote by $behindCount commit(s)")
            }
            aheadCount > 0 && behindCount > 0 -> {
                println("🔀 Branch has diverged: $aheadCount commit(s) ahead, $behindCount commit(s) behind")
            }
        }

        repository.close()

    } catch (e: Exception) {
        println("Error checking repository status: ${e.message}")
        e.printStackTrace()
    }
}

fun main() {
    // Replace with your repository path, or use "." for current directory
    val repositoryPath = "."

    println("Checking Git repository status...")

    // Example calls:
    // Without fetch (fast, uses local tracking info)
    checkRepositoryStatus(repositoryPath, fetchFromRemote = false)

    println("\n" + "=".repeat(50) + "\n")

    // With fetch (slower, gets latest remote info)
    // Uncomment the line below to test with fetch
    // checkRepositoryStatus(repositoryPath, fetchFromRemote = true)
}
