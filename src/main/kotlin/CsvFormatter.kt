package org.mikeb.gstat

class CsvFormatter : Formatter {
    override fun format(repoList: List<Repo>, config: Config) {
        println("\"Path\",\"Branch\",\"Modified\",\"TypeChange\",\"Added\",\"Deleted\",\"Renamed\",\"Untracked\""
            + ",\"Copied\",\"Updated\",\"HasRemote\",\"OnMainBranch\",\"InvalidRemote\",\"InvalidRepo\","
            + "\"Ahead\",\"Behind\"")
        repoList.forEach { repo ->
            println(
                "\"${repo.path}\",\"${repo.branch}\",\"${repo.modified}\",\"${repo.typeChange}\","
                        + "\"${repo.added}\",\"${repo.deleted}\",\"${repo.renamed}\",\"${repo.untracked}\","
                        + "\"${repo.copied}\",\"${repo.updated}\",\"${repo.hasRemote}\",\"${repo.onMainBranch}\","
                        + "\"${repo.invalidRemote}\",\"${repo.invalidRepo}\",${repo.ahead},${repo.behind}"
            )
        }
    }
}
