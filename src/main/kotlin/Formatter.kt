package org.mikeb.gstat

interface Formatter {
    fun format(repoList: List<Repo>, config: Config)
}
