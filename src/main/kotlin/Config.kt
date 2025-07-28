package org.mikeb.gstat

data class Config(
    var checkRemote: Boolean = false,
    var useCsvFormat: Boolean = false,
    var dirtyOnly: Boolean = false,
    var ignoreUntracked: Boolean = false,
    var verbose: Boolean = false,
    var noColor: Boolean = false,
    var directoryRoot: String = ""
)
