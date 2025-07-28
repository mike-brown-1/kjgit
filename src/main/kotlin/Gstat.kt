package org.mikeb.gstat

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file

class Gstat : CliktCommand(){
    init {
        versionOption(Version.VERSION)
    }
    private val checkRemote by option("-r", "--remote", help = "check against remote repository").flag()
    private val verbose by option("-v", "--verbose", help = "verbose output").flag()
    private val csvFormat by option("-c", "--csv", help = "csv output").flag()
    private val dirtyOnly by option("-d", "--dirty", help = "show only dirty repos").flag()
    private val noColor by option("-n", "--no-color", help = "do not use color in output").flag()
    private val ignoreUntracked by option("-u", "--untracked", help = "ignore untracked files").flag()
    private val directoryRoot by argument(help = "starting directory").file(mustExist = true)

    override fun run() {
        if (verbose) {
            echo(
                "checkRemote: $checkRemote, csvFormat: $csvFormat, dirtyOnly: $dirtyOnly, "
                        + "ignoreUntracked: $ignoreUntracked, noColor: $noColor, verbose: $verbose, "
                        + "root: ${directoryRoot.absolutePath}"
            )
        }
        val config = Config(checkRemote, csvFormat, dirtyOnly, ignoreUntracked, verbose, noColor,
            directoryRoot.absolutePath)
        val repos = getRepoData(directoryRoot, config)
        if (csvFormat) {
            CsvFormatter().format(repos, config)
        } else {
            TextFormatter().format(repos, config)
        }
    }
}

fun main(args: Array<String>) = Gstat().main(args)
