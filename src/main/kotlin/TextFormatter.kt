package org.mikeb.gstat

class TextFormatter : Formatter {
    override fun format(repoList: List<Repo>, config: Config) {
        var maxRepoSize = 0
        var maxBranchSize = 0

        repoList.forEach { repo ->
            if (repo.path.length > maxRepoSize) {
                maxRepoSize = repo.path.length
            }
            if (repo.branch.length > maxBranchSize) {
                maxBranchSize = repo.branch.length
            }
        }

        val template = "%s%" + maxRepoSize + "s | %" + maxBranchSize + "s | %8s | %25s | %s%s"

        repoList.forEach { repo ->
            val modifiedString = getModifiedString(repo)
            val isDirty = modifiedString.trim().isNotEmpty() || repo.ahead > 0 || repo.behind > 0
            var prefix = ""
            var suffix = ""
            if (!config.noColor) {
                if (isDirty) {
                    prefix = "\u001B[31m"
                    suffix = "\u001B[0m"
                }
            }
            if (!config.dirtyOnly) {
                println(
                    template.format(
                        prefix, repo.path, repo.branch, modifiedString,
                        getAheadBehindString(repo), branchWarning(repo), suffix
                    )
                )
            } else if (isDirty) {
                println(
                    template.format(
                        prefix, repo.path, repo.branch, modifiedString,
                        getAheadBehindString(repo), branchWarning(repo), suffix
                    )
                )
            }
        }
    }


/*
Use the following with a private function setFlag (boolean, true-value)
to create one string on a single line:

val longString = """
    This is a very long string that \
    we want to break up into multiple \
    lines in our code, but have it \
    appear as one line in the output.
""".trimIndent()
*/
    private fun displayFlag(flag: Boolean, display: String): String {
        return if (flag) { display } else { " " }
    }

    private fun getModifiedString(repo: Repo): String {
        return "${displayFlag(repo.modified, "M")}${displayFlag(repo.typeChange, "T")}" +
                "${displayFlag(repo.added, "A")}${displayFlag(repo.deleted, "D")}" +
                "${displayFlag(repo.renamed, "R")}${displayFlag(repo.untracked, "?")}" +
                "${displayFlag(repo.copied, "C")}${displayFlag(repo.updated, "U")}"
//        val sb = StringBuilder()
//        if (repo.modified) {
//            sb.append("M")
//        } else {
//            sb.append(" ")
//        }
//        if (repo.typeChange) {
//            sb.append("T")
//        } else {
//            sb.append(" ")
//        }
//        if (repo.added) {
//            sb.append("A")
//        } else {
//            sb.append(" ")
//        }
//        if (repo.deleted) {
//            sb.append("D")
//        } else {
//            sb.append(" ")
//        }
//        if (repo.renamed) {
//            sb.append("R")
//        } else {
//            sb.append(" ")
//        }
//        if (repo.untracked) {
//            sb.append("?")
//        } else {
//            sb.append(" ")
//        }
//        if (repo.copied) {
//            sb.append("C")
//        } else {
//            sb.append(" ")
//        }
//        if (repo.updated) {
//            sb.append("U")
//        } else {
//            sb.append(" ")
//        }
//        return sb.toString()
    }

    private fun getAheadBehindString(repo: Repo): String {
        var remoteString = ""

        if (repo.hasRemote) {
            remoteString = "Ahead: %3d, Behind: %3d".format(repo.ahead, repo.behind)
        }
        return remoteString
    }

    private fun branchWarning(repo: Repo): String {
        var result = ""
        if (repo.hasRemote) {
            result = if (repo.onMainBranch) {
                ""
            } else {
                "Not Main"
            }
        } else if (repo.remoteChecked) {
            result = "No Remote"
        }
        return result
    }
}
