package org.mikeb.gstat

import org.eclipse.jgit.lib.Repository

data class Repo(
    val path: String,
    var branch: String = "",
    var untracked: Boolean = false,

    // staged changes
    var added: Boolean = false,
    var changed: Boolean = false,
    var removed: Boolean = false,

    // unstaged changes
    var modified: Boolean = false,
    var deleted: Boolean = false,

    var conflicting: Boolean = false,

    var typeChange: Boolean = false,
    var renamed: Boolean = false,
    var copied: Boolean = false,
    var updated: Boolean = false,
    var hasRemote: Boolean = false,
    var onMainBranch: Boolean = false,
    var remoteChecked: Boolean = false,
    var invalidRemote: Boolean = false,
    var invalidRepo: Boolean = false,
    var ahead: Int = 0,
    var behind: Int = 0,
    var repository: Repository? = null,
    var isClean: Boolean = false

)
